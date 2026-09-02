package com.javampire.openscad.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.javampire.openscad.OpenSCADFileType;
import com.javampire.openscad.psi.stub.function.OpenSCADFunctionStubElementType;
import com.javampire.openscad.psi.stub.module.OpenSCADModuleStubElementType;
import com.javampire.openscad.psi.stub.variable.OpenSCADVariableStubElementType;

public class OpenSCADElementFactory {

    public static PsiElement createImportPath(final Project project, final String path) {
        final OpenSCADFile file = createFile(project, "include <" + path + ">;");
        final OpenSCADIncludeImport include = PsiTreeUtil.findChildOfType(file, OpenSCADIncludeImport.class);
        if (include == null) {
            throw new IllegalStateException("Failed to create import path for: " + path);
        }
        final OpenSCADImportPathRef pathRef = include.getImportPathRef();
        if (pathRef == null) {
            throw new IllegalStateException("Failed to create import path for: " + path);
        }
        return pathRef;
    }

    public static PsiElement createIdentifier(final Project project, final String name) {
        final OpenSCADFile file = createFile(project, name + "=0;");
        final ASTNode variableDeclaration = file.getNode().getFirstChildNode();
        if (variableDeclaration == null) {
            throw new IllegalStateException("Failed to create identifier for: " + name);
        }
        final ASTNode identifier = variableDeclaration.findChildByType(OpenSCADTypes.IDENTIFIER);
        if (identifier == null) {
            throw new IllegalStateException("Failed to create identifier for: " + name);
        }
        return identifier.getPsi();
    }

    public static OpenSCADFile createFile(final Project project, final String text) {
        final String name = "dummy.scad";
        return (OpenSCADFile) PsiFileFactory.getInstance(project).createFileFromText(name, OpenSCADFileType.INSTANCE, text);
    }

    public static IElementType getElementType(final String debugName) {
        if ("MODULE_DECLARATION".equals(debugName)) {
            return OpenSCADModuleStubElementType.INSTANCE;
        } else if ("FUNCTION_DECLARATION".equals(debugName)) {
            return OpenSCADFunctionStubElementType.INSTANCE;
        } else if ("VARIABLE_DECLARATION".equals(debugName)) {
            return OpenSCADVariableStubElementType.INSTANCE;
        } else {
            return new OpenSCADElementType(debugName);
        }
    }
}
