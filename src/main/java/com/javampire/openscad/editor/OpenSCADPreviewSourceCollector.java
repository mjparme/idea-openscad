package com.javampire.openscad.editor;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Collects OpenSCAD source files for in-browser WASM preview, including transitive imports.
 */
public final class OpenSCADPreviewSourceCollector {

    private static final String WORK_ROOT = "/work";

    private OpenSCADPreviewSourceCollector() {
    }

    public record PreviewSources(@NotNull String mainPath, @NotNull Map<String, String> files) {
    }

    private record PendingFile(@NotNull PsiFile file, @NotNull String virtualPath) {
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
        final Set<PsiFile> visited = new java.util.HashSet<>();
        final Queue<PendingFile> queue = new ArrayDeque<>();
        queue.add(new PendingFile(mainPsiFile, mainPath));

        while (!queue.isEmpty()) {
            final PendingFile pending = queue.remove();
            if (!visited.add(pending.file())) {
                continue;
            }

            files.put(pending.virtualPath(), getFileContent(pending.file()));

            for (final OpenSCADImportUtil.ImportEntry importEntry : OpenSCADImportUtil.getDirectImports(pending.file())) {
                final String importVirtualPath = resolveImportVirtualPath(pending.virtualPath(), importEntry.path());
                for (final PsiFile target : OpenSCADImportUtil.resolveImportFiles(pending.file(), importEntry.path())) {
                    queue.add(new PendingFile(target, importVirtualPath));
                }
            }
        }

        return new PreviewSources(mainPath, files);
    }

    /**
     * Prefer the open editor document so preview reflects unsaved edits and the latest save.
     */
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
    static String resolveImportVirtualPath(@NotNull final String importerVirtualPath, @NotNull final String importPath) {
        final int slash = importerVirtualPath.lastIndexOf('/');
        final String importerDir = slash >= 0 ? importerVirtualPath.substring(0, slash) : WORK_ROOT;
        return normalizeVirtualPath(importerDir + "/" + importPath);
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
