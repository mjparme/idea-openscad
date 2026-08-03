package com.javampire.openscad.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
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

    private static final String MODULES_RESOURCE = "/com/javampire/openscad/skeletons/builtin_modules.scad";
    private static final String FUNCTIONS_RESOURCE = "/com/javampire/openscad/skeletons/builtin_functions.scad";
    private static final String POSITIONAL_FIRST_ARGUMENT_MARKER = "// POSITIONAL_FIRST_ARGUMENT:";
    private static final Set<String> DEFAULT_POSITIONAL_FIRST_ARGUMENT_MODULES =
            Set.of("cube", "sphere", "rotate", "translate");

    private static Map<String, OpenSCADModuleDeclaration> moduleDeclarations;
    private static Map<String, OpenSCADFunctionDeclaration> functionDeclarations;
    private static Set<String> positionalFirstArgumentModules;
    private static long moduleSkeletonStamp = -1;
    private static long functionSkeletonStamp = -1;

    private BuiltinSkeletons() {
    }

    public static void clearCaches() {
        moduleDeclarations = null;
        functionDeclarations = null;
        positionalFirstArgumentModules = null;
        moduleSkeletonStamp = -1;
        functionSkeletonStamp = -1;
    }

    @Nullable
    public static OpenSCADModuleDeclaration findModuleDeclaration(@NotNull final Project project,
                                                                  @NotNull final String name) {
        return getModuleDeclarations(project).get(name);
    }

    @Nullable
    public static OpenSCADFunctionDeclaration findFunctionDeclaration(@NotNull final Project project,
                                                                      @NotNull final String name) {
        return getFunctionDeclarations(project).get(name);
    }

    public static boolean isPositionalFirstArgumentModule(@NotNull final String name) {
        if (DEFAULT_POSITIONAL_FIRST_ARGUMENT_MODULES.contains(name)) {
            return true;
        }
        return getPositionalFirstArgumentModules().contains(name);
    }

    @NotNull
    private static Map<String, OpenSCADModuleDeclaration> getModuleDeclarations(@NotNull final Project project) {
        final PsiFile skeleton = loadSkeleton(project, MODULES_RESOURCE);
        final long stamp = skeletonStamp(skeleton);
        if (moduleDeclarations == null || moduleSkeletonStamp != stamp) {
            moduleDeclarations = indexModules(skeleton);
            positionalFirstArgumentModules = parsePositionalFirstArgumentModules(skeleton);
            moduleSkeletonStamp = stamp;
        }
        return moduleDeclarations;
    }

    @NotNull
    private static Set<String> getPositionalFirstArgumentModules() {
        if (positionalFirstArgumentModules == null) {
            final URL resourceUrl = BuiltinSkeletons.class.getResource(MODULES_RESOURCE);
            if (resourceUrl != null) {
                final VirtualFile virtualFile = VfsUtil.findFileByURL(resourceUrl);
                if (virtualFile != null) {
                    try {
                        positionalFirstArgumentModules =
                                parsePositionalFirstArgumentModules(VfsUtil.loadText(virtualFile));
                    }
                    catch (java.io.IOException ignored) {
                        positionalFirstArgumentModules = Set.of();
                    }
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
        final PsiFile skeleton = loadSkeleton(project, FUNCTIONS_RESOURCE);
        final long stamp = skeletonStamp(skeleton);
        if (functionDeclarations == null || functionSkeletonStamp != stamp) {
            functionDeclarations = indexFunctions(skeleton);
            functionSkeletonStamp = stamp;
        }
        return functionDeclarations;
    }

    private static long skeletonStamp(@Nullable final PsiFile skeleton) {
        if (skeleton == null) {
            return -1;
        }
        final VirtualFile virtualFile = skeleton.getVirtualFile();
        return virtualFile != null ? virtualFile.getModificationStamp() : -1;
    }

    @Nullable
    private static PsiFile loadSkeleton(@NotNull final Project project, @NotNull final String resourcePath) {
        final URL resourceUrl = BuiltinSkeletons.class.getResource(resourcePath);
        if (resourceUrl == null) {
            return null;
        }
        final VirtualFile virtualFile = VfsUtil.findFileByURL(resourceUrl);
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
