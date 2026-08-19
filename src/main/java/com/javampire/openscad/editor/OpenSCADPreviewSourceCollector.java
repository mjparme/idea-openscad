package com.javampire.openscad.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.javampire.openscad.psi.OpenSCADImportUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects OpenSCAD source files for in-browser WASM preview, including transitive imports.
 * <p>
 * Uses text scanning for {@code use}/{@code include} paths so preview bundling matches OpenSCAD's
 * import graph even when PSI resolution is incomplete. Falls back to VFS lookup under the content root.
 */
public final class OpenSCADPreviewSourceCollector {

    private static final Logger LOG = Logger.getInstance(OpenSCADPreviewSourceCollector.class);
    private static final String WORK_ROOT = "/work";
    private static final Pattern IMPORT_PATTERN = Pattern.compile("\\b(?:use|include)\\s*<([^>]+)>");

    private OpenSCADPreviewSourceCollector() {
    }

    public record PreviewSources(@NotNull String mainPath, @NotNull Map<String, String> files) {
    }

    private record PendingFile(@NotNull String virtualPath, @Nullable PsiFile psiFile, @Nullable VirtualFile virtualFile) {
    }

    @Nullable
    public static PreviewSources collect(@NotNull final Project project, @NotNull final VirtualFile scadFile) {
        final PsiFile mainPsiFile = PsiManager.getInstance(project).findFile(scadFile);
        if (mainPsiFile == null) {
            return null;
        }

        final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        final VirtualFile contentRoot = fileIndex.getContentRootForFile(scadFile);
        if (contentRoot == null) {
            return null;
        }

        final String mainRelativePath = VfsUtil.getRelativePath(scadFile, contentRoot, '/');
        if (mainRelativePath == null) {
            return null;
        }

        final String mainPath = toVirtualPath(mainRelativePath);
        final Map<String, String> files = new LinkedHashMap<>();
        final Set<String> visitedPaths = new java.util.HashSet<>();
        final Queue<PendingFile> queue = new ArrayDeque<>();
        queue.add(new PendingFile(mainPath, mainPsiFile, scadFile));

        while (!queue.isEmpty()) {
            final PendingFile pending = queue.remove();
            if (!visitedPaths.add(pending.virtualPath())) {
                continue;
            }

            final String content = readContent(pending.psiFile(), pending.virtualFile());
            if (content == null) {
                LOG.warn("OpenSCAD preview: could not read " + pending.virtualPath());
                continue;
            }
            files.put(pending.virtualPath(), content);

            final String projectRelativePath = toProjectRelativePath(pending.virtualPath());
            for (final String importPath : collectImportPathsFromText(content)) {
                final String importVirtualPath = resolveImportVirtualPath(pending.virtualPath(), importPath);
                if (visitedPaths.contains(importVirtualPath)) {
                    continue;
                }

                final PendingFile imported = resolveImportedFile(
                        project,
                        contentRoot,
                        pending.psiFile(),
                        pending.virtualFile(),
                        projectRelativePath,
                        importPath,
                        importVirtualPath
                );
                if (imported != null) {
                    queue.add(imported);
                }
                else {
                    LOG.warn("OpenSCAD preview: unresolved import <" + importPath + "> from " + pending.virtualPath());
                }
            }
        }

        return new PreviewSources(mainPath, files);
    }

    @NotNull
    static List<String> collectImportPathsFromText(@NotNull final String content) {
        final List<String> paths = new ArrayList<>();
        final Matcher matcher = IMPORT_PATTERN.matcher(content);
        while (matcher.find()) {
            paths.add(matcher.group(1));
        }
        return paths;
    }

