package com.javampire.openscad.completion;

import static com.javampire.openscad.parser.OpenSCADParserTokenSets.FUNCTION_KEYWORDS;
import static com.javampire.openscad.parser.OpenSCADParserTokenSets.WITH_FULL_ARG_DECLARATION_LIST;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.javampire.openscad.OpenSCADFileType;
import com.javampire.openscad.OpenSCADLanguage;
import com.javampire.openscad.psi.OpenSCADArgAssignmentList;
import com.javampire.openscad.psi.BuiltinSkeletons;
import com.javampire.openscad.psi.BuiltinSkeletonResources;
import com.javampire.openscad.psi.OpenSCADArgDeclaration;
import com.javampire.openscad.psi.OpenSCADArgDeclarationList;
import com.javampire.openscad.psi.OpenSCADExpr;
import com.javampire.openscad.psi.OpenSCADFullArgDeclaration;
import com.javampire.openscad.psi.OpenSCADFullArgDeclarationList;
import com.javampire.openscad.psi.OpenSCADFunctionDeclaration;
import com.javampire.openscad.psi.OpenSCADImportUtil;
import com.javampire.openscad.psi.OpenSCADModuleDeclaration;
import com.javampire.openscad.psi.OpenSCADPsiImplUtil;
import com.javampire.openscad.psi.OpenSCADTypes;
import com.javampire.openscad.psi.OpenSCADVariableDeclaration;
import com.javampire.openscad.references.OpenSCADResolver;
import com.javampire.openscad.settings.OpenSCADSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class OpenSCADCompletionContributor extends CompletionContributor {
    private static final Logger LOG = Logger.getInstance(OpenSCADCompletionContributor.class);

    private record ModuleLookupObject(@NotNull OpenSCADModuleDeclaration module, boolean fillNamedArguments) {
    }

    private record ModuleParameterInfo(@NotNull String name, @Nullable String defaultValueText) {
        @NotNull
        String toCallSiteAssignment() {
            if (defaultValueText != null && !defaultValueText.isEmpty()) {
                return name + " = " + defaultValueText;
            }
            return name + " = ";
        }

        @NotNull
        String toCallSiteAssignment(final int index, final boolean positionalFirstArgumentModule) {
            if (positionalFirstArgumentModule && index == 0) {
                if (defaultValueText != null && !defaultValueText.isEmpty()) {
                    return defaultValueText;
                }
                return toCallSiteAssignment();
            }
            return toCallSiteAssignment();
        }
    }

    private record CachedModuleLookupObject(@NotNull String moduleName, boolean fillNamedArguments) {
    }

    private record CachedModuleInfo(@NotNull String name, @NotNull List<ModuleParameterInfo> parameters, @NotNull javax.swing.Icon icon) {
    }

    private static final InsertHandler<LookupElement> MODULE_FILL_INSERT_HANDLER =
            (context, item) -> fillModuleCallFromLookup(context, item);

    private static final InsertHandler<LookupElement> MODULE_PAREN_INSERT_HANDLER = (context, item) -> {
        final String moduleName = resolveModuleName(item, context.getProject());
        final List<ModuleParameterInfo> parameters = resolveModuleParameters(item, context.getProject(), moduleName);
        if (parameters.isEmpty()) {
            insertEmptyModuleCall(context);
        }
        else {
            insertParameterizedModuleCall(context);
        }
    };

    private static void fillModuleCallFromLookup(@NotNull final InsertionContext context,
                                                   @NotNull final LookupElement item) {
        final String moduleName = resolveModuleName(item, context.getProject());
        final List<ModuleParameterInfo> parameters = resolveModuleParameters(item, context.getProject(), moduleName);
        if (parameters.isEmpty()) {
            insertEmptyModuleCall(context);
            return;
        }
        insertFilledModuleCall(
                context,
                parameters,
                BuiltinSkeletons.isPositionalFirstArgumentModule(moduleName));
    }

    @NotNull
    private static String resolveModuleName(@NotNull final LookupElement item, @Nullable final Project project) {
        final Object lookupObject = unwrapLookupObject(item);
        if (lookupObject instanceof CachedModuleLookupObject cachedLookup) {
            return cachedLookup.moduleName();
        }
        if (lookupObject instanceof ModuleLookupObject moduleLookup) {
            final String name = moduleLookup.module().getName();
            if (name != null) {
                return name;
            }
        }
        return stripModuleLookupSuffix(item.getLookupString());
    }

    @NotNull
    private static List<ModuleParameterInfo> resolveModuleParameters(@NotNull final LookupElement item,
                                                                     @Nullable final Project project,
                                                                     @NotNull final String moduleName) {
        final Object lookupObject = unwrapLookupObject(item);
        if (lookupObject instanceof CachedModuleLookupObject) {
            return project != null ? getBuiltinModuleParameters(project, moduleName) : List.of();
        }
        if (lookupObject instanceof ModuleLookupObject moduleLookup) {
            return getModuleParameters(moduleLookup.module());
        }
        if (project != null) {
            final List<ModuleParameterInfo> builtinParameters = getBuiltinModuleParameters(project, moduleName);
            if (!builtinParameters.isEmpty()) {
                return builtinParameters;
            }
        }
        return List.of();
    }

    private record ModuleLookupData(@NotNull List<ModuleParameterInfo> parameters,
                                    boolean fillNamedArguments,
                                    boolean positionalFirstArgument,
                                    @NotNull String moduleName) {
    }

    private static ModuleLookupData getModuleLookupData(@NotNull final LookupElement item,
                                                        @Nullable final Project project) {
        final Object lookupObject = unwrapLookupObject(item);
        List<ModuleParameterInfo> parameters = List.of();
        boolean fillNamedArguments = false;
        String moduleName = stripModuleLookupSuffix(item.getLookupString());

        if (lookupObject instanceof ModuleLookupObject moduleLookup) {
            parameters = getModuleParameters(moduleLookup.module());
            fillNamedArguments = moduleLookup.fillNamedArguments();
            final String name = moduleLookup.module().getName();
            if (name != null) {
                moduleName = name;
            }
        }
        else if (lookupObject instanceof CachedModuleLookupObject cachedLookup) {
            fillNamedArguments = cachedLookup.fillNamedArguments();
            moduleName = cachedLookup.moduleName();
            if (project != null) {
                parameters = getBuiltinModuleParameters(project, moduleName);
            }
        }

        if (parameters.isEmpty() && fillNamedArguments && project != null) {
            parameters = getBuiltinModuleParameters(project, moduleName);
        }

        final boolean positionalFirst = BuiltinSkeletons.isPositionalFirstArgumentModule(moduleName);
        return new ModuleLookupData(parameters, fillNamedArguments, positionalFirst, moduleName);
    }

    @Nullable
    private static Object unwrapLookupObject(@NotNull final LookupElement item) {
        LookupElement current = item;
        while (current instanceof com.intellij.codeInsight.lookup.LookupElementDecorator decorator) {
            current = decorator.getDelegate();
        }
        final Object object = current.getObject();
        if (object instanceof LookupElement nested) {
            return nested.getObject();
        }
        return object;
    }

    @NotNull
    private static String stripModuleLookupSuffix(@NotNull final String lookupString) {
        final int suffixIndex = lookupString.indexOf(MODULE_WITH_ARGS_SUFFIX);
        if (suffixIndex >= 0) {
            return lookupString.substring(0, suffixIndex).trim();
        }
        return lookupString.trim();
    }

    @NotNull
    private static List<ModuleParameterInfo> getBuiltinModuleParameters(@NotNull final Project project,
                                                                        @NotNull final String moduleName) {
        final OpenSCADModuleDeclaration declaration = BuiltinSkeletons.findModuleDeclaration(project, moduleName);
        if (declaration == null) {
            return List.of();
        }
        return getModuleParameters(declaration);
    }

    private static final String MODULE_WITH_ARGS_SUFFIX = " (with args)";

    private static final String _FROM_ = " from ";
    private static final String BUILT_IN_MODULES_FILENAME = BuiltinSkeletonResources.MODULES_RESOURCE;
    private static final String BUILT_IN_FUNCTIONS_FILENAME = "/com/javampire/openscad/skeletons/builtin_functions.scad";
    private static final String BUILT_IN_SPECIAL_VARIABLES_FILENAME = "/com/javampire/openscad/skeletons/builtin_special_variables.scad";

    private static List<CachedModuleInfo> builtinModules;
    private static long builtinModulesContentHash = -1;
    private static List<LookupElement> builtinFunctions;
    private static List<LookupElement> builtinSpecialVariables;
    private static List<GlobalLibraryEntry> globalLibraryEntries;

    private record GlobalLibraryEntry(@NotNull PsiFile file, @NotNull String tailText) {
    }

    public OpenSCADCompletionContributor() {
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(OpenSCADLanguage.INSTANCE),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {

                        final Project project = parameters.getOriginalFile().getProject();
                        final PsiElement elementPosition = parameters.getPosition();

                        // No autocompletion when editing argument lists
                        if (OpenSCADTypes.ARG_DECLARATION == elementPosition.getParent().getNode().getElementType()) {
                            return;
                        }

                        // No autocompletion on literal
                        if (OpenSCADTypes.LITERAL_EXPR == elementPosition.getParent().getNode().getElementType()) {
                            return;
                        }

                        // No autocompletion for numbers
                        if ("".equals(result.getPrefixMatcher().getPrefix())) {
                            PsiElement previousElement = elementPosition.getParent().getPrevSibling();
                            if (previousElement != null) {
                                previousElement = previousElement.getLastChild();
                                if (previousElement instanceof PsiErrorElement) {
                                    previousElement = previousElement.getPrevSibling();
                                }
                                if (previousElement != null && "ERROR_ELEMENT".equals(previousElement.getNode().getElementType().toString())) {
                                    previousElement = elementPosition.getParent().getLastChild().getPrevSibling().getLastChild();
                                }
                                if (previousElement != null && OpenSCADTypes.NUMBER_LITERAL == previousElement.getNode().getElementType()) {
                                    return;
                                }
                            }
                        }

                        final boolean fillNamedArgumentsOnPrimaryCompletion = OpenSCADSettings.getInstance().isFillNamedArgumentsOnModuleCompletion();

                        // Add all accessible variables in the current file
                        addAccessibleVariables(result, elementPosition, null);
                        ProgressManager.checkCanceled();

                        // Add all parent arguments (from declaration lists)
                        addAccessibleArgumentDeclarations(result, elementPosition);
                        ProgressManager.checkCanceled();

                        // Add callee parameters when completing named call arguments
                        addCalleeArgumentDeclarations(result, elementPosition);
                        ProgressManager.checkCanceled();

                        // Add all accessible variables in includes
                        addIncludesAccessibleVariables(result, parameters.getOriginalFile());
                        ProgressManager.checkCanceled();

                        // Add local custom modules
                        addModules(result, elementPosition, null, fillNamedArgumentsOnPrimaryCompletion);
                        ProgressManager.checkCanceled();

                        // Add local custom functions
                        addFunctions(result, elementPosition, null);
                        ProgressManager.checkCanceled();

                        // Add builtin modules and functions
                        addBuiltinModules(project, result, fillNamedArgumentsOnPrimaryCompletion);
                        addBuiltinFunctions(project, result);
                        addBuiltinSpecialVariables(project, result);
                        ProgressManager.checkCanceled();

                        // Add declared library methods and functions
                        addLocalLibrariesModulesAndFunctions(result, parameters.getOriginalFile(), fillNamedArgumentsOnPrimaryCompletion);
                        ProgressManager.checkCanceled();

                        if (!parameters.isAutoPopup() && parameters.getInvocationCount() >= 1) {
                            addGlobalLibrariesModulesAndFunctions(result, project, fillNamedArgumentsOnPrimaryCompletion);
                        } else {
                            final String completionShortcut = KeymapUtil.getFirstKeyboardShortcutText(
                                    ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION));
                            result.addLookupAdvertisement("Press " + completionShortcut + " to add modules/functions from global libraries.");
                        }
                    }
                }
        );
    }

    /**
     * Get accessible variables declarations for the current element.
     *
     * @param result   Result set.
     * @param element  Psi element.
     * @param tailText Tail text to show in the result.
     */
    private void addAccessibleVariables(final CompletionResultSet result, final PsiElement element, final String tailText) {
        final List<OpenSCADVariableDeclaration> variableDeclarations = OpenSCADPsiImplUtil.getAccessibleVariableDeclaration(element);
        result.addAllElements(convertToLookupElements(variableDeclarations, null));
    }

    /**
     * Get accessible variables declarations from parent argument list declarations, i.e. variables declared in function or module parameters.
     *
     * @param result  Result set.
     * @param element Psi element.
     */
    private void addAccessibleArgumentDeclarations(final CompletionResultSet result, final PsiElement element) {
        result.addAllElements(convertToLookupElements(OpenSCADPsiImplUtil.getAccessibleArgumentDeclarations(element), null));

        // Parents with FULL_ARG_DECLARATION_LIST : for loop
        final List<PsiElement> fullArgDeclarationParents = OpenSCADPsiImplUtil.getParentsOfType(element, WITH_FULL_ARG_DECLARATION_LIST);
        final List<PsiElement> fullArgDeclarations = fullArgDeclarationParents.stream()
                .map(e -> PsiTreeUtil.getChildOfType(e, OpenSCADFullArgDeclarationList.class))
                .filter(Objects::nonNull)
                .map(e -> PsiTreeUtil.getChildOfType(e, OpenSCADFullArgDeclaration.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        result.addAllElements(convertToLookupElements(fullArgDeclarations, null));

        // Parents previous sibling with FULL_ARG_DECLARATION_LIST : let declaration
        final List<PsiElement> builtinExprParents = OpenSCADPsiImplUtil.getParentsOfType(element, FUNCTION_KEYWORDS);
        final List<PsiElement> letFullArgDeclaration = builtinExprParents.stream()
                .map(PsiElement::getFirstChild)
                .filter(e -> e.getNode().getElementType() == OpenSCADTypes.LET_KEYWORD)
                .map(PsiElement::getNextSibling)
                .filter(Objects::nonNull)
                .filter(e -> e instanceof OpenSCADFullArgDeclarationList)
                .map(e -> PsiTreeUtil.getChildrenOfType(e, OpenSCADFullArgDeclaration.class))
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        result.addAllElements(convertToLookupElements(letFullArgDeclaration, null));
    }

    private void addCalleeArgumentDeclarations(final CompletionResultSet result, final PsiElement element) {
        final OpenSCADArgAssignmentList argList = PsiTreeUtil.getParentOfType(element, OpenSCADArgAssignmentList.class);
        if (argList == null) {
            return;
        }
        result.addAllElements(convertToLookupElements(OpenSCADPsiImplUtil.getCalleeArgumentDeclarations(argList), null));
    }

    /**
     * Get accessible variables declaration from included files for the current element.
     *
     * @param result  Result set.
     * @param element Psi elements.
     */
    private void addIncludesAccessibleVariables(final CompletionResultSet result, final PsiFile file) {
        final List<LookupElement> importedVariables = new ArrayList<>();
        final Set<PsiFile> visitedFiles = new HashSet<>();
        OpenSCADImportUtil.collectIncludedVariables(
                file,
                visitedFiles,
                (variable, tailText) -> importedVariables.addAll(convertToLookupElements(List.of(variable), tailText))
        );
        result.addAllElements(importedVariables);
    }

    /**
     * Get module declarations.
     *
     * @param result   Result set.
     * @param element  Psi element.
     * @param tailText Tail text to display with completion result.
     */
    private void addModules(final CompletionResultSet result, final PsiElement element, final String tailText, final boolean fillNamedArguments) {
        result.addAllElements(getModules(element, tailText, fillNamedArguments));
    }

    /**
     * Get module declarations.
     *
     * @param element  Psi element.
     * @param tailText Tail text to display with completion result.
     * @return List of modules.
     */
    private List<LookupElement> getModules(final PsiElement element, final String tailText, final boolean fillNamedArguments) {
        final List<OpenSCADModuleDeclaration> moduleDeclarations = element instanceof PsiFile file
                ? OpenSCADPsiImplUtil.getFileModuleDeclarations(file)
                : OpenSCADPsiImplUtil.getAccessibleModuleDeclarations(element);

        return convertModuleToLookupElements(moduleDeclarations, tailText, fillNamedArguments);
    }

    /**
     * Get function declarations.
     *
     * @param result   Result set.
     * @param element  Psi element.
     * @param tailText Tail text to display with completion result.
     */
    private void addFunctions(final CompletionResultSet result, final PsiElement element, final String tailText) {
        result.addAllElements(getFunctions(element, tailText));
    }

    /**
     * Get function declarations.
     *
     * @param element  Psi element.
     * @param tailText Tail text to display with completion result.
     * @return List of functions.
     */
    private List<LookupElement> getFunctions(final PsiElement element, final String tailText) {
        final List<OpenSCADFunctionDeclaration> functionDeclarations = element instanceof PsiFile file
                ? OpenSCADPsiImplUtil.getFileFunctionDeclarations(file)
                : OpenSCADPsiImplUtil.getAccessibleFunctionDeclarations(element);
        return convertToLookupElements(functionDeclarations, tailText);
    }

    private void addBuiltinModules(@NotNull final Project project,
                                   @NotNull final CompletionResultSet result,
                                   final boolean fillNamedArguments) {
        final List<CachedModuleInfo> modules = getBuiltinModules(project);
        final List<LookupElement> elements = new ArrayList<>();
        for (CachedModuleInfo info : modules) {
            final LookupElement primary = toCachedModuleLookupElement(info, fillNamedArguments);
            if (primary != null) {
                elements.add(primary);
            }
            if (!fillNamedArguments && !info.parameters().isEmpty()) {
                final LookupElement withArgs = toCachedModuleWithArgsLookupElement(info);
                if (withArgs != null) {
                    elements.add(withArgs);
                }
            }
        }
        result.addAllElements(elements);
    }

    @NotNull
    private static List<CachedModuleInfo> getBuiltinModules(@NotNull final Project project) {
        final long contentHash = BuiltinSkeletonResources.contentHash(BUILT_IN_MODULES_FILENAME);
        if (builtinModules == null || builtinModulesContentHash != contentHash) {
            builtinModulesContentHash = contentHash;
            BuiltinSkeletons.clearCaches();
            final VirtualFile skeletonFile = BuiltinSkeletonResources.findVirtualFile(BUILT_IN_MODULES_FILENAME);
            final PsiFile moduleSkeleton = skeletonFile != null
                    ? PsiManager.getInstance(project).findFile(skeletonFile)
                    : null;
            if (moduleSkeleton == null) {
                LOG.warn("Can not parse builtin modules skeleton file, completion will not be available on modules.");
                builtinModules = List.of();
            } else {
                builtinModules = PsiTreeUtil.getChildrenOfTypeAsList(moduleSkeleton, OpenSCADModuleDeclaration.class).stream()
                        .map(OpenSCADCompletionContributor::toCachedModuleInfo)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        }
        return builtinModules;
    }

    public static void clearBuiltinModuleCompletionCache() {
        builtinModules = null;
        builtinModulesContentHash = -1;
    }

    @Nullable
    private static VirtualFile findBuiltinModulesVirtualFile() {
        return BuiltinSkeletonResources.findVirtualFile(BUILT_IN_MODULES_FILENAME);
    }

    private void addBuiltinFunctions(@NotNull final Project project, @NotNull final CompletionResultSet result) {
        if (builtinFunctions == null) {
            // Add builtin functions
            final PsiFile functionSkeleton = PsiManager.getInstance(project).findFile(
                    VfsUtil.findFileByURL(getClass().getResource(BUILT_IN_FUNCTIONS_FILENAME))
            );
            if (functionSkeleton == null) {
                LOG.warn("Can not parse builtin functions skeleton file, completion will not be available on functions.");
            } else {
                builtinFunctions = getFunctions(functionSkeleton, null);
            }
        }
        result.addAllElements(builtinFunctions);
    }

    private void addBuiltinSpecialVariables(@NotNull final Project project, @NotNull final CompletionResultSet result) {
        if (builtinSpecialVariables == null) {
            final PsiFile specialVariablesSkeleton = PsiManager.getInstance(project).findFile(
                    VfsUtil.findFileByURL(getClass().getResource(BUILT_IN_SPECIAL_VARIABLES_FILENAME))
            );
            if (specialVariablesSkeleton == null) {
                LOG.warn("Can not parse builtin special variables skeleton file, completion will not be available on special variables.");
                builtinSpecialVariables = List.of();
            } else {
                builtinSpecialVariables = convertToLookupElements(
                        PsiTreeUtil.getChildrenOfTypeAsList(specialVariablesSkeleton, OpenSCADVariableDeclaration.class),
                        null
                );
            }
        }
        result.addAllElements(builtinSpecialVariables);
    }

    /**
     * Get edited file include and use declaration targets.
     *
     * @param result  Result set.
     * @param element Current element.
     */
    private void addLocalLibrariesModulesAndFunctions(final CompletionResultSet result,
                                                      final PsiFile file,
                                                      final boolean fillNamedArguments) {
        final List<LookupElement> imports = new ArrayList<>();
        final Set<PsiFile> visitedFiles = new HashSet<>();
        OpenSCADImportUtil.collectImportedModulesAndFunctions(
                file,
                visitedFiles,
                symbol -> imports.addAll(toImportedSymbolLookupElements(symbol, fillNamedArguments))
        );
        result.addAllElements(imports);
    }

    @NotNull
    private List<LookupElement> toImportedSymbolLookupElements(@NotNull final OpenSCADImportUtil.ImportedSymbol symbol,
                                                               final boolean fillNamedArguments) {
        final PsiElement declaration = symbol.declaration();
        if (declaration instanceof OpenSCADModuleDeclaration module) {
            return convertModuleToLookupElements(List.of(module), symbol.sourceTailText(), fillNamedArguments);
        }
        if (declaration instanceof OpenSCADFunctionDeclaration function) {
            return convertToLookupElements(List.of(function), symbol.sourceTailText());
        }
        return List.of();
    }

    private void addGlobalLibrariesModulesAndFunctions(final CompletionResultSet result,
                                                       final Project project,
                                                       final boolean fillNamedArguments) {
        if (globalLibraryEntries == null) {
            final PsiManager psiManager = PsiManager.getInstance(project);
            final ModifiableModelsProvider modelsProvider = ApplicationManager.getApplication().getService(ModifiableModelsProvider.class);
            final Library[] librariesPathRoots = modelsProvider.getLibraryTableModifiableModel().getLibraries();
            final List<VirtualFile> librariesPaths = Arrays.stream(librariesPathRoots)
                    .map(libraryPathsRoot -> libraryPathsRoot.getFiles(OrderRootType.CLASSES))
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList());

            globalLibraryEntries = new ArrayList<>();
            for (VirtualFile librariesPath : librariesPaths) {
                final List<PsiFile> libraries = VfsUtil.collectChildrenRecursively(librariesPath).stream()
                        .map(psiManager::findFile)
                        .filter(Objects::nonNull)
                        .filter(PsiElement::isValid)
                        .filter(psiFile -> psiFile.getFileType() == OpenSCADFileType.INSTANCE)
                        .collect(Collectors.toList());

                for (PsiFile library : libraries) {
                    final String libraryRelPath = library.getVirtualFile().getCanonicalPath()
                            .substring(librariesPath.getCanonicalPath().length() + 1);
                    globalLibraryEntries.add(new GlobalLibraryEntry(library, _FROM_ + libraryRelPath));
                }
            }
        }

        final List<LookupElement> imports = new ArrayList<>();
        for (GlobalLibraryEntry entry : globalLibraryEntries) {
            imports.addAll(getModules(entry.file(), entry.tailText(), fillNamedArguments));
            imports.addAll(getFunctions(entry.file(), entry.tailText()));
        }
        result.addAllElements(imports);
    }

    private <T extends PsiElement> List<LookupElement> convertToLookupElements(final List<T> elements, final String tailText) {
        return elements.stream()
                .map(element -> {
                    final ItemPresentation presentation = OpenSCADPsiImplUtil.getPresentation(element);
                    final String text = presentation.getPresentableText();
                    if (text == null) {
                        return null;
                    }
                    LookupElementBuilder builder = LookupElementBuilder
                            .create(text)
                            .withIcon(presentation.getIcon(true));
                    if (tailText != null) {
                        builder = builder.appendTailText(tailText, true);
                    }
                    return builder;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<LookupElement> convertModuleToLookupElements(final List<OpenSCADModuleDeclaration> modules,
                                                              final String tailText,
                                                              final boolean fillNamedArguments) {
        final List<LookupElement> elements = new ArrayList<>();
        for (OpenSCADModuleDeclaration module : modules) {
            final LookupElement primary = toModuleLookupElement(module, tailText, fillNamedArguments);
            if (primary != null) {
                elements.add(primary);
            }
            if (!fillNamedArguments && !getModuleParameters(module).isEmpty()) {
                final LookupElement withArgs = toModuleWithArgsLookupElement(module, tailText);
                if (withArgs != null) {
                    elements.add(withArgs);
                }
            }
        }
        return elements;
    }

    private static LookupElement toModuleLookupElement(@NotNull final OpenSCADModuleDeclaration module,
                                                       final String tailText,
                                                       final boolean fillNamedArguments) {
        final ItemPresentation presentation = OpenSCADPsiImplUtil.getPresentation(module);
        final String text = presentation.getPresentableText();
        if (text == null) {
            return null;
        }
        LookupElementBuilder builder = LookupElementBuilder
                .create(new ModuleLookupObject(module, fillNamedArguments), text)
                .withIcon(presentation.getIcon(true))
                .withInsertHandler(fillNamedArguments ? MODULE_FILL_INSERT_HANDLER : MODULE_PAREN_INSERT_HANDLER);
        if (tailText != null) {
            builder = builder.appendTailText(tailText, true);
        }
        return builder;
    }

    private static LookupElement toModuleWithArgsLookupElement(@NotNull final OpenSCADModuleDeclaration module,
                                                               final String tailText) {
        final ItemPresentation presentation = OpenSCADPsiImplUtil.getPresentation(module);
        final String text = presentation.getPresentableText();
        if (text == null) {
            return null;
        }
        LookupElementBuilder builder = LookupElementBuilder
                .create(new ModuleLookupObject(module, true), text)
                .withIcon(presentation.getIcon(true))
                .appendTailText(MODULE_WITH_ARGS_SUFFIX, true)
                .withInsertHandler(MODULE_FILL_INSERT_HANDLER);
        if (tailText != null) {
            builder = builder.appendTailText(tailText, true);
        }
        return builder;
    }

    private static CachedModuleInfo toCachedModuleInfo(@NotNull final OpenSCADModuleDeclaration module) {
        final ItemPresentation presentation = OpenSCADPsiImplUtil.getPresentation(module);
        final String text = presentation.getPresentableText();
        if (text == null) {
            return null;
        }
        return new CachedModuleInfo(text, getModuleParameters(module), presentation.getIcon(true));
    }

    private static LookupElement toCachedModuleLookupElement(@NotNull final CachedModuleInfo moduleInfo,
                                                             final boolean fillNamedArguments) {
        return LookupElementBuilder
                .create(new CachedModuleLookupObject(moduleInfo.name(), fillNamedArguments), moduleInfo.name())
                .withIcon(moduleInfo.icon())
                .withInsertHandler(fillNamedArguments ? MODULE_FILL_INSERT_HANDLER : MODULE_PAREN_INSERT_HANDLER);
    }

    private static LookupElement toCachedModuleWithArgsLookupElement(@NotNull final CachedModuleInfo moduleInfo) {
        return LookupElementBuilder
                .create(new CachedModuleLookupObject(moduleInfo.name(), true), moduleInfo.name())
                .withIcon(moduleInfo.icon())
                .appendTailText(MODULE_WITH_ARGS_SUFFIX, true)
                .withInsertHandler(MODULE_FILL_INSERT_HANDLER);
    }

    @NotNull
    private static List<ModuleParameterInfo> getModuleParameters(@NotNull final OpenSCADModuleDeclaration module) {
        final OpenSCADArgDeclarationList argList = module.getArgDeclarationList();
        if (argList == null) {
            return List.of();
        }
        return PsiTreeUtil.getChildrenOfTypeAsList(argList, OpenSCADArgDeclaration.class).stream()
                .map(OpenSCADCompletionContributor::toModuleParameterInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Nullable
    private static ModuleParameterInfo toModuleParameterInfo(@NotNull final OpenSCADArgDeclaration declaration) {
        final String name = declaration.getName();
        if (name == null) {
            return null;
        }
        final OpenSCADExpr defaultExpr = declaration.getExpr();
        final String defaultValueText = defaultExpr != null ? defaultExpr.getText() : null;
        return new ModuleParameterInfo(name, defaultValueText);
    }

    private static void insertEmptyModuleCall(@NotNull final InsertionContext context) {
        final Editor editor = context.getEditor();
        final Document document = editor.getDocument();
        final int offset = context.getTailOffset();
        if (offset < document.getTextLength() && document.getText().charAt(offset) == '(') {
            moveCaretAfterModuleCallParentheses(context);
            return;
        }
        document.insertString(offset, "()");
        editor.getCaretModel().moveToOffset(offset + 2);
    }

    private static void insertParameterizedModuleCall(@NotNull final InsertionContext context) {
        final Editor editor = context.getEditor();
        final Document document = editor.getDocument();
        final int offset = context.getTailOffset();
        if (offset < document.getTextLength() && document.getText().charAt(offset) == '(') {
            moveCaretAfterModuleCallParentheses(context);
            return;
        }
        document.insertString(offset, "()");
        editor.getCaretModel().moveToOffset(offset + 2);
    }

    private static void moveCaretAfterModuleCallParentheses(@NotNull final InsertionContext context) {
        final Editor editor = context.getEditor();
        final Document document = editor.getDocument();
        final int offset = context.getTailOffset();
        if (offset >= document.getTextLength() || document.getText().charAt(offset) != '(') {
            return;
        }
        int caretOffset = offset + 1;
        if (caretOffset < document.getTextLength() && document.getText().charAt(caretOffset) == ')') {
            caretOffset++;
        }
        editor.getCaretModel().moveToOffset(caretOffset);
    }

    private static void insertFilledModuleCall(@NotNull final InsertionContext context,
                                               @NotNull final List<ModuleParameterInfo> parameters,
                                               final boolean positionalFirstArgument) {
        final Editor editor = context.getEditor();
        final Document document = editor.getDocument();
        final int offset = context.getTailOffset();
        final String argumentList = buildCallSiteArgumentList(parameters, positionalFirstArgument);

        if (offset < document.getTextLength() && document.getText().charAt(offset) == '(') {
            final int insertOffset = offset + 1;
            final boolean hasClosingParen = insertOffset < document.getTextLength()
                    && document.getText().charAt(insertOffset) == ')';
            document.insertString(insertOffset, argumentList);
            if (!hasClosingParen) {
                document.insertString(insertOffset + argumentList.length(), ")");
            }
            editor.getCaretModel().moveToOffset(offset + argumentList.length() + 2);
            return;
        }

        document.insertString(offset, "(" + argumentList + ")");
        editor.getCaretModel().moveToOffset(offset + argumentList.length() + 2);
    }

    @NotNull
    private static String buildCallSiteArgumentList(@NotNull final List<ModuleParameterInfo> parameters,
                                                    final boolean positionalFirstArgument) {
        final List<String> assignments = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            assignments.add(parameters.get(i).toCallSiteAssignment(i, positionalFirstArgument));
        }
        return String.join(", ", assignments);
    }

    static boolean lookupElementFillsNamedArguments(@NotNull final LookupElement item) {
        return getModuleLookupData(item, null).fillNamedArguments();
    }
}
