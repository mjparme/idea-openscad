package com.javampire.openscad.refactoring;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.javampire.openscad.psi.OpenSCADResolvableElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenSCADRenameProcessor extends RenamePsiElementProcessor {

    @Override
    public boolean canProcessElement(@NotNull PsiElement element) {
        return OpenSCADRenameUtil.isRenamable(element);
    }

    @Nullable
    @Override
    public PsiElement substituteElementToRename(@NotNull PsiElement element, @Nullable Editor editor) {
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
}
