package com.javampire.openscad.psi;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.javampire.openscad.references.OpenSCADResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class OpenSCADImportUtil {

    public enum ImportKind {
        USE,
        INCLUDE
    }

    public record ImportEntry(@NotNull ImportKind kind, @NotNull String path, @NotNull PsiElement importStatement) {
    }

    private OpenSCADImportUtil() {
    }

    @Nullable
    public static String getImportPath(@NotNull final PsiElement importStatement) {
        final OpenSCADImportPathRefElement pathRef = getImportPathRef(importStatement);
        return pathRef != null ? pathRef.getImportPath() : null;
    }

    @Nullable
    private static OpenSCADImportPathRefElement getImportPathRef(@NotNull final PsiElement importStatement) {
        if (importStatement instanceof OpenSCADIncludeImport include) {
            final OpenSCADImportPathRef pathRef = include.getImportPathRef();
            return pathRef instanceof OpenSCADImportPathRefElement element ? element : null;
        }
        if (importStatement instanceof OpenSCADUseImport use) {
            final OpenSCADImportPathRef pathRef = use.getImportPathRef();
            return pathRef instanceof OpenSCADImportPathRefElement element ? element : null;
        }
        return null;
    }

    @NotNull
    public static List<ImportEntry> getDirectImports(@NotNull final PsiFile file) {
        final List<ImportEntry> result = new ArrayList<>();
        for (final PsiElement child : file.getChildren()) {
            if (!(child instanceof OpenSCADImport importWrapper)) {
                continue;
            }
            final OpenSCADUseImport use = importWrapper.getUseImport();
            if (use != null) {
                final String path = getImportPath(use);
                if (path != null) {
                    result.add(new ImportEntry(ImportKind.USE, path, use));
                }
                continue;
            }
            final OpenSCADIncludeImport include = importWrapper.getIncludeImport();
            if (include != null) {
                final String path = getImportPath(include);
                if (path != null) {
                    result.add(new ImportEntry(ImportKind.INCLUDE, path, include));
                }
            }
        }
        return result;
    }

    @NotNull
    public static List<PsiFile> resolveImportFiles(@NotNull final PsiFile contextFile, @NotNull final String importPath) {
        final Module module = ModuleUtil.findModuleForFile(contextFile.getOriginalFile());
        if (module != null) {
            List<PsiFile> files = OpenSCADResolver.findModuleLibrary(module, importPath);
            if (files.isEmpty()) {
                files = OpenSCADResolver.findModuleContentFile(module, importPath);
            }
            return files;
        }
        List<PsiFile> files = OpenSCADResolver.findProjectLibrary(contextFile.getProject(), importPath);
        if (files.isEmpty()) {
            files = OpenSCADResolver.findProjectContentFile(contextFile.getProject(), importPath);
        }
        return files;
    }

    @NotNull
    public static String formatSourceTailText(@NotNull final String importPath) {
        return " from " + importPath;
    }

    /**
     * Collects variables made available through {@code include} directives.
     * {@code use} imports do not export variables per OpenSCAD semantics.
     */
    public static void collectIncludedVariables(@NotNull final PsiFile file,
                                                @NotNull final Set<PsiFile> visitedFiles,
                                                @NotNull final BiConsumer<OpenSCADVariableDeclaration, String> consumer) {
        for (final ImportEntry importEntry : getDirectImports(file)) {
            if (importEntry.kind() != ImportKind.INCLUDE) {
                continue;
            }
            final List<PsiFile> targets = resolveImportFiles(file, importEntry.path());
            if (targets.isEmpty()) {
                continue;
            }
            collectVariablesFromIncludedFile(targets.get(0), importEntry.path(), visitedFiles, consumer);
        }
    }

    private static void collectVariablesFromIncludedFile(@NotNull final PsiFile file,
                                                         @NotNull final String sourcePath,
                                                         @NotNull final Set<PsiFile> visitedFiles,
                                                         @NotNull final BiConsumer<OpenSCADVariableDeclaration, String> consumer) {
        if (!visitedFiles.add(file)) {
            return;
        }

        final String tailText = formatSourceTailText(sourcePath);
        for (final OpenSCADVariableDeclaration variable : PsiTreeUtil.getChildrenOfTypeAsList(file, OpenSCADVariableDeclaration.class)) {
            consumer.accept(variable, tailText);
        }

        for (final ImportEntry importEntry : getDirectImports(file)) {
            if (importEntry.kind() != ImportKind.INCLUDE) {
                continue;
            }
            final List<PsiFile> targets = resolveImportFiles(file, importEntry.path());
            if (targets.isEmpty()) {
                continue;
            }
            collectVariablesFromIncludedFile(targets.get(0), importEntry.path(), visitedFiles, consumer);
        }
    }

    /**
     * Collects modules and functions from {@code use} and {@code include} directives.
     * Nested {@code use} imports do not propagate to the base file.
     */
    public static void collectImportedModulesAndFunctions(@NotNull final PsiFile file,
                                                          @NotNull final Set<PsiFile> visitedFiles,
                                                          @NotNull final Consumer<ImportedSymbol> consumer) {
        for (final ImportEntry importEntry : getDirectImports(file)) {
            final List<PsiFile> targets = resolveImportFiles(file, importEntry.path());
            if (targets.isEmpty()) {
                continue;
            }
            final PsiFile target = targets.get(0);
            final String tailText = formatSourceTailText(importEntry.path());

            if (importEntry.kind() == ImportKind.USE) {
                addFileModulesAndFunctions(target, tailText, consumer);
            }
            else {
                collectModulesAndFunctionsFromIncludedFile(target, importEntry.path(), visitedFiles, consumer);
            }
        }
    }

    private static void collectModulesAndFunctionsFromIncludedFile(@NotNull final PsiFile file,
                                                                   @NotNull final String sourcePath,
                                                                   @NotNull final Set<PsiFile> visitedFiles,
                                                                   @NotNull final Consumer<ImportedSymbol> consumer) {
        if (!visitedFiles.add(file)) {
            return;
        }

        addFileModulesAndFunctions(file, formatSourceTailText(sourcePath), consumer);

        for (final ImportEntry importEntry : getDirectImports(file)) {
            final List<PsiFile> targets = resolveImportFiles(file, importEntry.path());
            if (targets.isEmpty()) {
                continue;
            }
            final PsiFile target = targets.get(0);
            final String tailText = formatSourceTailText(importEntry.path());

            if (importEntry.kind() == ImportKind.USE) {
                addFileModulesAndFunctions(target, tailText, consumer);
            }
            else {
                collectModulesAndFunctionsFromIncludedFile(target, importEntry.path(), visitedFiles, consumer);
            }
        }
    }

    private static void addFileModulesAndFunctions(@NotNull final PsiFile file,
                                                   @NotNull final String tailText,
                                                   @NotNull final Consumer<ImportedSymbol> consumer) {
        for (final OpenSCADModuleDeclaration module : OpenSCADPsiImplUtil.getFileModuleDeclarations(file)) {
            consumer.accept(new ImportedSymbol(module, tailText));
        }
        for (final OpenSCADFunctionDeclaration function : OpenSCADPsiImplUtil.getFileFunctionDeclarations(file)) {
            consumer.accept(new ImportedSymbol(function, tailText));
        }
    }

    public record ImportedSymbol(@NotNull PsiElement declaration, @NotNull String sourceTailText) {
    }
}
