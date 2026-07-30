package com.javampire.openscad.references;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import com.javampire.openscad.psi.OpenSCADNamedElement;
import com.javampire.openscad.psi.OpenSCADResolvableElement;
import com.javampire.openscad.psi.OpenSCADVariableDeclaration;
import com.javampire.openscad.psi.OpenSCADVariableRefExpr;
import com.javampire.openscad.psi.OpenSCADPsiImplUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class OpenSCADCallReference extends PsiReferenceBase<OpenSCADResolvableElement> implements PsiPolyVariantReference {

    private static final Logger LOG = Logger.getInstance(OpenSCADCallReference.class);

    private final String referencedName;

    public OpenSCADCallReference(@NotNull OpenSCADResolvableElement element, TextRange textRange) {
        super(element, textRange);
        referencedName = element.getName();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        if (myElement instanceof OpenSCADVariableRefExpr) {
            return resolveVariableReferences();
        }
        Project project = myElement.getProject();
        final OpenSCADReferenceResolver resolver = myElement.getReferenceResolver();
        if (resolver == null) {
            return ResolveResult.EMPTY_ARRAY;
        }
        final Collection<? extends OpenSCADNamedElement> elementResults = resolver.get(
                this.referencedName, project, GlobalSearchScope.allScope(project)
        );
        LOG.debug("multiResolve elementResults:" + elementResults);
        final List<ResolveResult> results = new ArrayList<>();
        for (OpenSCADNamedElement calledElement : elementResults) {
            results.add(new PsiElementResolveResult(calledElement));
        }
        LOG.debug("multiResolve results:" + results);
        return results.toArray(new ResolveResult[0]);
    }

    @NotNull
    private ResolveResult[] resolveVariableReferences() {
        final List<OpenSCADVariableDeclaration> accessibleDeclarations =
                OpenSCADPsiImplUtil.getAccessibleVariableDeclaration(myElement);
        final List<ResolveResult> scopedResults = new ArrayList<>();
        for (OpenSCADVariableDeclaration declaration : accessibleDeclarations) {
            if (referencedName.equals(declaration.getName())) {
                scopedResults.add(new PsiElementResolveResult(declaration));
            }
        }
        if (!scopedResults.isEmpty()) {
            if (scopedResults.size() == 1) {
                return scopedResults.toArray(ResolveResult.EMPTY_ARRAY);
            }
            return new ResolveResult[]{scopedResults.get(scopedResults.size() - 1)};
        }
        Project project = myElement.getProject();
        final OpenSCADReferenceResolver resolver = myElement.getReferenceResolver();
        if (resolver == null) {
            return ResolveResult.EMPTY_ARRAY;
        }
        final Collection<? extends OpenSCADNamedElement> elementResults = resolver.get(
                referencedName, project, GlobalSearchScope.allScope(project)
        );
        final List<ResolveResult> results = new ArrayList<>();
        for (OpenSCADNamedElement calledElement : elementResults) {
            results.add(new PsiElementResolveResult(calledElement));
        }
        return results.toArray(ResolveResult.EMPTY_ARRAY);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        LOG.debug("resolve called");
        final ResolveResult[] resolveResults = multiResolve(false);
        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        if (!(element instanceof OpenSCADNamedElement namedElement)) {
            return false;
        }
        final PsiElement resolved = resolve();
        if (resolved != null) {
            return resolved.equals(element);
        }
        return Objects.equals(referencedName, namedElement.getName())
                && myElement.getContainingFile().equals(element.getContainingFile());
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        if (!(element instanceof OpenSCADNamedElement namedElement)) {
            throw new IncorrectOperationException("Cannot bind to non-OpenSCAD element");
        }
        final String newName = namedElement.getName();
        if (newName == null) {
            throw new IncorrectOperationException("Target element has no name");
        }
        final PsiElement renamed = myElement.setName(newName);
        if (renamed == null) {
            throw new IncorrectOperationException("Failed to rename reference");
        }
        return renamed;
    }

    @Override
    public String toString() {
        return "OpenSCADCallReference(" + this.referencedName + ", " + getRangeInElement() + ")";
    }
}
