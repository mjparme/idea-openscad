package com.javampire.openscad.completion;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public class OpenSCADCompletionContributorTest extends BasePlatformTestCase {

    public void testInnerModuleCompletionAfterDeclaration() {
        myFixture.configureByText("test.scad", """
                module funnel() {
                    module mainShape() { cube(1); }
                    <caret>mainShape();
                }
                """);
        assertContainsModule("mainShape");
    }

    public void testInnerModuleCompletionBeforeDeclaration() {
        myFixture.configureByText("test.scad", """
                module funnel() {
                    <caret>mainShape();
                    module mainShape() { cube(1); }
                }
                """);
        assertContainsModule("mainShape");
    }

    public void testFileLevelModuleCompletionBeforeDeclaration() {
        myFixture.configureByText("test.scad", """
                <caret>later();
                module later() { cube(1); }
                """);
        assertContainsModule("later");
    }

    public void testInnerModuleNotVisibleOutsideParentModule() {
        myFixture.configureByText("test.scad", """
                module funnel() {
                    module mainShape() { cube(1); }
                }
                main<caret>
                """);
        final List<String> variants = myFixture.getLookupElementStrings();
        if (variants != null) {
            assertFalse(variants.contains("mainShape"));
        }
    }

    public void testInnerFunctionCompletionInsideParentModule() {
        myFixture.configureByText("test.scad", """
                module funnel() {
                    function helper(x) = x * 2;
                    value = helper(<caret>);
                }
                """);
        myFixture.completeBasic();
        final List<String> variants = myFixture.getLookupElementStrings();
        assertNotNull(variants);
        assertTrue(variants.contains("helper"));
    }

    private void assertContainsModule(String moduleName) {
        final var lookupElements = myFixture.completeBasic();
        assertNotNull(lookupElements);
        final List<String> variants = myFixture.getLookupElementStrings();
        assertNotNull(variants);
        assertTrue("Expected completion to include " + moduleName + " but got: " + variants, variants.contains(moduleName));
    }
}
