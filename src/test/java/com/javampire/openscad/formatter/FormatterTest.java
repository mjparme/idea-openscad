package com.javampire.openscad.formatter;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.containers.ContainerUtil;

import java.nio.file.Path;

public class FormatterTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/testData/openscad/formatter";
    }

    @Override
    protected void setUp() throws Exception {
        VfsRootAccess.allowRootAccess(getTestRootDisposable(), Path.of(getTestDataPath()).toAbsolutePath().toString());
        super.setUp();
    }

    public void testFormatterDefault() {
        myFixture.configureByFile("IndentObjectsElements.scad");
        ApplicationManager.getApplication().runWriteAction(() ->
                CommandProcessor.getInstance().runUndoTransparentAction(() ->
                        CodeStyleManager.getInstance(getProject()).reformatText(
                                myFixture.getFile(),
                                ContainerUtil.newArrayList(myFixture.getFile().getTextRange())
                        )
                )
        );
        myFixture.checkResultByFile("IndentObjectsElements_result_default.scad");
    }

    public void testFormatterNoIndentObjectsElements() {
        myFixture.configureByFile("IndentObjectsElements.scad");
        ApplicationManager.getApplication().runWriteAction(() ->
                CommandProcessor.getInstance().runUndoTransparentAction(() -> {
                    CodeStyle.getCustomSettings(myFixture.getFile(), OpenSCADCodeStyleSettings.class).INDENT_CASCADING_TRANSFORMATIONS = false;
                    CodeStyleManager.getInstance(myFixture.getProject()).reformatText(
                            myFixture.getFile(),
                            ContainerUtil.newArrayList(myFixture.getFile().getTextRange())
                    );
                })
        );
        myFixture.checkResultByFile("IndentObjectsElements_result_noIndentCascadingTransformations.scad");
    }
}
