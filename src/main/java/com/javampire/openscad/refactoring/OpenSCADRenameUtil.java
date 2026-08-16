package com.javampire.openscad.refactoring;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.javampire.openscad.parser.OpenSCADParserTokenSets;
import com.javampire.openscad.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class OpenSCADRenameUtil {

    private OpenSCADRenameUtil() {
    }

    public static boolean isRenamable(@Nullable PsiElement element) {
        if (!(element instanceof PsiNameIdentifierOwner owner) || owner.getNameIdentifier() == null) {
            return false;
        }

        if (OpenSCADParserTokenSets.NON_RENAMABLE_ELEMENTS.contains(element.getNode().getElementType())) {
            return false;
        }

        return element instanceof OpenSCADModuleDeclaration
                || element instanceof OpenSCADFunctionDeclaration
                || element instanceof OpenSCADVariableDeclaration
                || element instanceof OpenSCADArgDeclaration
                || element instanceof OpenSCADFullArgDeclaration
                || element instanceof OpenSCADResolvableElement
                || element instanceof OpenSCADParameterReference;
    }

    /**
     * Returns the element that should be renamed for refactorings invoked on {@code element},
     * e.g. the module declaration when the caret is on the identifier in {@code module foo()}.
     */
    @Nullable
    public static PsiElement getRenamableElement(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        if (isRenamable(element)) {
            if (element instanceof OpenSCADResolvableElement resolvable) {
                final PsiReference reference = resolvable.getReference();
                if (reference != null) {
                    final PsiElement resolved = reference.resolve();
                    if (resolved != null) {
                        return resolved;
                    }
                }
            }
            return element;
        }

        if (element.getNode().getElementType() == OpenSCADTypes.IDENTIFIER) {
            final PsiNameIdentifierOwner owner = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner.class, false);
            if (owner != null && isRenamable(owner)) {
                return owner;
            }
        }
        
        return null;
    }

    @NotNull
    public static String getTypeLabel(@NotNull PsiElement element) {
        if (element instanceof OpenSCADModuleDeclaration) {
            return "module";
        }
        if (element instanceof OpenSCADFunctionDeclaration) {
            return "function";
        }
        if (element instanceof OpenSCADVariableDeclaration) {
            return "variable";
        }
        if (element instanceof OpenSCADArgDeclaration || element instanceof OpenSCADFullArgDeclaration) {
            return "parameter";
        }
        if (element instanceof OpenSCADResolvableElement) {
            return "reference";
        }
        return "symbol";
    }

    /**
     * File-scope variables can be referenced from other files via {@code include} and need
     * member-style inplace rename so all references are updated across the project.
     */
    public static boolean isFileScopeVariable(@NotNull OpenSCADVariableDeclaration variable) {
        return PsiTreeUtil.getParentOfType(variable, OpenSCADModuleDeclaration.class, OpenSCADFunctionDeclaration.class) == null;
    }
}
