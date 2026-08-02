package com.javampire.openscad.templates;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.javampire.openscad.OpenSCADLanguage;
import org.jetbrains.annotations.NotNull;

final class OpenSCADTemplateContextType extends TemplateContextType {

    OpenSCADTemplateContextType() {
        super("OpenSCAD");
    }

    @Override
    public boolean isInContext(@NotNull TemplateActionContext context) {
        return context.getFile().getLanguage().is(OpenSCADLanguage.INSTANCE);
    }
}
