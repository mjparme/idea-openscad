package com.javampire.openscad.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import com.javampire.openscad.psi.OpenSCADArgDeclaration;
import com.javampire.openscad.psi.OpenSCADParameterReference;
import com.javampire.openscad.psi.OpenSCADPsiImplUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OpenSCADParameterCallReference extends PsiReferenceBase<OpenSCADParameterReference> {

    private final String referencedName;

    public OpenSCADParameterCallReference(@NotNull OpenSCADParameterReference element) {
        super(element, new TextRange(0, element.getTextLength()));
        referencedName = element.getName();
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        if (referencedName == null) {
            return null;
        }
        for (OpenSCADArgDeclaration declaration : OpenSCADPsiImplUtil.getCalleeArgumentDeclarations(myElement)) {
            if (referencedName.equals(declaration.getName())) {
                return declaration;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return OpenSCADPsiImplUtil.getCalleeArgumentDeclarations(myElement).toArray();
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        if (!(element instanceof OpenSCADArgDeclaration argDeclaration)) {
            throw new IncorrectOperationException("Cannot bind to non-parameter element");
        }
        final String newName = argDeclaration.getName();
        if (newName == null) {
            throw new IncorrectOperationException("Target parameter has no name");
        }
        final PsiElement renamed = myElement.setName(newName);
        if (renamed == null) {
            throw new IncorrectOperationException("Failed to rename parameter reference");
        }
        return renamed;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return element.equals(resolve());
    }

    @Override
    public String toString() {
        return "OpenSCADParameterCallReference(" + referencedName + ", " + getRangeInElement() + ")";
    }
}
