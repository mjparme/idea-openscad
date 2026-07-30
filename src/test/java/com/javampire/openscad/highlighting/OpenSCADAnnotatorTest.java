package com.javampire.openscad.highlighting;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public class OpenSCADAnnotatorTest extends BasePlatformTestCase {

    public void testModuleAndVariableNamesUseDistinctSemanticColors() {
        myFixture.configureByText("test.scad", """
                myVar = 10;
                module myMod() {}
                myMod();
                function myFunc(x) = myVar + x;
                """);

        final List<HighlightInfo> highlights = myFixture.doHighlighting();

        assertHighlightKey("myVar", OpenSCADSyntaxHighlighter.VARIABLE_NAME, highlights, 2);
        assertHighlightKey("myMod", OpenSCADSyntaxHighlighter.MODULE_NAME, highlights, 2);
        assertHighlightKey("myFunc", OpenSCADSyntaxHighlighter.FUNCTION_NAME, highlights, 1);
    }

    private static void assertHighlightKey(String text,
                                           TextAttributesKey expectedKey,
                                           List<HighlightInfo> highlights,
                                           int expectedCount) {
        final long matches = highlights.stream()
                .filter(info -> text.equals(info.getText()))
                .filter(info -> expectedKey.equals(info.forcedTextAttributesKey))
                .count();
        assertEquals(
                "Unexpected highlight count for '" + text + "' with key " + expectedKey.getExternalName(),
                expectedCount,
                matches
        );
    }
}
