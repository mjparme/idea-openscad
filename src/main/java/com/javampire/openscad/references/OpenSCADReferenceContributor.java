package com.javampire.openscad.references;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import com.javampire.openscad.psi.OpenSCADResolvableElement;
import org.jetbrains.annotations.NotNull;

/**
 * User: mjparme
 * Date: 7/23/22
 * Time: 2:18 PM
 */
public class OpenSCADReferenceContributor extends PsiReferenceContributor {
    private final static Logger LOG = Logger.getInstance("#com.javampire.openscad.references.OpenSCADReferenceContributor");
    
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar psiReferenceRegistrar) {
        PsiElementPattern.Capture<PsiLiteralExpression> pattern = PlatformPatterns.psiElement(PsiLiteralExpression.class);
        psiReferenceRegistrar.registerReferenceProvider(pattern, new OpenSCADReferenceProvider());
    }

    private class OpenSCADReferenceProvider extends PsiReferenceProvider {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
            PsiLiteralExpression literalExpression = (PsiLiteralExpression) psiElement;
            String value = literalExpression.getValue() instanceof String ? (String) literalExpression.getValue() : null;
            LOG.debug("getReferencesByElement(), value: {}", value);

            if (value != null) {
                TextRange property = new TextRange(0, value.length() + 1);
                LOG.debug("getReferencesByElement(), property: {}", property);
                return new PsiReference[] {new OpenSCADCallReference((OpenSCADResolvableElement) psiElement, property)};
            }

            return PsiReference.EMPTY_ARRAY;
        }
    }
}
