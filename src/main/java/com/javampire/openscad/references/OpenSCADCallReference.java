package com.javampire.openscad.references;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import com.javampire.openscad.psi.OpenSCADFunctionDeclaration;
import com.javampire.openscad.psi.OpenSCADModuleDeclaration;
import com.javampire.openscad.psi.OpenSCADNamedElement;
import com.javampire.openscad.psi.OpenSCADResolvableElement;
import com.javampire.openscad.psi.OpenSCADVariableDeclaration;
import com.javampire.openscad.psi.OpenSCADVariableRefExpr;
import com.javampire.openscad.psi.OpenSCADModuleObjNameRef;
import com.javampire.openscad.psi.OpenSCADModuleOpNameRef;
import com.javampire.openscad.psi.OpenSCADFunctionNameRef;
import com.javampire.openscad.psi.OpenSCADPsiImplUtil;
import com.javampire.openscad.psi.stub.function.OpenSCADFunctionIndex;
import com.javampire.openscad.psi.stub.module.OpenSCADModuleIndex;
import com.javampire.openscad.references.OpenSCADReferenceResolver;
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
        if (myElement instanceof OpenSCADModuleObjNameRef || myElement instanceof OpenSCADModuleOpNameRef) {
            return resolveScopedModuleReferences();
        }
        if (myElement instanceof OpenSCADFunctionNameRef) {
            return resolveScopedFunctionReferences();
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

    @NotNull
    private ResolveResult[] resolveScopedModuleReferences() {
        final List<OpenSCADModuleDeclaration> accessibleDeclarations =
                OpenSCADPsiImplUtil.getAccessibleModuleDeclarations(myElement);
        final ResolveResult[] scopedResults = toScopedResults(accessibleDeclarations);
        if (scopedResults.length > 0) {
            return scopedResults;
        }
        return resolveIndexedReferences(OpenSCADModuleIndex.getInstance());
    }

    @NotNull
    private ResolveResult[] resolveScopedFunctionReferences() {
        final List<OpenSCADFunctionDeclaration> accessibleDeclarations =
                OpenSCADPsiImplUtil.getAccessibleFunctionDeclarations(myElement);
        final ResolveResult[] scopedResults = toScopedResults(accessibleDeclarations);
        if (scopedResults.length > 0) {
            return scopedResults;
        }
        return resolveIndexedReferences(OpenSCADFunctionIndex.getInstance());
    }

    @NotNull
    private ResolveResult[] toScopedResults(@NotNull final List<? extends OpenSCADNamedElement> accessibleDeclarations) {
        final List<ResolveResult> scopedResults = new ArrayList<>();
        for (OpenSCADNamedElement declaration : accessibleDeclarations) {
            if (referencedName.equals(declaration.getName())) {
                scopedResults.add(new PsiElementResolveResult(declaration));
            }
        }
        if (scopedResults.isEmpty()) {
            return ResolveResult.EMPTY_ARRAY;
        }
        if (scopedResults.size() == 1) {
            return scopedResults.toArray(ResolveResult.EMPTY_ARRAY);
        }
        return new ResolveResult[]{scopedResults.get(scopedResults.size() - 1)};
    }

    @NotNull
    private ResolveResult[] resolveIndexedReferences(@NotNull OpenSCADReferenceResolver resolver) {
        Project project = myElement.getProject();
        final Collection<? extends OpenSCADNamedElement> elementResults = resolver.get(
                referencedName, project, GlobalSearchScope.allScope(project)
        );
        if (elementResults.isEmpty()) {
            return ResolveResult.EMPTY_ARRAY;
        }
        final PsiFile containingFile = myElement.getContainingFile();
        final List<ResolveResult> sameFileResults = new ArrayList<>();
        for (OpenSCADNamedElement calledElement : elementResults) {
            if (calledElement.getContainingFile().equals(containingFile)) {
                sameFileResults.add(new PsiElementResolveResult(calledElement));
            }
        }
        if (sameFileResults.size() == 1) {
            return sameFileResults.toArray(ResolveResult.EMPTY_ARRAY);
        }
        if (sameFileResults.size() > 1) {
            return new ResolveResult[]{sameFileResults.get(sameFileResults.size() - 1)};
        }
        if (elementResults.size() == 1) {
            return new ResolveResult[]{new PsiElementResolveResult(elementResults.iterator().next())};
        }
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
