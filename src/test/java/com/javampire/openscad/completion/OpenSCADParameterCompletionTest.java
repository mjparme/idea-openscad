package com.javampire.openscad.completion;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public class OpenSCADParameterCompletionTest extends BasePlatformTestCase {

    public void testCalleeParameterCompletionAtCallSite() {
        myFixture.configureByText("test.scad", """
                module funnel() {
                    mainShape(<caret>);
                    module mainShape(delta = 0) { cube(1); }
                }
                """);
        myFixture.completeBasic();
        final List<String> variants = myFixture.getLookupElementStrings();
        assertNotNull(variants);
        assertTrue("Expected delta in completion at call site but got: " + variants, variants.contains("delta"));
    }

    public void testEnclosingModuleParameterCompletionInsideBody() {
        myFixture.configureByText("test.scad", """
                module mainShape(delta = 0) {
                    cube(<caret>);
                }
                """);
        myFixture.completeBasic();
        final List<String> variants = myFixture.getLookupElementStrings();
        assertNotNull(variants);
        assertTrue("Expected delta in completion inside module body but got: " + variants, variants.contains("delta"));
    }
}
