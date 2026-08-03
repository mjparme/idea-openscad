package com.javampire.openscad.psi;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class BuiltinSkeletonsTest extends BasePlatformTestCase {

    public void testPositionalFirstArgumentModulesIncludeCommonBuiltins() {
        assertTrue(BuiltinSkeletons.isPositionalFirstArgumentModule("cube"));
        assertTrue(BuiltinSkeletons.isPositionalFirstArgumentModule("sphere"));
        assertTrue(BuiltinSkeletons.isPositionalFirstArgumentModule("rotate"));
        assertTrue(BuiltinSkeletons.isPositionalFirstArgumentModule("translate"));
    }

    public void testPositionalFirstArgumentModulesExcludeOtherBuiltins() {
        assertFalse(BuiltinSkeletons.isPositionalFirstArgumentModule("linear_extrude"));
        assertFalse(BuiltinSkeletons.isPositionalFirstArgumentModule("union"));
    }
}
