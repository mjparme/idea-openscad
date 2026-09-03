package com.javampire.openscad.psi;

import static com.javampire.openscad.parser.OpenSCADParserTokenSets.DOC_IN_PARENT;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PlatformIcons;
import com.javampire.openscad.OpenSCADIcons;
import com.javampire.openscad.parser.OpenSCADParserTokenSets;
import com.javampire.openscad.psi.stub.function.OpenSCADFunctionStubElementType;
import com.javampire.openscad.psi.stub.module.OpenSCADModuleStubElementType;
import com.javampire.openscad.psi.stub.variable.OpenSCADVariableStubElementType;
import com.javampire.openscad.psi.OpenSCADTypes;
import com.javampire.openscad.references.OpenSCADParameterCallReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.*;

public class OpenSCADPsiImplUtil {

    private static final Logger LOG = Logger.getInstance(OpenSCADPsiImplUtil.class);

    private static final Pattern MULTILINE_PATTERN = Pattern.compile("\\R");

    public static ItemPresentation getPresentation(@NotNull final PsiElement element) {
        return new ItemPresentation() {
            @Nullable
            @Override
            public String getPresentableText() {
                if (element instanceof OpenSCADNamedElement) {
                    return ((OpenSCADNamedElement) element).getName();
                }
                return null;
            }

            @Override
            public String getLocationString() {
                return element.getContainingFile().getName();
            }

            @Nullable
            @Override
            public Icon getIcon(boolean unused) {
                if (element.getNode().getElementType() == OpenSCADTypes.MODULE_DECLARATION) {
                    return OpenSCADIcons.MODULE;
                }
                if (element.getNode().getElementType() == OpenSCADTypes.FUNCTION_DECLARATION) {
                    return OpenSCADIcons.FUNCTION;
                }
                if (element.getNode().getElementType() == OpenSCADTypes.VARIABLE_DECLARATION) {
                    return PlatformIcons.VARIABLE_ICON;
                }
                if (element.getNode().getElementType() == OpenSCADTypes.IMPORT) {
                    return OpenSCADIcons.OPENSCAD_LOGO;
                }
                return null;
            }
        };
    }

    public static PsiElement setName(@NotNull final PsiElement element, @NotNull final String newName) {
        if (OpenSCADParserTokenSets.NON_RENAMABLE_ELEMENTS.contains(element.getNode().getElementType())) {
            throw new IncorrectOperationException("Builtin functions/modules can't be renamed");
        }

        return OpenSCADNamedElementImpl.setName(element, newName);
    }

    public static PsiElement getNameIdentifier(@NotNull PsiElement element) {
        final ASTNode nameNode = OpenSCADNamedElementImpl.getNameNode(element.getNode());
        if (nameNode != null) {
            return nameNode.getPsi();
        }

        return null;
    }

    public static int getTextOffset(@NotNull PsiElement element) {
        final PsiElement nameIdentifier = getNameIdentifier(element);
        if (nameIdentifier != null) {
            return nameIdentifier.getTextOffset();
        }

        return element.getTextRange().getStartOffset();
    }

    public static PsiReference getReference(PsiElement element) {
        if (element instanceof OpenSCADParameterReference parameterReference) {
            return new OpenSCADParameterCallReference(parameterReference);
        }

        LOG.debug("Unhandled reference element: " + element);
        return null;
    }

    /**
     * Builds the declaration of a module or a function, consisting of name + argument list
     *
     * @param element a module or a function
     * @return name(arg1, ...)
     */
    public static String getNameWithArgumentList(@NotNull OpenSCADNamedElement element, boolean shortForm) {
        StringBuilder buf = new StringBuilder();
        buf.append(element.getName());
        final ASTNode argListNode = element.getNode().findChildByType(OpenSCADTypes.ARG_DECLARATION_LIST);
        if (argListNode == null) {
            buf.append("()");
        } else if (argListNode.getTextLength() > 100 && shortForm) {
            buf.append("(...)");
        } else {
            buf.append(argListNode.getText());
        }

        return buf.toString();
    }

    private static boolean isMultiLine(@NotNull PsiElement element) {
        return MULTILINE_PATTERN.matcher(element.getText()).find();
    }

