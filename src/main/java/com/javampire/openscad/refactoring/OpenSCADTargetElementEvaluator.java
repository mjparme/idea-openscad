package com.javampire.openscad.refactoring;

import com.intellij.codeInsight.TargetElementEvaluatorEx2;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Maps raw {@code IDENTIFIER} tokens (e.g. the name in {@code module foo()}) to their declaring element
 * so rename, navigation, and other caret-based actions work on declaration names.
 */
public class OpenSCADTargetElementEvaluator extends TargetElementEvaluatorEx2 {

    @Override
    public @Nullable PsiElement getNamedElement(@NotNull PsiElement element) {
        return OpenSCADRenameUtil.getRenamableElement(element);
    }
}
