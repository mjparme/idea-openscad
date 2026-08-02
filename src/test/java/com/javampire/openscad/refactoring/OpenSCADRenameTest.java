package com.javampire.openscad.refactoring;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import com.javampire.openscad.psi.OpenSCADFunctionDeclaration;
import com.javampire.openscad.psi.OpenSCADModuleDeclaration;
import com.javampire.openscad.psi.OpenSCADModuleObjNameRef;
import com.javampire.openscad.psi.OpenSCADVariableDeclaration;
import com.javampire.openscad.psi.OpenSCADVariableRefExpr;

import java.nio.file.Path;

public class OpenSCADRenameTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/testData/openscad/rename";
    }

    @Override
    protected void setUp() throws Exception {
        VfsRootAccess.allowRootAccess(getTestRootDisposable(), Path.of(getTestDataPath()).toAbsolutePath().toString());
        super.setUp();
    }

    public void testRenameModule() {
        myFixture.configureByFile("module_rename_before.scad");
        OpenSCADModuleDeclaration module = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADModuleDeclaration.class);
        assertNotNull(module);
        myFixture.renameElement(module, "bar");
        myFixture.checkResultByFile("module_rename_after.scad");
    }

    public void testRenameModuleFromCallBeforeDeclaration() {
        myFixture.configureByFile("module_call_before_declaration_before.scad");
        OpenSCADModuleObjNameRef moduleCall = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADModuleObjNameRef.class);
        assertNotNull(moduleCall);
        assertEquals("main", moduleCall.getName());
        OpenSCADRenameProcessor processor = new OpenSCADRenameProcessor();
        PsiElement toRename = processor.substituteElementToRename(moduleCall, null);
        assertInstanceOf(toRename, OpenSCADModuleDeclaration.class);
        assertEquals("main", ((OpenSCADModuleDeclaration) toRename).getName());
        myFixture.renameElement(toRename, "entry");
        myFixture.checkResultByFile("module_call_before_declaration_after.scad");
    }

    public void testRenameModuleFromNameIdentifier() {
        myFixture.configureByFile("module_call_before_declaration_before.scad");
        OpenSCADModuleDeclaration module = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADModuleDeclaration.class);
        assertNotNull(module);
        PsiElement nameIdentifier = module.getNameIdentifier();
        assertNotNull(nameIdentifier);
        OpenSCADRenameProcessor processor = new OpenSCADRenameProcessor();
        PsiElement toRename = processor.substituteElementToRename(nameIdentifier, null);
        assertInstanceOf(toRename, OpenSCADModuleDeclaration.class);
        myFixture.renameElement(toRename, "entry");
        myFixture.checkResultByFile("module_call_before_declaration_after.scad");
    }

    public void testResolveNamedElementAtModuleDeclarationName() {
        myFixture.configureByFile("module_call_before_declaration_before.scad");
        OpenSCADModuleDeclaration module = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADModuleDeclaration.class);
        assertNotNull(module);
        PsiElement nameIdentifier = module.getNameIdentifier();
        assertNotNull(nameIdentifier);
        PsiElement named = TargetElementUtil.getNamedElement(nameIdentifier);
        assertInstanceOf(named, OpenSCADModuleDeclaration.class);
        assertEquals("main", ((OpenSCADModuleDeclaration) named).getName());
    }

    public void testRenameModuleDeclarationViaShiftF6Handler() {
        myFixture.configureByFile("module_declaration_shift_f6_before.scad");
        CodeInsightTestUtil.doInlineRename(new MemberInplaceRenameHandler(), "entry", myFixture);
        myFixture.checkResultByFile("module_call_before_declaration_after.scad");
    }

    public void testRenameModuleCallViaShiftF6Handler() {
        myFixture.configureByFile("module_call_shift_f6_before.scad");
        CodeInsightTestUtil.doInlineRename(new MemberInplaceRenameHandler(), "entry", myFixture);
        myFixture.checkResultByFile("module_call_before_declaration_after.scad");
    }

    public void testRenameModuleFromCaretOnDeclarationName() {
        myFixture.configureByFile("module_call_before_declaration_before.scad");
        OpenSCADModuleDeclaration module = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADModuleDeclaration.class);
        assertNotNull(module);
        PsiElement nameIdentifier = module.getNameIdentifier();
        assertNotNull(nameIdentifier);
        int offset = nameIdentifier.getTextRange().getStartOffset();
        myFixture.getEditor().getCaretModel().moveToOffset(offset);

        DataContext dataContext = SimpleDataContext.builder()
                .add(CommonDataKeys.EDITOR, myFixture.getEditor())
                .add(CommonDataKeys.PSI_FILE, myFixture.getFile())
                .build();
        assertFalse(new OpenSCADRenameHandler().isAvailableOnDataContext(dataContext));
        assertTrue(new MemberInplaceRenameHandler().isAvailableOnDataContext(dataContext));

        CodeInsightTestUtil.doInlineRename(new MemberInplaceRenameHandler(), "entry", myFixture);
        myFixture.checkResultByFile("module_call_before_declaration_after.scad");
    }

    public void testRenameFunction() {
        myFixture.configureByFile("function_rename_before.scad");
        OpenSCADFunctionDeclaration function = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADFunctionDeclaration.class);
        assertNotNull(function);
        myFixture.renameElement(function, "bar");
        myFixture.checkResultByFile("function_rename_after.scad");
    }

    public void testRenameVariable() {
        myFixture.configureByFile("variable_rename_before.scad");
        OpenSCADVariableDeclaration variable = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADVariableDeclaration.class);
        assertNotNull(variable);
        myFixture.renameElement(variable, "y");
        myFixture.checkResultByFile("variable_rename_after.scad");
    }

    public void testRenameModuleScopedVariableFromDeclaration() {
        myFixture.configureByFile("module_variable_rename_before.scad");
        OpenSCADVariableDeclaration variable = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADVariableDeclaration.class);
        assertNotNull(variable);
        myFixture.renameElement(variable, "boxTemp");
        myFixture.checkResultByFile("module_variable_rename_after.scad");
    }

    public void testRenameModuleScopedVariableFromReference() {
        myFixture.configureByFile("module_variable_rename_before.scad");
        OpenSCADVariableRefExpr variableRef = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADVariableRefExpr.class);
        assertNotNull(variableRef);
        OpenSCADRenameProcessor processor = new OpenSCADRenameProcessor();
        PsiElement toRename = processor.substituteElementToRename(variableRef, null);
        assertInstanceOf(toRename, OpenSCADVariableDeclaration.class);
        myFixture.renameElement(toRename, "boxTemp");
        myFixture.checkResultByFile("module_variable_rename_after.scad");
    }

    public void testRenameFileScopeVariableUsedInModule() {
        myFixture.configureByFile("file_scope_variable_in_module_before.scad");
        OpenSCADVariableDeclaration outerVar = PsiTreeUtil.getChildOfType(myFixture.getFile(), OpenSCADVariableDeclaration.class);
        assertNotNull(outerVar);
        assertEquals("outerVar", outerVar.getName());
        myFixture.renameElement(outerVar, "globalVar");
        myFixture.checkResultByFile("file_scope_variable_in_module_after.scad");
    }

    public void testRenameFileScopeVariableFromReferenceInsideModule() {
        myFixture.configureByFile("file_scope_variable_in_module_before.scad");
        OpenSCADVariableRefExpr variableRef = PsiTreeUtil.findChildOfType(myFixture.getFile(), OpenSCADVariableRefExpr.class);
        assertNotNull(variableRef);
        assertEquals("outerVar", variableRef.getName());
        OpenSCADRenameProcessor processor = new OpenSCADRenameProcessor();
        PsiElement toRename = processor.substituteElementToRename(variableRef, null);
        assertInstanceOf(toRename, OpenSCADVariableDeclaration.class);
        assertEquals("outerVar", ((OpenSCADVariableDeclaration) toRename).getName());
        myFixture.renameElement(toRename, "globalVar");
        myFixture.checkResultByFile("file_scope_variable_in_module_after.scad");
    }

    public void testRenameFileScopeVariableDoesNotRenameShadowingDeclarationInModule() {
        myFixture.configureByFile("file_scope_variable_shadowed_in_module_before.scad");
        OpenSCADVariableDeclaration outerVar = PsiTreeUtil.getChildOfType(myFixture.getFile(), OpenSCADVariableDeclaration.class);
        assertNotNull(outerVar);
        assertEquals("outerVar", outerVar.getName());
        myFixture.renameElement(outerVar, "globalVar");
        myFixture.checkResultByFile("file_scope_variable_shadowed_in_module_after.scad");
    }
}