    @Nullable
    public static String getDocString(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        final ASTNode node = element.getNode();
        if (node == null) {
            return null;
        }

        if (DOC_IN_PARENT.contains(node.getElementType())) {
            return getDocString(element.getParent());
        }

        final PsiReference reference = element.getReference();
        if (reference != null) {
            return getDocString(reference.resolve());
        }

        PsiElement docElement = PsiTreeUtil.skipWhitespacesBackward(element);
        if (docElement == null) {
            return null;
        }

        ASTNode docNode = docElement.getNode();
        if (docNode == null) {
            return null;
        }
        String text = docElement.getText();
        if (text == null) {
            return null;
        }

        IElementType docNodeElementType = docNode.getElementType();
        if (docNodeElementType == OpenSCADTypes.COMMENT_SINGLELINE_BLOCK) {
            text = text.replaceAll("(?sm)^\\s*//", "");
        } else if (docNodeElementType != OpenSCADTypes.COMMENT_DOC) {
            text = null;
        } else {
            text = text.replaceFirst("(?s)^\\s*/\\*\\*", "");
            text = text.replaceFirst("(?s)\\s*\\*/\\s*$", "");
            text = text.replaceAll("(?sm)^\\s*\\*", "");
        }

        // If there's no documentation comment placed before the element, and if the element
        // is on one line with an end of line comment, take that comment as documentation
        if (text == null && !isMultiLine(element)) {
            final PsiElement nextComment = PsiTreeUtil.skipWhitespacesForward(element);
            if (nextComment == null) {
                return null;
            }
            final ASTNode commentNode = nextComment.getNode();
            if (commentNode == null) {
                return null;
            }
            if (commentNode.getElementType() == OpenSCADTypes.COMMENT_SINGLELINE) {
                for (PsiElement wsElement : PsiTreeUtil.getElementsOfRange(element, nextComment)) {
                    if (isMultiLine(wsElement)) {
                        return null;
                    }
                }
                text = commentNode.getText();
                text = text.replaceAll("(?sm)^\\s*//", "");
            }
        }

        if (text != null) {
            text = text.replaceAll("<", "&lt;");
            text = text.replaceAll(">", "&gt;");
            text = "<pre>" + text + "</pre>";
        }

        LOG.debug("Help text: " + text);
        return text;
    }


    /**
     * Recursively get all variables declaration accessible to element, i.e. before element and before its parents.
     *
     * @param element Element for which accessible variables will be returned.
     * @return List of accessible variable declarations.
     */
    public static List<OpenSCADVariableDeclaration> getAccessibleVariableDeclaration(@NotNull final PsiElement element) {
        final PsiElement parent = element.getParent();

        // Get parent accessible variables if any
        List<OpenSCADVariableDeclaration> variableDeclarationsInParent = (parent == null || parent instanceof PsiFileBase) ? new ArrayList<>() : getAccessibleVariableDeclaration(parent);

        // Loop from first sibling to element (variables declared after elements are not accessible)
        if (parent != null) {
            for (PsiElement sibling = parent.getFirstChild(); sibling != null && sibling != element; sibling = sibling.getNextSibling()) {
                collectVariableDeclarations(sibling, variableDeclarationsInParent);
            }
        }

        return variableDeclarationsInParent;
    }

    /**
     * Returns parameter declarations from enclosing module and function declarations.
     */
    public static List<OpenSCADArgDeclaration> getAccessibleArgumentDeclarations(@NotNull final PsiElement element) {
        final List<OpenSCADArgDeclaration> result = new ArrayList<>();
        for (final PsiElement parent : getParentsOfType(element, OpenSCADParserTokenSets.WITH_ARG_DECLARATION_LIST)) {
            final OpenSCADArgDeclarationList argList = PsiTreeUtil.getChildOfType(parent, OpenSCADArgDeclarationList.class);
            if (argList != null) {
                result.addAll(PsiTreeUtil.getChildrenOfTypeAsList(argList, OpenSCADArgDeclaration.class));
            }
        }

        return result;
    }

