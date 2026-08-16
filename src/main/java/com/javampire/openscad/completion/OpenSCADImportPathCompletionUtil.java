package com.javampire.openscad.completion;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.javampire.openscad.psi.OpenSCADIncludeImport;
import com.javampire.openscad.psi.OpenSCADImportPathRef;
import com.javampire.openscad.psi.OpenSCADImportPathRefElement;
import com.javampire.openscad.psi.OpenSCADTypes;
import com.javampire.openscad.psi.OpenSCADUseImport;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.javampire.openscad.OpenSCADFileType;
import com.javampire.openscad.references.OpenSCADResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class OpenSCADImportPathCompletionUtil {

    private OpenSCADImportPathCompletionUtil() {
    }

    record ImportPathSuggestion(@NotNull String path, boolean directory) {
    }

    @Nullable
    static OpenSCADImportPathRefElement findImportPathRefAtOffset(@NotNull final PsiFile file, final int offset) {
        final OpenSCADImportPathRefElement pathRef = findImportPathRef(file, offset, file.findElementAt(offset));
        if (pathRef != null && offsetInsideActiveImportPath(file, offset, pathRef)) {
            return pathRef;
        }
        return null;
    }

    private static boolean offsetInsideActiveImportPath(@NotNull final PsiFile file,
                                                        final int offset,
                                                        @NotNull final OpenSCADImportPathRefElement pathRef) {
        final String text = file.getText();
        final int open = text.lastIndexOf('<', offset - 1);
        if (open < 0) {
            return false;
        }
        final int close = text.indexOf('>', open + 1);
        if (close >= 0 && offset > close) {
            return false;
        }
        return offset >= pathRef.getTextRange().getStartOffset()
            && offset <= pathRef.getTextRange().getEndOffset() + 1;
    }

    @Nullable
    static OpenSCADImportPathRefElement findImportPathRef(@NotNull final PsiFile file,
                                                          final int offset,
                                                          @Nullable final PsiElement position) {
        if (position != null) {
            final OpenSCADImportPathRefElement pathRef = findImportPathRef(position);
            if (pathRef != null && offsetInsideActiveImportPath(file, offset, pathRef)) {
                return pathRef;
            }
        }

        final String text = file.getText();
        final int lineStart = text.lastIndexOf('\n', Math.max(0, offset - 1)) + 1;
        final int start = Math.max(lineStart, offset - 8);
        for (int probe = offset; probe >= start; probe--) {
            final PsiElement probeElement = file.findElementAt(probe);
            if (probeElement == null) {
                continue;
            }
            final OpenSCADImportPathRefElement pathRef = findImportPathRef(probeElement);
            if (pathRef != null && offsetInsideActiveImportPath(file, offset, pathRef)) {
                return pathRef;
            }
        }
        return null;
    }

    @NotNull
    static String partialImportPathAtOffset(@NotNull final OpenSCADImportPathRefElement pathRef,
                                            final int offset,
                                            @NotNull final String prefixMatcherPrefix) {
        if (!prefixMatcherPrefix.isEmpty()) {
            return prefixMatcherPrefix;
        }
        final int pathStart = pathRef.getTextRange().getStartOffset();
        final int relativeOffset = Math.min(Math.max(offset - pathStart, 0), pathRef.getTextLength());
        if (relativeOffset == 0) {
            return "";
        }
        return pathRef.getText().substring(0, relativeOffset);
    }

    static boolean isInsideIncompleteImportPath(@NotNull final PsiFile file, final int offset) {
        return extractPartialImportPathFromText(file, offset) != null;
    }

    /**
     * Returns the typed path between {@code <} and the caret when the closing {@code >} is not yet present.
     */
    @Nullable
    static String extractPartialImportPathFromText(@NotNull final PsiFile file, final int offset) {
        if (offset <= 0) {
            return null;
        }
        final String text = file.getText();
        final int open = text.lastIndexOf('<', offset - 1);
        if (open < 0) {
            return null;
        }
        final int pathStart = open + 1;
        final int close = text.indexOf('>', pathStart);
        if (close >= 0 && close < offset) {
            return null;
        }
        final int pathEnd = Math.min(offset, text.length());
        if (pathEnd <= pathStart) {
            return "";
        }
        return text.substring(pathStart, pathEnd);
    }

    @Nullable
    static OpenSCADImportPathRefElement findImportPathRef(@NotNull final PsiElement position) {
        if (position instanceof OpenSCADImportPathRefElement pathRef) {
            return pathRef;
        }
        if (position.getNode() != null && position.getNode().getElementType() == OpenSCADTypes.IMPORT_PATH) {
            final PsiElement parent = position.getParent();
            if (parent instanceof OpenSCADImportPathRefElement pathRef) {
                return pathRef;
            }
        }

        final PsiElement importStatement = PsiTreeUtil.getParentOfType(position, OpenSCADIncludeImport.class, OpenSCADUseImport.class);
        if (importStatement instanceof OpenSCADIncludeImport include) {
            return asPathRefElement(include.getImportPathRef());
        }
        if (importStatement instanceof OpenSCADUseImport use) {
            return asPathRefElement(use.getImportPathRef());
        }

        return PsiTreeUtil.getParentOfType(position, OpenSCADImportPathRefElement.class);
    }

    @Nullable
    private static OpenSCADImportPathRefElement asPathRefElement(@Nullable final OpenSCADImportPathRef pathRef) {
        return pathRef instanceof OpenSCADImportPathRefElement element ? element : null;
    }

    @NotNull
    static List<ImportPathSuggestion> suggestPaths(@NotNull final PsiFile contextFile, @NotNull final String partialPath) {
        final VirtualFile contextVirtualFile = contextFile.getVirtualFile();
        if (contextVirtualFile == null) {
            return List.of();
        }

        final PathParts parts = PathParts.split(partialPath);
        final Set<String> seen = new LinkedHashSet<>();
        final List<ImportPathSuggestion> suggestions = new ArrayList<>();

        addDirectoryListingSuggestions(contextVirtualFile, parts, seen, suggestions);
        addResolverSuggestions(contextFile, partialPath, seen, suggestions);
        addProjectFileSuggestions(contextFile, partialPath, parts, seen, suggestions);

        return suggestions;
    }

    private static void addDirectoryListingSuggestions(@NotNull final VirtualFile contextFile,
                                                       @NotNull final PathParts parts,
                                                       @NotNull final Set<String> seen,
                                                       @NotNull final List<ImportPathSuggestion> suggestions) {
        final VirtualFile directory = resolveDirectory(contextFile, parts.parentPath());
        if (directory == null) {
            return;
        }

        for (final VirtualFile child : directory.getChildren()) {
            final String childName = child.getName();
            if (!nameMatchesPrefix(childName, parts.segmentPrefix())) {
                continue;
            }

            if (child.isDirectory()) {
                addSuggestion(parts.parentPath() + childName + "/", true, seen, suggestions);
                continue;
            }

            if ("scad".equals(child.getExtension())) {
                addSuggestion(parts.parentPath() + childName, false, seen, suggestions);
            }
        }
    }

    private static void addResolverSuggestions(@NotNull final PsiFile contextFile,
                                               @NotNull final String partialPath,
                                               @NotNull final Set<String> seen,
                                               @NotNull final List<ImportPathSuggestion> suggestions) {
        final GlobalSearchScope scope = importSearchScope(contextFile);
        final Project project = contextFile.getProject();

        collectResolverMatches(project, scope, contextFile, partialPath, seen, suggestions);
        if (!partialPath.endsWith(".scad")) {
            collectResolverMatches(project, scope, contextFile, partialPath + ".scad", seen, suggestions);
        }
    }

    private static void collectResolverMatches(@NotNull final Project project,
                                               @NotNull final GlobalSearchScope scope,
                                               @NotNull final PsiFile contextFile,
                                               @NotNull final String candidatePath,
                                               @NotNull final Set<String> seen,
                                               @NotNull final List<ImportPathSuggestion> suggestions) {
        for (final PsiFile match : OpenSCADResolver.findFilesByRelativePath(project, scope, candidatePath)) {
            final VirtualFile virtualFile = match.getVirtualFile();
            if (virtualFile == null) {
                continue;
            }
            final VirtualFile contextVirtualFile = contextFile.getVirtualFile();
            if (contextVirtualFile == null) {
                continue;
            }
            final String importPath = toImportPath(contextVirtualFile, virtualFile);
            if (importPath != null) {
                addSuggestion(importPath, false, seen, suggestions);
            }
        }
    }

    private static void addProjectFileSuggestions(@NotNull final PsiFile contextFile,
                                                  @NotNull final String partialPath,
                                                  @NotNull final PathParts parts,
                                                  @NotNull final Set<String> seen,
                                                  @NotNull final List<ImportPathSuggestion> suggestions) {
        if (parts.segmentPrefix().isEmpty()) {
            return;
        }

        final GlobalSearchScope scope = importSearchScope(contextFile);
        final Project project = contextFile.getProject();
        final ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);

        final VirtualFile contextVirtualFile = contextFile.getVirtualFile();
        if (contextVirtualFile == null) {
            return;
        }

        for (final VirtualFile file : FileTypeIndex.getFiles(OpenSCADFileType.INSTANCE, scope)) {
            if (file.isDirectory() || !"scad".equals(file.getExtension())) {
                continue;
            }

            final String importPath = toImportPath(contextVirtualFile, file);
            if (importPath == null || !matchesPartialPath(importPath, partialPath)) {
                continue;
            }

            if (!fileIndex.isInContent(file) && !scope.contains(file)) {
                continue;
            }

            addSuggestion(importPath, false, seen, suggestions);
        }
    }

    @NotNull
    private static GlobalSearchScope importSearchScope(@NotNull final PsiFile contextFile) {
        final Module module = ModuleUtil.findModuleForFile(contextFile.getOriginalFile());
        if (module != null) {
            return module.getModuleWithLibrariesScope().union(module.getModuleContentScope());
        }
        return ProjectScope.getContentScope(contextFile.getProject())
            .union(ProjectScope.getLibrariesScope(contextFile.getProject()));
    }

    @Nullable
    private static VirtualFile resolveDirectory(@NotNull final VirtualFile contextFile, @NotNull final String parentPath) {
        final VirtualFile contextDir = contextFile.getParent();
        if (contextDir == null) {
            return null;
        }
        if (parentPath.isEmpty()) {
            return contextDir;
        }

        final String normalized = parentPath.endsWith("/") ? parentPath.substring(0, parentPath.length() - 1) : parentPath;
        if (normalized.isEmpty()) {
            return contextDir;
        }

        return contextDir.findFileByRelativePath(normalized);
    }

    @Nullable
    static String toImportPath(@NotNull final VirtualFile contextFile, @NotNull final VirtualFile targetFile) {
        final VirtualFile contextDir = contextFile.getParent();
        if (contextDir == null) {
            return targetFile.getName();
        }

        final String relative = VfsUtil.getRelativePath(targetFile, contextDir, '/');
        if (relative != null && !relative.startsWith("/")) {
            return relative;
        }

        return targetFile.getName();
    }

    private static boolean matchesPartialPath(@NotNull final String importPath, @NotNull final String partialPath) {
        if (!importPath.startsWith(partialPath)) {
            return false;
        }
        return importPath.length() == partialPath.length()
            || importPath.charAt(partialPath.length()) == '/'
            || importPath.charAt(partialPath.length()) == '.';
    }

    private static boolean nameMatchesPrefix(@NotNull final String name, @NotNull final String prefix) {
        return prefix.isEmpty() || name.startsWith(prefix);
    }

    private static void addSuggestion(@NotNull final String path,
                                      final boolean directory,
                                      @NotNull final Set<String> seen,
                                      @NotNull final List<ImportPathSuggestion> suggestions) {
        if (seen.add(path)) {
            suggestions.add(new ImportPathSuggestion(path, directory));
        }
    }

    private record PathParts(@NotNull String parentPath, @NotNull String segmentPrefix) {
        @NotNull
        static PathParts split(@NotNull final String partialPath) {
            final int lastSlash = partialPath.lastIndexOf('/');
            if (lastSlash < 0) {
                return new PathParts("", partialPath);
            }
            return new PathParts(partialPath.substring(0, lastSlash + 1), partialPath.substring(lastSlash + 1));
        }
    }
}
