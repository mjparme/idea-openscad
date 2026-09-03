package com.javampire.openscad.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads builtin module/function declarations from bundled skeleton files.
 * Used when stub indexes do not include skeleton roots (e.g. in lightweight tests).
 */
public final class BuiltinSkeletons {

    private static final String MODULES_RESOURCE = BuiltinSkeletonResources.MODULES_RESOURCE;
    private static final String FUNCTIONS_RESOURCE = BuiltinSkeletonResources.FUNCTIONS_RESOURCE;
    private static final String POSITIONAL_FIRST_ARGUMENT_MARKER = "// POSITIONAL_FIRST_ARGUMENT:";
    private static final Set<String> DEFAULT_POSITIONAL_FIRST_ARGUMENT_MODULES =
        Set.of("cube", "sphere", "rotate", "translate");
    /** Built-in primitives that typically end a statement with {@code ;}, not child blocks. */
    private static final Set<String> STATEMENT_ENDING_SHAPE_MODULES = Set.of(
        "cube", "sphere", "cylinder", "polyhedron",
        "square", "circle", "polygon", "text", "surface", "import"
    );

    private static Map<String, OpenSCADModuleDeclaration> moduleDeclarations;
    private static Map<String, OpenSCADFunctionDeclaration> functionDeclarations;
    private static Set<String> positionalFirstArgumentModules;
    private static long moduleSkeletonContentHash = -1;
    private static long functionSkeletonContentHash = -1;

    private BuiltinSkeletons() {
    }

    public static void clearCaches() {
        moduleDeclarations = null;
        functionDeclarations = null;
        positionalFirstArgumentModules = null;
        moduleSkeletonContentHash = -1;
        functionSkeletonContentHash = -1;
    }

    @Nullable
    public static OpenSCADModuleDeclaration findModuleDeclaration(@NotNull final Project project, @NotNull final String name) {
        return getModuleDeclarations(project).get(name);
    }

    @Nullable
    public static OpenSCADFunctionDeclaration findFunctionDeclaration(@NotNull final Project project, @NotNull final String name) {
        return getFunctionDeclarations(project).get(name);
    }

    public static boolean isPositionalFirstArgumentModule(@NotNull final String name) {
        if (DEFAULT_POSITIONAL_FIRST_ARGUMENT_MODULES.contains(name)) {
            return true;
        }

        return getPositionalFirstArgumentModules().contains(name);
    }

    /**
     * Whether module completion should append {@code ;} when the call ends the current statement.
     * User-defined modules and built-in shape primitives do; CSG/transform builtins do not.
     */
    public static boolean shouldAppendSemicolonOnModuleCompletion(@Nullable final Project project,
        @NotNull final String moduleName) {
        if (STATEMENT_ENDING_SHAPE_MODULES.contains(moduleName)) {
            return true;
        }
        if (project == null) {
            return false;
        }
        return findModuleDeclaration(project, moduleName) == null;
    }

    @NotNull
    private static Map<String, OpenSCADModuleDeclaration> getModuleDeclarations(@NotNull final Project project) {
        final long contentHash = BuiltinSkeletonResources.contentHash(MODULES_RESOURCE);
        if (moduleDeclarations == null || moduleSkeletonContentHash != contentHash) {
            final PsiFile skeleton = loadSkeleton(project, MODULES_RESOURCE);
            moduleDeclarations = indexModules(skeleton);
            positionalFirstArgumentModules = parsePositionalFirstArgumentModules(skeleton);
            moduleSkeletonContentHash = contentHash;
        }

        return moduleDeclarations;
    }