    /**
     * Returns loop and let bindings from enclosing {@code for}/{@code let}/{@code assign} scopes.
     */
    public static List<OpenSCADFullArgDeclaration> getAccessibleFullArgDeclarations(@NotNull final PsiElement element) {
        final List<OpenSCADFullArgDeclaration> result = new ArrayList<>();
        for (final PsiElement parent : getParentsOfType(element, OpenSCADParserTokenSets.WITH_FULL_ARG_DECLARATION_LIST)) {
            if (parent instanceof OpenSCADForObj forObj) {
                collectForObjFullArgDeclarations(element, forObj, result);
            }
        }
        collectLetFullArgDeclarations(element, result);
        return result;
    }

    /**
     * Returns list-comprehension {@code for (name = expr)} bindings visible at the given element.
     */
    public static List<OpenSCADForBinding> getAccessibleForElementBindings(@NotNull final PsiElement element) {
        final List<OpenSCADForBinding> result = new ArrayList<>();
        collectForElementBindingsFromContainers(element, result);
        return result;
    }

    private static void collectForElementBindingsFromContainers(@NotNull final PsiElement element,
        @NotNull final List<OpenSCADForBinding> result) {
        PsiElement container = element.getParent();
        while (container != null && !(container instanceof PsiFileBase)) {
            collectForElementBindingsInContainer(container, element, result);
            container = container.getParent();
        }
    }

    private static void collectForElementBindingsInContainer(@NotNull final PsiElement container,
        @NotNull final PsiElement element,
        @NotNull final List<OpenSCADForBinding> result) {
        final List<OpenSCADForElement> activeForElements = new ArrayList<>();
        for (PsiElement sibling = container.getFirstChild(); sibling != null; sibling = sibling.getNextSibling()) {
            if (sibling instanceof OpenSCADForElement forElement) {
                activeForElements.add(forElement);
                if (element.equals(sibling) || PsiTreeUtil.isAncestor(sibling, element, false)) {
                    for (final OpenSCADForElement activeForElement : activeForElements) {
                        collectForElementBindingsInScope(element, activeForElement, result);
                    }
                    return;
                }
                continue;
            }
            if (element.equals(sibling) || PsiTreeUtil.isAncestor(sibling, element, false)) {
                for (final OpenSCADForElement activeForElement : activeForElements) {
                    addAllForElementBindings(activeForElement, result);
                }
                return;
            }
        }
    }

    private static void collectForElementBindingsInScope(@NotNull final PsiElement element,
        @NotNull final OpenSCADForElement forElement,
        @NotNull final List<OpenSCADForBinding> result) {
        final OpenSCADForDeclarationList declarationList = forElement.getForDeclarationList();
        if (declarationList == null) {
            return;
        }
        final OpenSCADForDeclaration declaration = declarationList.getForDeclaration();
        if (declaration == null) {
            return;
        }
        if (isInsideForDeclaration(element, declaration)) {
            addPrecedingForBindings(declaration, element, result);
        } else {
            addAllForElementBindings(forElement, result);
        }
    }

    private static void addAllForElementBindings(@NotNull final OpenSCADForElement forElement,
        @NotNull final List<OpenSCADForBinding> result) {
        final OpenSCADForDeclaration declaration = forElement.getForDeclarationList().getForDeclaration();
        if (declaration != null) {
            result.addAll(extractForDeclarationBindings(declaration));
        }
    }

    /**
     * Returns all loop and let bindings visible at the given element.
     */
    public static List<OpenSCADNamedElement> getAccessibleLoopBindings(@NotNull final PsiElement element) {
        final List<OpenSCADNamedElement> result = new ArrayList<>(getAccessibleFullArgDeclarations(element));
        result.addAll(getAccessibleForElementBindings(element));
        return result;
    }

    private static boolean isInsideForDeclaration(@NotNull final PsiElement element,
        @NotNull final OpenSCADForDeclaration declaration) {
        return PsiTreeUtil.isAncestor(declaration, element, false) && !element.equals(declaration);
    }

    private static void addPrecedingForBindings(@NotNull final OpenSCADForDeclaration declaration,
        @NotNull final PsiElement element,
        @NotNull final List<OpenSCADForBinding> result) {
        for (final OpenSCADForBinding binding : extractForDeclarationBindings(declaration)) {
            if (element.equals(binding.getNameIdentifier()) || isInForBindingExpression(element, binding)) {
                break;
            }
            result.add(binding);
        }
    }

