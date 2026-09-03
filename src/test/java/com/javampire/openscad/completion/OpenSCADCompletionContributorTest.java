package com.javampire.openscad.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.javampire.openscad.psi.BuiltinSkeletons;
import com.javampire.openscad.psi.OpenSCADArgDeclaration;
import com.javampire.openscad.psi.OpenSCADModuleDeclaration;
import com.javampire.openscad.settings.OpenSCADSettings;

import java.util.List;
import java.util.Objects;

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

    public void testZeroArgModuleCompletionInsertsParentheses() {
        myFixture.configureByText("test.scad", """
                <caret>blow();
                module blowerHolder() { cube(1); }
                """);
        final LookupElement blowerHolder = findLookupItem("blowerHolder");
        selectLookupItem(blowerHolder);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                blowerHolder();
                module blowerHolder() { cube(1); }
                """);
    }

    public void testParameterizedModuleCompletionInsertsParenthesesWithCaretAfter() {
        myFixture.configureByText("test.scad", """
                <caret>main
                module mainShape(delta = 0) { cube(delta); }
                """);
        final LookupElement mainShape = findLookupItem("mainShape");
        selectLookupItem(mainShape);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                mainShape();
                module mainShape(delta = 0) { cube(delta); }
                """);
        final int closeParenOffset = myFixture.getFile().getText().indexOf(')');
        assertEquals(closeParenOffset + 2, myFixture.getEditor().getCaretModel().getOffset());
    }

    public void testModuleCompletionDoesNotInsertSemicolonForBuiltinOperators() {
        myFixture.configureByText("test.scad", "<caret>uni");
        final LookupElement union = findLookupItem("union");
        selectLookupItem(union);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("union()");

        myFixture.configureByText("test.scad", "<caret>trans");
        final LookupElement translate = findLookupItem("translate");
        selectLookupItem(translate);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("translate()");

        myFixture.configureByText("test.scad", "<caret>diff");
        final LookupElement difference = findLookupItem("difference");
        selectLookupItem(difference);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("difference()");
    }

    public void testModuleCompletionDoesNotInsertSemicolonBeforeFollowingModuleCall() {
        myFixture.configureByText("test.scad", """
                union() cu<caret> sphere();
                """);
        final LookupElement cube = findLookupItem("cube");
        selectLookupItem(cube);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                union() cube() sphere();
                """);
    }

    public void testModuleCompletionDoesNotInsertSemicolonInsideArgumentList() {
        myFixture.configureByText("test.scad", """
                union(cu<caret>, sphere());
                """);
        final LookupElement cube = findLookupItem("cube");
        selectLookupItem(cube);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                union(cube(), sphere());
                """);
    }

    public void testParameterizedModuleCompletionMovesCaretAfterExistingParentheses() {
        myFixture.configureByText("test.scad", """
                <caret>main();
                module mainShape(delta = 0) { cube(delta); }
                """);
        final LookupElement mainShape = findLookupItem("mainShape");
        selectLookupItem(mainShape);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                mainShape();
                module mainShape(delta = 0) { cube(delta); }
                """);
        final int openParenOffset = myFixture.getFile().getText().indexOf('(');
        assertEquals(openParenOffset + 2, myFixture.getEditor().getCaretModel().getOffset());
    }

    public void testParameterizedModuleShowsWithArgsCompletionItem() {
        myFixture.configureByText("test.scad", """
                <caret>main
                module mainShape(delta = 0, width = 10) { cube(delta); }
                """);
        assertNotNull(findLookupItemWithFilledArgs("mainShape"));
        assertEquals(2, countLookupItems("mainShape"));
    }

    public void testWithArgsCompletionItemFillsNamedArguments() {
        myFixture.configureByText("test.scad", """
                <caret>main
                module mainShape(delta = 0, width = 10) { cube(delta); }
                """);
        final LookupElement mainShapeWithArgs = findLookupItemWithFilledArgs("mainShape");
        selectLookupItem(mainShapeWithArgs);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                mainShape(delta = 0, width = 10);
                module mainShape(delta = 0, width = 10) { cube(delta); }
                """);
        final int closeParenOffset = myFixture.getFile().getText().indexOf(')');
        assertEquals(closeParenOffset + 2, myFixture.getEditor().getCaretModel().getOffset());
    }

    public void testWithArgsCompletionItemUsesEmptyPlaceholderForParametersWithoutDefaults() {
        myFixture.configureByText("test.scad", """
                <caret>main
                module mainShape(delta = 0, extra) { cube(delta); }
                """);
        final LookupElement mainShapeWithArgs = findLookupItemWithFilledArgs("mainShape");
        selectLookupItem(mainShapeWithArgs);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("""
                mainShape(delta = 0, extra = );
                module mainShape(delta = 0, extra) { cube(delta); }
                """);
        final int closeParenOffset = myFixture.getFile().getText().indexOf(')');
        assertEquals(closeParenOffset + 2, myFixture.getEditor().getCaretModel().getOffset());
    }

    public void testWithArgsCompletionItemHiddenWhenSettingEnabled() {
        final OpenSCADSettings settings = OpenSCADSettings.getInstance();
        final boolean previous = settings.isFillNamedArgumentsOnModuleCompletion();
        settings.setFillNamedArgumentsOnModuleCompletion(true);
        try {
            myFixture.configureByText("test.scad", """
                    <caret>main
                    module mainShape(delta = 0) { cube(delta); }
                    """);
            assertEquals(1, countLookupItems("mainShape"));
        }
        finally {
            settings.setFillNamedArgumentsOnModuleCompletion(previous);
        }
    }

    public void testParameterizedModuleSettingFillsNamedArguments() {
        final OpenSCADSettings settings = OpenSCADSettings.getInstance();
        final boolean previous = settings.isFillNamedArgumentsOnModuleCompletion();
        settings.setFillNamedArgumentsOnModuleCompletion(true);
        try {
            myFixture.configureByText("test.scad", """
                    <caret>main
                    module mainShape(delta = 0) { cube(delta); }
                    """);
            final LookupElement mainShape = findLookupItem("mainShape");
            selectLookupItem(mainShape);
            myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
            myFixture.checkResult("""
                    mainShape(delta = 0);
                    module mainShape(delta = 0) { cube(delta); }
                    """);
            final int closeParenOffset = myFixture.getFile().getText().indexOf(')');
            assertEquals(closeParenOffset + 2, myFixture.getEditor().getCaretModel().getOffset());
        }
        finally {
            settings.setFillNamedArgumentsOnModuleCompletion(previous);
        }
    }

    public void testBuiltinCubeWithArgsUsesPositionalFirstParameter() {
        myFixture.configureByText("test.scad", "<caret>");
        final LookupElement cubeWithArgs = findLookupItemWithFilledArgs("cube");
        assertNotNull(cubeWithArgs);
        selectLookupItem(cubeWithArgs);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("cube([1, 1, 1], center = false);");
        final int closeParenOffset = myFixture.getFile().getText().indexOf(')');
        assertEquals(closeParenOffset + 2, myFixture.getEditor().getCaretModel().getOffset());
    }

    public void testBuiltinCubePrimaryWithSettingUsesPositionalFirstParameter() {
        final OpenSCADSettings settings = OpenSCADSettings.getInstance();
        final boolean previous = settings.isFillNamedArgumentsOnModuleCompletion();
        settings.setFillNamedArgumentsOnModuleCompletion(true);
        try {
            myFixture.configureByText("test.scad", "<caret>");
            final LookupElement cube = findLookupItem("cube");
            selectLookupItem(cube);
            myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
            myFixture.checkResult("cube([1, 1, 1], center = false);");
            final int closeParenOffset = myFixture.getFile().getText().indexOf(')');
            assertEquals(closeParenOffset + 2, myFixture.getEditor().getCaretModel().getOffset());
        }
        finally {
            settings.setFillNamedArgumentsOnModuleCompletion(previous);
        }
    }

    public void testBuiltinLinearExtrudeWithArgsUsesNamedParameters() {
        myFixture.configureByText("test.scad", "<caret>");
        final LookupElement linearExtrudeWithArgs = findLookupItemWithFilledArgs("linear_extrude");
        assertNotNull(linearExtrudeWithArgs);
        selectLookupItem(linearExtrudeWithArgs);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("linear_extrude(height = fanwidth, center = true, convexity = 10, twist = -fanrot, slices = 20, scale = 1.0)");
    }

    public void testBuiltinRotateWithArgsUsesCurrentSkeletonDefaults() {
        final OpenSCADModuleDeclaration rotate = BuiltinSkeletons.findModuleDeclaration(getProject(), "rotate");
        assertNotNull(rotate);
        final OpenSCADArgDeclaration firstArg = rotate.getArgDeclarationList().getArgDeclarationList().get(0);
        final String firstDefault = firstArg.getExpr().getText();

        myFixture.configureByText("test.scad", "<caret>");
        final LookupElement rotateWithArgs = findLookupItemWithFilledArgs("rotate");
        assertNotNull(rotateWithArgs);
        selectLookupItem(rotateWithArgs);
        myFixture.finishLookup(Lookup.REPLACE_SELECT_CHAR);
        myFixture.checkResult("rotate(" + firstDefault + ")");
    }

    public void testUseImportProvidesModuleAndFunctionCompletionsWithSource() {
        myFixture.configureByText("use_lib.scad", """
                lib_var = 1;
                module libModule() { cube(1); }
                function libFunction(x) = x;
                """);
        myFixture.configureByText("use_main.scad", """
                use <use_lib.scad>
                lib<caret>
                """);
        assertContainsCompletion("libModule");
        assertContainsCompletion("libFunction");
        assertCompletionHasTailText("libModule", " from use_lib.scad");
        assertCompletionHasTailText("libFunction", " from use_lib.scad");
        assertDoesNotContainCompletion("lib_var");
    }

    public void testIncludeImportProvidesVariablesModulesAndFunctionsWithSource() {
        myFixture.configureByText("include_lib.scad", """
                lib_var = 1;
                module libModule() { cube(1); }
                function libFunction(x) = x;
                """);
        myFixture.configureByText("include_main.scad", """
                include <include_lib.scad>
                lib<caret>
                """);
        assertContainsCompletion("libModule");
        assertContainsCompletion("libFunction");
        assertContainsCompletion("lib_var");
        assertCompletionHasTailText("lib_var", " from include_lib.scad");
    }

    public void testNestedUseDoesNotPropagateToBaseFile() {
        myFixture.configureByText("nested_use_nested.scad", """
                module nestedModule() { sphere(1); }
                """);
        myFixture.configureByText("nested_use_middle.scad", """
                use <nested_use_nested.scad>
                """);
        myFixture.configureByText("nested_use_main.scad", """
                use <nested_use_middle.scad>
                nest<caret>
                """);
        assertDoesNotContainCompletion("nestedModule");
    }

    public void testTransitiveIncludeProvidesNestedVariables() {
        myFixture.configureByText("transitive_utils.scad", """
                utils_var = 42;
                """);
        myFixture.configureByText("transitive_lib.scad", """
                include <transitive_utils.scad>
                """);
        myFixture.configureByText("transitive_main.scad", """
                include <transitive_lib.scad>
                <caret>
                """);
        assertContainsCompletion("utils_var");
        assertCompletionHasTailText("utils_var", " from transitive_utils.scad");
    }

    public void testIncludeBringsInModulesFromUsedDependency() {
        myFixture.configureByText("include_use_nested.scad", """
                module nestedModule() { sphere(1); }
                """);
        myFixture.configureByText("include_use_lib.scad", """
                use <include_use_nested.scad>
                """);
        myFixture.configureByText("include_use_main.scad", """
                include <include_use_lib.scad>
                <caret>
                """);
        assertContainsCompletion("nestedModule");
        assertCompletionHasTailText("nestedModule", " from include_use_nested.scad");
    }

    public void testParentRelativeUseImportProvidesCompletions() {
        myFixture.addFileToProject("lib/cubes.scad", "module roundedCube() { cube(1); }");
        myFixture.addFileToProject("fiber-arts/thread-holder/main.scad", """
                use <../../lib/cubes.scad>
                
                """);
        myFixture.configureFromTempProjectFile("fiber-arts/thread-holder/main.scad");
        myFixture.getEditor().getCaretModel().moveToOffset(myFixture.getEditor().getDocument().getTextLength());
        assertContainsCompletion("roundedCube");
        assertCompletionHasTailText("roundedCube", " from ../../lib/cubes.scad");
    }

    private void selectLookupItem(LookupElement item) {
        final var lookup = myFixture.getLookup();
        assertNotNull(lookup);
        lookup.setCurrentItem(item);
    }

    private LookupElement findLookupItem(String name) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull("No completion items returned", items);
        LookupElement fallback = null;
        for (LookupElement item : items) {
            if (!name.equals(item.getLookupString())) {
                continue;
            }
            if (!OpenSCADCompletionContributor.lookupElementFillsNamedArguments(item)) {
                return item;
            }
            if (fallback == null) {
                fallback = item;
            }
        }
        if (fallback != null) {
            return fallback;
        }
        fail("Expected completion to include " + name + " but got: " + myFixture.getLookupElementStrings());
        return null;
    }

    private LookupElement findLookupItemWithFilledArgs(String name) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull("No completion items returned", items);
        for (LookupElement item : items) {
            if (name.equals(item.getLookupString())
                    && OpenSCADCompletionContributor.lookupElementFillsNamedArguments(item)) {
                return item;
            }
        }
        return null;
    }

    private int countLookupItems(String name) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull(items);
        int count = 0;
        for (LookupElement item : items) {
            if (name.equals(item.getLookupString())) {
                count++;
            }
        }
        return count;
    }

    private void assertContainsModule(String moduleName) {
        final var lookupElements = myFixture.completeBasic();
        assertNotNull(lookupElements);
        final List<String> variants = myFixture.getLookupElementStrings();
        assertNotNull(variants);
        assertTrue("Expected completion to include " + moduleName + " but got: " + variants, variants.contains(moduleName));
    }

    private void assertContainsCompletion(String name) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull("No completion items returned", items);
        for (LookupElement item : items) {
            if (name.equals(item.getLookupString())) {
                return;
            }
        }
        fail("Expected completion to include " + name + " but got: " + myFixture.getLookupElementStrings());
    }

    private void assertDoesNotContainCompletion(String name) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull(items);
        for (LookupElement item : items) {
            if (name.equals(item.getLookupString())) {
                fail("Expected completion to exclude " + name + " but got: " + myFixture.getLookupElementStrings());
            }
        }
    }

    private void assertCompletionHasTailText(String name, String expectedTailText) {
        final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        assertNotNull(items);
        for (LookupElement item : items) {
            if (name.equals(item.getLookupString())) {
                final LookupElementPresentation presentation = LookupElementPresentation.renderElement(item);
                assertTrue("Expected tail text '" + expectedTailText + "' for " + name,
                        Objects.toString(presentation.getTailText(), "").contains(expectedTailText));
                return;
            }
        }
        fail("Expected completion to include " + name + " but got: " + myFixture.getLookupElementStrings());
    }
}