    @Nullable
    private static PendingFile resolveImportedFile(@NotNull final Project project,
                                                   @NotNull final VirtualFile contentRoot,
                                                   @Nullable final PsiFile importerPsiFile,
                                                   @Nullable final VirtualFile importerVirtualFile,
                                                   @NotNull final String importerProjectRelativePath,
                                                   @NotNull final String importPath,
                                                   @NotNull final String importVirtualPath) {
        if (importerPsiFile != null) {
            for (final PsiFile target : OpenSCADImportUtil.resolveImportFiles(importerPsiFile, importPath)) {
                return new PendingFile(importVirtualPath, target, target.getVirtualFile());
            }
        }

        if (importerVirtualFile != null && importerVirtualFile.getParent() != null && !importPath.startsWith("BOSL/")) {
            final VirtualFile resolved = importerVirtualFile.getParent().findFileByRelativePath(importPath);
            if (resolved != null && !resolved.isDirectory()) {
                final PsiFile psiFile = PsiManager.getInstance(project).findFile(resolved);
                return new PendingFile(importVirtualPath, psiFile, resolved);
            }
        }

        final String importedProjectRelativePath = resolveImportProjectRelativePath(importerProjectRelativePath, importPath);
        final VirtualFile resolvedFromRoot = contentRoot.findFileByRelativePath(importedProjectRelativePath);
        if (resolvedFromRoot != null && !resolvedFromRoot.isDirectory()) {
            final PsiFile psiFile = PsiManager.getInstance(project).findFile(resolvedFromRoot);
            return new PendingFile(importVirtualPath, psiFile, resolvedFromRoot);
        }

        return null;
    }

    @Nullable
    static String readContent(@Nullable final PsiFile psiFile, @Nullable final VirtualFile virtualFile) {
        if (psiFile != null) {
            return getFileContent(psiFile);
        }
        if (virtualFile != null) {
            final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document != null) {
                return document.getText();
            }
            try {
                return VfsUtil.loadText(virtualFile);
            }
            catch (IOException e) {
                LOG.warn("Failed to read " + virtualFile.getPath(), e);
            }
        }
        return null;
    }

    @NotNull
    static String getFileContent(@NotNull final PsiFile psiFile) {
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile != null) {
            final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document != null) {
                return document.getText();
            }
            try {
                return VfsUtil.loadText(virtualFile);
            }
            catch (IOException e) {
                return psiFile.getText();
            }
        }
        return psiFile.getText();
    }

    @NotNull
    private static String toVirtualPath(@NotNull final String relativePath) {
        return WORK_ROOT + "/" + relativePath;
    }

    @NotNull
    private static String toProjectRelativePath(@NotNull final String virtualPath) {
        if (virtualPath.startsWith(WORK_ROOT + "/")) {
            return virtualPath.substring(WORK_ROOT.length() + 1);
        }
        return virtualPath.startsWith("/") ? virtualPath.substring(1) : virtualPath;
    }

    @NotNull
    static String resolveImportProjectRelativePath(@NotNull final String importerProjectRelativePath,
                                                   @NotNull final String importPath) {
        if (importPath.startsWith("BOSL/")) {
            return "lib/bosl/" + importPath.substring("BOSL/".length());
        }
        final int slash = importerProjectRelativePath.lastIndexOf('/');
        final String importerDir = slash >= 0 ? importerProjectRelativePath.substring(0, slash) : "";
        return normalizeProjectRelativePath(importerDir + "/" + importPath);
    }

    @NotNull
    static String resolveImportVirtualPath(@NotNull final String importerVirtualPath, @NotNull final String importPath) {
        if (importPath.startsWith("BOSL/")) {
            return normalizeVirtualPath(WORK_ROOT + "/" + importPath);
        }
        final int slash = importerVirtualPath.lastIndexOf('/');
        final String importerDir = slash >= 0 ? importerVirtualPath.substring(0, slash) : WORK_ROOT;
        return normalizeVirtualPath(importerDir + "/" + importPath);
    }

    @NotNull
    private static String normalizeProjectRelativePath(@NotNull final String path) {
        return normalizeVirtualPath("/" + path).substring(1);
    }

    @NotNull
    private static String normalizeVirtualPath(@NotNull final String path) {
        final String[] parts = path.split("/");
        final java.util.Deque<String> stack = new ArrayDeque<>();
        for (final String part : parts) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!stack.isEmpty()) {
                    stack.removeLast();
                }
                continue;
            }
            stack.addLast(part);
        }
        final StringBuilder normalized = new StringBuilder();
        for (final String part : stack) {
            normalized.append('/').append(part);
        }
        return normalized.isEmpty() ? WORK_ROOT : normalized.toString();
    }
}