    private static boolean isInForBindingExpression(@NotNull final PsiElement element,
        @NotNull final OpenSCADForBinding binding) {
        final PsiElement expression = getForBindingExpression(binding);
        return expression != null && PsiTreeUtil.isAncestor(expression, element, false);
    }

    @Nullable
    private static PsiElement getForBindingExpression(@NotNull final OpenSCADForBinding binding) {
        PsiElement cursor = PsiTreeUtil.skipWhitespacesAndCommentsForward(binding.getNameIdentifier().getNextSibling());
        if (cursor == null || cursor.getNode().getElementType() != OpenSCADTypes.EQUALS) {
            return null;
        }
        return PsiTreeUtil.skipWhitespacesAndCommentsForward(cursor.getNextSibling());
    }

    private static List<OpenSCADForBinding> extractForDeclarationBindings(@NotNull final OpenSCADForDeclaration declaration) {
        final OpenSCADForDeclarationCstyle cstyle = declaration.getForDeclarationCstyle();
        if (cstyle != null) {
            return extractCstyleLoopBindings(cstyle);
        }
        return extractTopLevelEqualsBindings(declaration);
    }

    private static List<OpenSCADForBinding> extractTopLevelEqualsBindings(@NotNull final PsiElement container) {
        final List<OpenSCADForBinding> result = new ArrayList<>();
        for (PsiElement child = container.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (OpenSCADForBinding.isBindingIdentifier(child)) {
                result.add(new OpenSCADForBinding(child, container));
            }
        }
        return result;
    }

