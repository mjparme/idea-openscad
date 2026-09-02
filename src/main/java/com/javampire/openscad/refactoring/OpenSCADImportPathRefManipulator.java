package com.javampire.openscad.refactoring;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import com.javampire.openscad.psi.OpenSCADElementFactory;
import com.javampire.openscad.psi.OpenSCADImportPathRefElement;
import org.jetbrains.annotations.NotNull;

public class OpenSCADImportPathRefManipulator extends AbstractElementManipulator<OpenSCADImportPathRefElement> {

    @NotNull
    @Override
    public OpenSCADImportPathRefElement handleContentChange(
            @NotNull OpenSCADImportPathRefElement element,
            @NotNull TextRange range,
            String newContent
    ) throws IncorrectOperationException {
        if (newContent == null) {
            throw new IncorrectOperationException("newContent must not be null");
        }
        final String oldText = element.getText();
        final String updatedText = range.replace(oldText, newContent);
        if (updatedText.equals(oldText)) {
            return element;
        }
        final PsiElement replacement = OpenSCADElementFactory.createImportPath(element.getProject(), updatedText);
        final PsiElement replaced = element.replace(replacement);
        if (!(replaced instanceof OpenSCADImportPathRefElement updated)) {
            throw new IncorrectOperationException("Failed to replace import path");
        }
        return updated;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull OpenSCADImportPathRefElement element) {
        return TextRange.from(0, element.getTextLength());
    }
}
