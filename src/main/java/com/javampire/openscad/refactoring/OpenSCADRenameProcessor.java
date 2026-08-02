package com.javampire.openscad.refactoring;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenSCADRenameProcessor extends RenamePsiElementProcessor {

    @Override
    public boolean canProcessElement(@NotNull PsiElement element) {
        return OpenSCADRenameUtil.getRenamableElement(element) != null;
    }

    @Nullable
    @Override
    public PsiElement substituteElementToRename(@NotNull PsiElement element, @Nullable Editor editor) {
        final PsiElement renamable = OpenSCADRenameUtil.getRenamableElement(element);
        return renamable != null ? renamable : element;
    }
}
