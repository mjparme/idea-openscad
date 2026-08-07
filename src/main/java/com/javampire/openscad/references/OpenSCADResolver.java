package com.javampire.openscad.references;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OpenSCADResolver {

    public static List<PsiFile> findModuleContentFile(@NotNull final PsiFile contextFile, @NotNull final String fileRelativePath) {
        final Module module = ModuleUtil.findModuleForFile(contextFile.getOriginalFile());
        if (module == null) {
            return List.of();
        }
        return findImportFile(contextFile, module.getModuleContentScope(), fileRelativePath);
    }

    public static List<PsiFile> findModuleLibrary(@NotNull final PsiFile contextFile, @NotNull final String fileRelativePath) {
        final Module module = ModuleUtil.findModuleForFile(contextFile.getOriginalFile());
        if (module == null) {
            return List.of();
        }
        return findImportFile(contextFile, module.getModuleWithLibrariesScope(), fileRelativePath);
    }

    public static List<PsiFile> findProjectContentFile(@NotNull final PsiFile contextFile, @NotNull final String fileRelativePath) {
        return findImportFile(contextFile, ProjectScope.getContentScope(contextFile.getProject()), fileRelativePath);
    }

    public static List<PsiFile> findProjectLibrary(@NotNull final PsiFile contextFile, @NotNull final String fileRelativePath) {
        return findImportFile(contextFile, ProjectScope.getLibrariesScope(contextFile.getProject()), fileRelativePath);
    }

    @NotNull
    public static List<PsiFile> findImportFile(@NotNull final PsiFile contextFile,
                                               @NotNull final GlobalSearchScope scope,
                                               @NotNull final String fileRelativePath) {
        final Project project = contextFile.getProject();
        final PsiManager psiManager = PsiManager.getInstance(project);

        final PsiFile resolvedFromContext = resolveRelativeToContainingFile(contextFile, scope, fileRelativePath, psiManager);
        if (resolvedFromContext != null) {
            return List.of(resolvedFromContext);
        }

        return findFilesByRelativePath(project, scope, fileRelativePath);
    }

    @Nullable
    private static PsiFile resolveRelativeToContainingFile(@NotNull final PsiFile contextFile,
                                                           @NotNull final GlobalSearchScope scope,
                                                           @NotNull final String fileRelativePath,
                                                           @NotNull final PsiManager psiManager) {
        final VirtualFile contextVirtualFile = contextFile.getVirtualFile();
        if (contextVirtualFile == null) {
            return null;
        }
        final VirtualFile parent = contextVirtualFile.getParent();
        if (parent == null) {
            return null;
        }
        final VirtualFile resolved = parent.findFileByRelativePath(fileRelativePath);
        if (resolved == null || resolved.isDirectory() || !scope.contains(resolved)) {
            return null;
        }
        return psiManager.findFile(resolved);
    }

    public static List<PsiFile> findFilesByRelativePath(@NotNull final Project project, @NotNull final GlobalSearchScope scope, @NotNull final String fileRelativePath) {
        final String name = new File(fileRelativePath).getName();
        final Collection<VirtualFile> potentialFiles = FilenameIndex.getVirtualFilesByName(name, scope);
        final List<PsiFile> fileList = new ArrayList<>();
        final String relativePath = fileRelativePath.startsWith("/") ? fileRelativePath : "/" + fileRelativePath;
        for (final VirtualFile f : potentialFiles) {
            if (f.getPath().endsWith(relativePath)) {
                fileList.add(PsiManager.getInstance(project).findFile(f));
            }
        }
        return fileList;
    }
}
