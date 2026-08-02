package com.javampire.openscad.templates;

import com.intellij.codeInsight.template.LiveTemplateContextService;
import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class OpenSCADTemplateContextTypeTest extends BasePlatformTestCase {

    public void testContextAppliesInScadFile() {
        myFixture.configureByText("test.scad", "cube(1);<caret>");
        final TemplateContextType contextType = getOpenSCADContextType();
        final TemplateActionContext actionContext = TemplateActionContext.expanding(myFixture.getFile(), myFixture.getCaretOffset());

        assertTrue(contextType.isInContext(actionContext));
        assertEquals("OPENSCAD", contextType.getContextId());
    }

    public void testContextDoesNotApplyInOtherFiles() {
        myFixture.configureByText("test.java", "class Example {<caret>}");
        final TemplateContextType contextType = getOpenSCADContextType();
        final TemplateActionContext actionContext = TemplateActionContext.expanding(myFixture.getFile(), myFixture.getCaretOffset());

        assertFalse(contextType.isInContext(actionContext));
    }

    public void testApplicableContextTypesInScadFile() {
        myFixture.configureByText("test.scad", "cube(1);<caret>");
        final TemplateActionContext actionContext = TemplateActionContext.expanding(myFixture.getFile(), myFixture.getCaretOffset());

        assertTrue(TemplateManagerImpl.getApplicableContextTypes(actionContext)
                .stream()
                .anyMatch(OpenSCADTemplateContextType.class::isInstance));
    }

    private static TemplateContextType getOpenSCADContextType() {
        return LiveTemplateContextService.getInstance().getTemplateContextType(OpenSCADTemplateContextType.class);
    }
}