    private static List<OpenSCADForBinding> extractCstyleLoopBindings(@NotNull final OpenSCADForDeclarationCstyle cstyle) {
        final List<OpenSCADForBinding> result = new ArrayList<>();
        int semicolonsSeen = 0;
        for (PsiElement child = cstyle.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode().getElementType() == OpenSCADTypes.SEMICOLON) {
                semicolonsSeen++;
                continue;
            }
            if (semicolonsSeen == 0 || semicolonsSeen == 2) {
                if (OpenSCADForBinding.isBindingIdentifier(child)) {
                    result.add(new OpenSCADForBinding(child, cstyle));
                }
            }
        }
        return result;
    }

    private static void collectForObjFullArgDeclarations(@NotNull final PsiElement element,
        @NotNull final OpenSCADForObj forObj,
        @NotNull final List<OpenSCADFullArgDeclaration> result) {
        final OpenSCADFullArgDeclarationList argList = forObj.getFullArgDeclarationList();
        if (argList == null) {
            return;
        }
        if (isInsideFullArgDeclarationList(element, argList)) {
            addPrecedingFullArgDeclarations(argList, element, result);
        } else if (isInForObjBody(element, forObj)) {
            addAllFullArgDeclarations(argList, result);
        }
    }

    private static void collectLetFullArgDeclarations(@NotNull final PsiElement element,
        @NotNull final List<OpenSCADFullArgDeclaration> result) {
        OpenSCADLetElement letElement = PsiTreeUtil.getParentOfType(element, OpenSCADLetElement.class);
        while (letElement != null) {
            collectLetScopeFullArgDeclarations(element, letElement.getFullArgDeclarationList(), result);
            letElement = PsiTreeUtil.getParentOfType(letElement.getParent(), OpenSCADLetElement.class);
        }

        OpenSCADBuiltinExpr builtinExpr = PsiTreeUtil.getParentOfType(element, OpenSCADBuiltinExpr.class);
        while (builtinExpr != null) {
            final PsiElement firstChild = builtinExpr.getFirstChild();
            if (firstChild != null && firstChild.getNode().getElementType() == OpenSCADTypes.LET_KEYWORD) {
                final OpenSCADFullArgDeclarationList argList = builtinExpr.getFullArgDeclarationList();
                if (argList != null) {
                    collectLetScopeFullArgDeclarations(element, argList, result);
                }
            }
            builtinExpr = PsiTreeUtil.getParentOfType(builtinExpr.getParent(), OpenSCADBuiltinExpr.class);
        }
    }

    private static void collectLetScopeFullArgDeclarations(@NotNull final PsiElement element,
        @NotNull final OpenSCADFullArgDeclarationList argList,
        @NotNull final List<OpenSCADFullArgDeclaration> result) {
        if (isInsideFullArgDeclarationList(element, argList)) {
            addPrecedingFullArgDeclarations(argList, element, result);
            return;
        }
        final PsiElement body = PsiTreeUtil.skipWhitespacesAndCommentsForward(argList);
        if (body != null && PsiTreeUtil.isAncestor(body, element, false)) {
            addAllFullArgDeclarations(argList, result);
        }
    }

    private static boolean isInForObjBody(@NotNull final PsiElement element, @NotNull final OpenSCADForObj forObj) {
        final OpenSCADFullArgDeclarationList argList = forObj.getFullArgDeclarationList();
        if (argList == null) {
            return false;
        }
        final PsiElement body = PsiTreeUtil.skipWhitespacesAndCommentsForward(argList);
        return body != null && PsiTreeUtil.isAncestor(body, element, false);
    }

    private static boolean isInsideFullArgDeclarationList(@NotNull final PsiElement element,
        @NotNull final OpenSCADFullArgDeclarationList argList) {
        return PsiTreeUtil.isAncestor(argList, element, false) && !element.equals(argList);
    }

    private static void addPrecedingFullArgDeclarations(@NotNull final OpenSCADFullArgDeclarationList argList,
        @NotNull final PsiElement element,
        @NotNull final List<OpenSCADFullArgDeclaration> result) {
        for (final OpenSCADFullArgDeclaration declaration : PsiTreeUtil.getChildrenOfTypeAsList(argList, OpenSCADFullArgDeclaration.class)) {
            if (declaration.equals(element) || PsiTreeUtil.isAncestor(declaration, element, false)) {
                break;
            }
            result.add(declaration);
        }
    }

    private static void addAllFullArgDeclarations(@NotNull final OpenSCADFullArgDeclarationList argList,
        @NotNull final List<OpenSCADFullArgDeclaration> result) {
        result.addAll(PsiTreeUtil.getChildrenOfTypeAsList(argList, OpenSCADFullArgDeclaration.class));
    }

    /**
     * Returns parameter declarations for the callee associated with a call-site argument list.
     */
    public static List<OpenSCADArgDeclaration> getCalleeArgumentDeclarations(@NotNull final OpenSCADParameterReference parameterReference) {
        final OpenSCADArgAssignmentList argList = PsiTreeUtil.getParentOfType(parameterReference, OpenSCADArgAssignmentList.class);
        if (argList == null) {
            return List.of();
        }

        return getCalleeArgumentDeclarations(argList);
    }

    public static List<OpenSCADArgDeclaration> getCalleeArgumentDeclarations(@NotNull final OpenSCADArgAssignmentList argList) {
        final OpenSCADResolvableElement callTarget = getCallTarget(argList);
        if (callTarget == null) {
            return List.of();
        }

        final PsiReference reference = callTarget.getReference();
        if (reference == null) {
            return List.of();
        }

        final PsiElement resolved = reference.resolve();
        if (resolved instanceof OpenSCADModuleDeclaration moduleDeclaration) {
            return getDeclarationArgumentList(moduleDeclaration.getArgDeclarationList());
        }

        if (resolved instanceof OpenSCADFunctionDeclaration functionDeclaration) {
            return getDeclarationArgumentList(functionDeclaration.getArgDeclarationList());
        }

        return List.of();
    }

    @Nullable
    public static OpenSCADResolvableElement getCallTarget(@NotNull final OpenSCADArgAssignmentList argList) {
        final PsiElement parent = argList.getParent();
        if (parent instanceof OpenSCADModuleCallObj moduleCallObj) {
            return moduleCallObj.getModuleObjNameRef();
        }

        if (parent instanceof OpenSCADModuleCallOp moduleCallOp) {
            return moduleCallOp.getModuleOpNameRef();
        }

        if (parent instanceof OpenSCADFunctionCallExpr functionCallExpr) {
            return functionCallExpr.getFunctionNameRef();
        }

        if (parent instanceof OpenSCADBuiltinObj builtinObj) {
            final OpenSCADBuiltinObjRef builtinObjRef = builtinObj.getBuiltinObjRef();
            if (builtinObjRef instanceof OpenSCADResolvableElement resolvable) {
                return resolvable;
            }
            final OpenSCADBuiltinOverridableObjRef overridableObjRef = builtinObj.getBuiltinOverridableObjRef();
            return overridableObjRef instanceof OpenSCADResolvableElement resolvable ? resolvable : null;
        }

        if (parent instanceof OpenSCADBuiltinOp builtinOp) {
            final OpenSCADCommonOpRef commonOpRef = builtinOp.getCommonOpRef();
            if (commonOpRef instanceof OpenSCADResolvableElement resolvable) {
                return resolvable;
            }
            final OpenSCADBuiltinOverridableOpRef overridableOpRef = builtinOp.getBuiltinOverridableOpRef();
            if (overridableOpRef instanceof OpenSCADResolvableElement resolvable) {
                return resolvable;
            }
            final OpenSCADBuiltinOverridableOpAsFunctionRef asFunctionRef = builtinOp.getBuiltinOverridableOpAsFunctionRef();
            return asFunctionRef instanceof OpenSCADResolvableElement resolvable ? resolvable : null;
        }

        if (parent instanceof OpenSCADBuiltinExpr builtinExpr) {
            final OpenSCADBuiltinExprRef builtinExprRef = builtinExpr.getBuiltinExprRef();
            if (builtinExprRef instanceof OpenSCADResolvableElement resolvable) {
                return resolvable;
            }
            final OpenSCADBuiltinOverridableExprRef overridableExprRef = builtinExpr.getBuiltinOverridableExprRef();
            if (overridableExprRef == null) {
                return null;
            }
            final OpenSCADBuiltinOverridableOpAsFunctionRef asFunctionRef = overridableExprRef.getBuiltinOverridableOpAsFunctionRef();
            if (asFunctionRef instanceof OpenSCADResolvableElement resolvable) {
                return resolvable;
            }
            final OpenSCADBuiltinOverridableOpRef overridableOpRef = overridableExprRef.getBuiltinOverridableOpRef();
            return overridableOpRef instanceof OpenSCADResolvableElement resolvable ? resolvable : null;
        }

        return null;
    }

    private static List<OpenSCADArgDeclaration> getDeclarationArgumentList(@Nullable final OpenSCADArgDeclarationList argList) {
        if (argList == null) {
            return List.of();
        }
        return PsiTreeUtil.getChildrenOfTypeAsList(argList, OpenSCADArgDeclaration.class);
    }

    public static List<OpenSCADModuleDeclaration> getFileModuleDeclarations(@NotNull final PsiFile file) {
        return PsiTreeUtil.getChildrenOfTypeAsList(file, OpenSCADModuleDeclaration.class);
    }

    public static List<OpenSCADFunctionDeclaration> getFileFunctionDeclarations(@NotNull final PsiFile file) {
        return PsiTreeUtil.getChildrenOfTypeAsList(file, OpenSCADFunctionDeclaration.class);
    }

    public static List<OpenSCADModuleDeclaration> getAccessibleModuleDeclarations(@NotNull final PsiElement element) {
        final Set<OpenSCADModuleDeclaration> result = new LinkedHashSet<>();
        addFileLevelModuleDeclarations(element, result);
        OpenSCADModuleDeclaration enclosingModule = PsiTreeUtil.getParentOfType(element, OpenSCADModuleDeclaration.class);
        while (enclosingModule != null) {
            final PsiElement body = getModuleBodyContainer(enclosingModule);
            if (body != null) {
                collectModuleDeclarationsInContainer(body, result);
            }
            enclosingModule = PsiTreeUtil.getParentOfType(enclosingModule.getParent(), OpenSCADModuleDeclaration.class);
        }

        return new ArrayList<>(result);
    }

    public static List<OpenSCADFunctionDeclaration> getAccessibleFunctionDeclarations(@NotNull final PsiElement element) {
        final Set<OpenSCADFunctionDeclaration> result = new LinkedHashSet<>();
        addFileLevelFunctionDeclarations(element, result);
        OpenSCADModuleDeclaration enclosingModule = PsiTreeUtil.getParentOfType(element, OpenSCADModuleDeclaration.class);
        while (enclosingModule != null) {
            final PsiElement body = getModuleBodyContainer(enclosingModule);
            if (body != null) {
                collectFunctionDeclarationsInContainer(body, result);
            }
            enclosingModule = PsiTreeUtil.getParentOfType(enclosingModule.getParent(), OpenSCADModuleDeclaration.class);
        }

        return new ArrayList<>(result);
    }

    private static @Nullable PsiElement getModuleBodyContainer(@NotNull final OpenSCADModuleDeclaration module) {
        final OpenSCADArgDeclarationList args = module.getArgDeclarationList();
        if (args == null) {
            return null;
        }
        PsiElement statement = PsiTreeUtil.skipWhitespacesAndCommentsForward(args);
        return statement;
    }

    private static void addFileLevelModuleDeclarations(@NotNull final PsiElement element,
        @NotNull final Set<OpenSCADModuleDeclaration> result) {
        final PsiFile file = element.getContainingFile();
        if (file == null) {
            return;
        }

        for (final PsiElement child : file.getChildren()) {
            if (child instanceof OpenSCADModuleDeclaration moduleDeclaration && !PsiTreeUtil.isAncestor(moduleDeclaration, element, false)) {
                result.add(moduleDeclaration);
            }
        }
    }

    private static void addFileLevelFunctionDeclarations(@NotNull final PsiElement element,
        @NotNull final Set<OpenSCADFunctionDeclaration> result) {
        final PsiFile file = element.getContainingFile();
        if (file == null) {
            return;
        }

        for (final PsiElement child : file.getChildren()) {
            if (child instanceof OpenSCADFunctionDeclaration functionDeclaration && !PsiTreeUtil.isAncestor(functionDeclaration, element, false)) {
                result.add(functionDeclaration);
            }
        }
    }

    private static void collectModuleDeclarationsInContainer(@NotNull final PsiElement container,
        @NotNull final Set<OpenSCADModuleDeclaration> result) {
        for (PsiElement child = container.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof OpenSCADModuleDeclaration moduleDeclaration) {
                result.add(moduleDeclaration);
            } else if (!isModuleOrFunctionDeclaration(child)) {
                collectModuleDeclarationsInContainer(child, result);
            }
        }
    }

    private static void collectFunctionDeclarationsInContainer(@NotNull final PsiElement container,
        @NotNull final Set<OpenSCADFunctionDeclaration> result) {
        for (PsiElement child = container.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof OpenSCADFunctionDeclaration functionDeclaration) {
                result.add(functionDeclaration);
            } else if (!isModuleOrFunctionDeclaration(child)) {
                collectFunctionDeclarationsInContainer(child, result);
            }
        }
    }

    private static boolean isModuleOrFunctionDeclaration(@NotNull final PsiElement element) {
        final IElementType type = element.getNode().getElementType();
        return type == OpenSCADModuleStubElementType.INSTANCE || type == OpenSCADFunctionStubElementType.INSTANCE;
    }

    private static void collectVariableDeclarations(@NotNull PsiElement element,
        @NotNull List<OpenSCADVariableDeclaration> result) {
        if (element.getNode().getElementType() == OpenSCADVariableStubElementType.INSTANCE) {
            result.add((OpenSCADVariableDeclaration) element);
        } else {
            final OpenSCADVariableDeclaration declaration = PsiTreeUtil.getChildOfType(element, OpenSCADVariableDeclaration.class);
            if (declaration != null) {
                result.add(declaration);
            }
        }
    }

    /**
     * Recursively get all parents of types elementTypes.
     *
     * @param element Element for which matching parents will be returned.
     * @param elementTypes Allowed parent types.
     * @return List of matching parents.
     */
    public static List<PsiElement> getParentsOfType(@Nullable PsiElement element, @NotNull TokenSet elementTypes) {
        List<PsiElement> matchingParents = new ArrayList<>();
        if (element != null && !(element instanceof PsiFileBase)) {
            element = element.getParent();
        }

        while (element != null) {
            final ASTNode node = element.getNode();
            if (node != null && elementTypes.contains(node.getElementType())) {
                matchingParents.add(element);
            }
            element = element.getParent();
        }

        return matchingParents;
    }
}
