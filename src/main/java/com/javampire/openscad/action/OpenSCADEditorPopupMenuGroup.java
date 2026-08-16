package com.javampire.openscad.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
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
        final ActionManager actionManager = ActionManager.getInstance();
        for (final AnAction action : getChildren(actionManager)) {
            final Presentation childPresentation = new Presentation();
            final AnActionEvent childEvent = AnActionEvent.createEvent(
                    action,
                    event.getDataContext(),
                    childPresentation,
                    event.getPlace(),
                    ActionUiKind.NONE,
                    null
            );
            ActionUtil.updateAction(action, childEvent);
            if (childPresentation.isVisible()) {
                anyVisible = true;
                if (childPresentation.isEnabled()) {
                    anyEnabled = true;
                }
            }
        }
        presentation.setVisible(anyVisible);
        presentation.setEnabled(anyEnabled);
    }
}
