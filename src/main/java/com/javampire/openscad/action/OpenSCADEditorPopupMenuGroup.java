package com.javampire.openscad.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

/**
 * Hides the OpenSCAD editor context submenu when none of its actions apply,
 * instead of leaving a disabled submenu entry in the popup.
 */
public class OpenSCADEditorPopupMenuGroup extends DefaultActionGroup implements DumbAware {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        super.update(event);
        final Presentation presentation = event.getPresentation();
        boolean anyVisible = false;
        boolean anyEnabled = false;
        for (final AnAction action : getChildren(event)) {
            final Presentation child = action.getTemplatePresentation();
            if (child.isVisible()) {
                anyVisible = true;
                if (child.isEnabled()) {
                    anyEnabled = true;
                }
            }
        }
        presentation.setVisible(anyVisible);
        presentation.setEnabled(anyEnabled);
    }
}
