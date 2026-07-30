package com.javampire.openscad.refactoring;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class OpenSCADNamedElementManipulator extends AbstractElementManipulator<PsiNameIdentifierOwner> {

    @NotNull
    @Override
    public PsiNameIdentifierOwner handleContentChange(
            @NotNull PsiNameIdentifierOwner element,
            @NotNull TextRange range,
            String newContent
    ) throws IncorrectOperationException {
        if (!OpenSCADRenameUtil.isRenamable(element)) {
            throw new IncorrectOperationException("Element cannot be renamed");
        }
        String name = element.getName();
        if (name == null || !range.equals(getRangeInElement(element))) {
            throw new IncorrectOperationException("Unexpected rename range");
        }
        PsiElement renamed = element.setName(newContent);
        if (!(renamed instanceof PsiNameIdentifierOwner owner)) {
            throw new IncorrectOperationException("Rename failed");
        }
        return owner;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull PsiNameIdentifierOwner element) {
        PsiElement nameIdentifier = element.getNameIdentifier();
        if (nameIdentifier == null) {
            return TextRange.EMPTY_RANGE;
        }
        int start = nameIdentifier.getTextRange().getStartOffset() - element.getTextRange().getStartOffset();
        return TextRange.create(start, start + nameIdentifier.getTextLength());
    }
}
