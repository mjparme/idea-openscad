package com.javampire.openscad.refactoring;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
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
}
