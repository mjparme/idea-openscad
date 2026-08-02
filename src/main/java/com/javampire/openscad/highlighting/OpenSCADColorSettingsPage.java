package com.javampire.openscad.highlighting;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.javampire.openscad.OpenSCADIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class OpenSCADColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("Include", OpenSCADSyntaxHighlighter.IMPORT),
            new AttributesDescriptor("Include path", OpenSCADSyntaxHighlighter.IMPORT_PATH),
            new AttributesDescriptor("Identifier", OpenSCADSyntaxHighlighter.IDENTIFIER),
            new AttributesDescriptor("Module name", OpenSCADSyntaxHighlighter.MODULE_NAME),
            new AttributesDescriptor("Function name", OpenSCADSyntaxHighlighter.FUNCTION_NAME),
            new AttributesDescriptor("Variable name", OpenSCADSyntaxHighlighter.VARIABLE_NAME),
            new AttributesDescriptor("Parameter name", OpenSCADSyntaxHighlighter.PARAMETER_NAME),
            new AttributesDescriptor("Keyword", OpenSCADSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Builtin geometric operator", OpenSCADSyntaxHighlighter.OPERATOR_KEYWORD),
            new AttributesDescriptor("Builtin object primitive", OpenSCADSyntaxHighlighter.OBJECT_KEYWORD),
            new AttributesDescriptor("Builtin function", OpenSCADSyntaxHighlighter.FUNCTION_KEYWORD),
            new AttributesDescriptor("Number", OpenSCADSyntaxHighlighter.NUMBER),
            new AttributesDescriptor("String", OpenSCADSyntaxHighlighter.STRING),
            new AttributesDescriptor("Separator", OpenSCADSyntaxHighlighter.SEPARATOR),
            new AttributesDescriptor("Brackets", OpenSCADSyntaxHighlighter.BRACKETS),
            new AttributesDescriptor("Braces", OpenSCADSyntaxHighlighter.BRACES),
            new AttributesDescriptor("Parentheses", OpenSCADSyntaxHighlighter.PARENTHESES),
            new AttributesDescriptor("Comment", OpenSCADSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Customizer comment", OpenSCADSyntaxHighlighter.CUSTOMIZER_COMMENT),
            new AttributesDescriptor("Operation sign", OpenSCADSyntaxHighlighter.OPERATION_SIGN),
    };

    @Nullable
    @Override
    public Icon getIcon() {
        return OpenSCADIcons.OPENSCAD_LOGO;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new OpenSCADSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "<variable>myVar</variable> = 10;\n"
                + "module <module>myMod</module>(<parameter>size</parameter>) { cube(<parameter>size</parameter>); }\n"
                + "<module>myMod</module>(10);\n"
                + "function <function>myFunc</function>(<parameter>x</parameter>) = <variable>myVar</variable> + <parameter>x</parameter>;\n";
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return Map.of(
                "module", OpenSCADSyntaxHighlighter.MODULE_NAME,
                "function", OpenSCADSyntaxHighlighter.FUNCTION_NAME,
                "variable", OpenSCADSyntaxHighlighter.VARIABLE_NAME,
                "parameter", OpenSCADSyntaxHighlighter.PARAMETER_NAME
        );
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "OpenSCAD";
    }
}