    @NotNull
    private static Set<String> getPositionalFirstArgumentModules() {
        if (positionalFirstArgumentModules == null) {
            final VirtualFile virtualFile = BuiltinSkeletonResources.findVirtualFile(MODULES_RESOURCE);
            if (virtualFile != null) {
                try {
                    positionalFirstArgumentModules =
                        parsePositionalFirstArgumentModules(virtualFile.contentsToByteArray());
                } catch (java.io.IOException ignored) {
                    positionalFirstArgumentModules = Set.of();
                }
            }

            if (positionalFirstArgumentModules == null) {
                positionalFirstArgumentModules = Set.of();
            }

            if (positionalFirstArgumentModules.isEmpty()) {
                positionalFirstArgumentModules = DEFAULT_POSITIONAL_FIRST_ARGUMENT_MODULES;
            }
        }

        return positionalFirstArgumentModules;
    }

    @NotNull
    private static Map<String, OpenSCADFunctionDeclaration> getFunctionDeclarations(@NotNull final Project project) {
        final long contentHash = BuiltinSkeletonResources.contentHash(FUNCTIONS_RESOURCE);
        if (functionDeclarations == null || functionSkeletonContentHash != contentHash) {
            functionDeclarations = indexFunctions(loadSkeleton(project, FUNCTIONS_RESOURCE));
            functionSkeletonContentHash = contentHash;
        }

        return functionDeclarations;
    }

    @Nullable
    private static PsiFile loadSkeleton(@NotNull final Project project, @NotNull final String resourcePath) {
        final VirtualFile virtualFile = BuiltinSkeletonResources.findVirtualFile(resourcePath);
        if (virtualFile == null) {
            return null;
        }

        return PsiManager.getInstance(project).findFile(virtualFile);
    }

    @NotNull
    private static Map<String, OpenSCADModuleDeclaration> indexModules(@Nullable final PsiFile skeleton) {
        if (skeleton == null) {
            return Map.of();
        }

        final Map<String, OpenSCADModuleDeclaration> result = new HashMap<>();
        for (final OpenSCADModuleDeclaration declaration :
            PsiTreeUtil.getChildrenOfTypeAsList(skeleton, OpenSCADModuleDeclaration.class)) {
            final String name = declaration.getName();
            if (name != null) {
                result.put(name, declaration);
            }
        }

        return result;
    }

    @NotNull
    private static Map<String, OpenSCADFunctionDeclaration> indexFunctions(@Nullable final PsiFile skeleton) {
        if (skeleton == null) {
            return Map.of();
        }

        final Map<String, OpenSCADFunctionDeclaration> result = new HashMap<>();
        for (final OpenSCADFunctionDeclaration declaration :
            PsiTreeUtil.getChildrenOfTypeAsList(skeleton, OpenSCADFunctionDeclaration.class)) {
            final String name = declaration.getName();
            if (name != null) {
                result.put(name, declaration);
            }
        }

        return result;
    }

    @NotNull
    private static Set<String> parsePositionalFirstArgumentModules(@Nullable final PsiFile skeleton) {
        if (skeleton == null) {
            return Set.of();
        }

        return parsePositionalFirstArgumentModules(skeleton.getText());
    }

    @NotNull
    private static Set<String> parsePositionalFirstArgumentModules(@NotNull final byte[] skeletonBytes) {
        return parsePositionalFirstArgumentModules(new String(skeletonBytes, StandardCharsets.UTF_8));
    }

    @NotNull
    private static Set<String> parsePositionalFirstArgumentModules(@NotNull final String skeletonText) {
        for (final String line : skeletonText.split("\n")) {
            final String trimmed = line.trim().replace("\uFEFF", "");
            if (!trimmed.contains(POSITIONAL_FIRST_ARGUMENT_MARKER)) {
                continue;
            }

            final int markerIndex = trimmed.indexOf(POSITIONAL_FIRST_ARGUMENT_MARKER);
            final String names = trimmed.substring(markerIndex + POSITIONAL_FIRST_ARGUMENT_MARKER.length()).trim();
            if (names.isEmpty()) {
                return Set.of();
            }

            return Arrays.stream(names.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
        }
        
        return Set.of();
    }
}
