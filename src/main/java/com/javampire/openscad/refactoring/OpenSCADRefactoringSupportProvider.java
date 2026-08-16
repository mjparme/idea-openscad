package com.javampire.openscad.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.javampire.openscad.psi.OpenSCADFunctionDeclaration;
import com.javampire.openscad.psi.OpenSCADModuleDeclaration;
import com.javampire.openscad.psi.OpenSCADVariableDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenSCADRefactoringSupportProvider extends RefactoringSupportProvider {

    @Override
    public boolean isMemberInplaceRenameAvailable(@NotNull PsiElement element, PsiElement context) {
        PsiElement renamable = resolveRenamable(element, context);
        if (renamable instanceof OpenSCADModuleDeclaration || renamable instanceof OpenSCADFunctionDeclaration) {
            return true;
        }
        if (renamable instanceof OpenSCADVariableDeclaration variable) {
            return OpenSCADRenameUtil.isFileScopeVariable(variable);
        }
        return false;
    }

    @Override
    public boolean isInplaceRenameAvailable(@NotNull PsiElement element, PsiElement context) {
        PsiElement renamable = resolveRenamable(element, context);
        if (renamable instanceof OpenSCADVariableDeclaration variable) {
            return !OpenSCADRenameUtil.isFileScopeVariable(variable);
        }
        return false;
    }

    @Nullable
    private static PsiElement resolveRenamable(@NotNull PsiElement element, @Nullable PsiElement context) {
        PsiElement renamable = OpenSCADRenameUtil.getRenamableElement(element);
        if (renamable == null && context != null) {
            renamable = OpenSCADRenameUtil.getRenamableElement(context);
        }
        if (!(renamable instanceof PsiNameIdentifierOwner owner) || !OpenSCADRenameUtil.isRenamable(owner)) {
            return null;
        }
        return renamable;
    }
}
