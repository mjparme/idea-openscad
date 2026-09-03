package com.javampire.openscad.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Named binding for a list-comprehension {@code for (name = expr)} declaration.
 */
public class OpenSCADForBinding extends FakePsiElement implements OpenSCADNamedElement {

    private final PsiElement nameIdentifier;
    private final PsiElement context;

    public OpenSCADForBinding(@NotNull final PsiElement nameIdentifier, @NotNull final PsiElement context) {
        this.nameIdentifier = nameIdentifier;
        this.context = context;
    }

    @Override
    public @Nullable String getName() {
        return nameIdentifier.getText();
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        return nameIdentifier;
    }

    @Override
    public @NotNull PsiElement setName(@NotNull final String newName) throws IncorrectOperationException {
        final PsiElement replacement = OpenSCADElementFactory.createIdentifier(nameIdentifier.getProject(), newName);
        nameIdentifier.replace(replacement);
        return this;
    }

    @Override
    public @Nullable PsiElement getParent() {
        return context.getParent();
    }

    @Override
    public @NotNull TextRange getTextRange() {
        return nameIdentifier.getTextRange();
    }

    @Override
    public @NotNull PsiFile getContainingFile() {
        return nameIdentifier.getContainingFile();
    }

    @Override
    public boolean isValid() {
        return nameIdentifier.isValid();
    }

    @Override
    public boolean textMatches(@NotNull final CharSequence text) {
        return nameIdentifier.textMatches(text);
    }

    @Override
    public int getTextOffset() {
        return nameIdentifier.getTextOffset();
    }

    @NotNull
    public PsiElement getContext() {
        return context;
    }

    public static boolean isBindingIdentifier(@NotNull final PsiElement element) {
        if (element.getNode().getElementType() != OpenSCADTypes.IDENTIFIER) {
            return false;
        }
        final PsiElement next = skipToEquals(element.getNextSibling());
        return next != null && next.getNode().getElementType() == OpenSCADTypes.EQUALS;
    }

    @Nullable
    private static PsiElement skipToEquals(@Nullable PsiElement element) {
        while (element != null) {
            if (element.getNode().getElementType() == OpenSCADTypes.EQUALS) {
                return element;
            }
            if (element instanceof OpenSCADExpr) {
                return null;
            }
            element = element.getNextSibling();
        }
        return null;
    }
}
