package com.javampire.openscad.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.javampire.openscad.parser.OpenSCADParserTokenSets;
import com.javampire.openscad.psi.*;
import org.jetbrains.annotations.NotNull;

public class OpenSCADUnresolvedReferenceInspection extends LocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new OpenSCADVisitor() {
            @Override
            public void visitResolvableElement(@NotNull OpenSCADResolvableElement element) {
                checkUnresolvedReference(element, holder);
            }

            @Override
            public void visitVariableRefExpr(@NotNull OpenSCADVariableRefExpr element) {
                checkUnresolvedReference(element, holder);
            }
        };
    }

    private static void checkUnresolvedReference(@NotNull OpenSCADResolvableElement element, @NotNull ProblemsHolder holder) {
        if (OpenSCADParserTokenSets.NON_RENAMABLE_ELEMENTS.contains(element.getNode().getElementType())) {
            return;
        }

        if (element.getReferenceResolver() == null) {
            return;
        }
        final PsiReference reference = element.getReference();
        if (reference == null || reference.resolve() != null) {
            return;
        }
        final String name = element.getName();
        if (name == null) {
            return;
        }
        final PsiElement highlightTarget = element.getNameIdentifier() != null ? element.getNameIdentifier() : element;
        holder.registerProblem(
                highlightTarget,
                "Cannot resolve " + referenceTypeLabel(element) + " '" + name + "'",
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
        );
    }

    @NotNull
    private static String referenceTypeLabel(@NotNull OpenSCADResolvableElement element) {
        if (element instanceof OpenSCADModuleObjNameRef || element instanceof OpenSCADModuleOpNameRef) {
            return "module";
        }
        if (element instanceof OpenSCADFunctionNameRef) {
            return "function";
        }
        if (element instanceof OpenSCADVariableRefExpr) {
            return "variable";
        }
        return "reference";
    }
}
