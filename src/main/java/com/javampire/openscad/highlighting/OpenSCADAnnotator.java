package com.javampire.openscad.highlighting;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiReference;
import com.javampire.openscad.psi.*;
import org.jetbrains.annotations.NotNull;

public class OpenSCADAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof OpenSCADModuleDeclaration || element instanceof OpenSCADModuleObjNameRef || element instanceof OpenSCADModuleOpNameRef) {
            annotate(element, OpenSCADSyntaxHighlighter.MODULE_NAME, holder);
        } else if (element instanceof OpenSCADFunctionDeclaration || element instanceof OpenSCADFunctionNameRef) {
            annotate(element, OpenSCADSyntaxHighlighter.FUNCTION_NAME, holder);
        } else if (element instanceof OpenSCADArgDeclaration) {
            annotate(element, OpenSCADSyntaxHighlighter.PARAMETER_NAME, holder);
        } else if (element instanceof OpenSCADVariableRefExpr variableRef) {
            annotate(variableRef, resolvesToParameter(variableRef)
                    ? OpenSCADSyntaxHighlighter.PARAMETER_NAME
                    : OpenSCADSyntaxHighlighter.VARIABLE_NAME, holder);
        } else if (element instanceof OpenSCADParameterReference parameterRef) {
            annotate(parameterRef, OpenSCADSyntaxHighlighter.PARAMETER_NAME, holder);
        } else if (element instanceof OpenSCADVariableDeclaration) {
            annotate(element, OpenSCADSyntaxHighlighter.VARIABLE_NAME, holder);
        }
    }

    private static boolean resolvesToParameter(@NotNull OpenSCADVariableRefExpr variableRef) {
        final PsiReference reference = variableRef.getReference();
        return reference != null && reference.resolve() instanceof OpenSCADArgDeclaration;
    }

    private static void annotate(@NotNull PsiElement element, @NotNull TextAttributesKey attributesKey, @NotNull AnnotationHolder holder) {
        final PsiElement range = element instanceof PsiNameIdentifierOwner owner ? owner.getNameIdentifier() : element;
        if (range == null) {
            return;
        }

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(range)
                .textAttributes(attributesKey)
                .create();
    }
}
