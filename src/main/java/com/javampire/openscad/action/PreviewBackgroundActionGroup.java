package com.javampire.openscad.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.javampire.openscad.settings.OpenSCADSettings;
import com.javampire.openscad.editor.OpenSCADPreviewFileEditor;
import com.javampire.openscad.editor.PreviewBackground;
import org.jetbrains.annotations.NotNull;

/**
 * Toolbar dropdown for preview scene background.
 */
public class PreviewBackgroundActionGroup extends DefaultActionGroup implements DumbAware {

    public PreviewBackgroundActionGroup() {
        super("Background", true);
        for (final PreviewBackground background : PreviewBackground.values()) {
            add(new SetPreviewBackgroundAction(background));
        }
        getTemplatePresentation().setIcon(AllIcons.Actions.Colors);
        getTemplatePresentation().setDescription("Preview scene background");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        final Presentation presentation = event.getPresentation();
        if (!OpenSCADSettings.getInstance().hasExecutable()
                || !OpenSCADAction.isSupportedActionPlace(event)
                || !OpenSCADAction.isOpenScadContext(event)) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        presentation.setEnabledAndVisible(true);
        final OpenSCADPreviewFileEditor previewEditor = event.getData(OpenSCADDataKeys.PREVIEW_EDITOR);
        if (previewEditor != null) {
            presentation.setText("Background: " + previewEditor.getEditorConfig().getPreviewBackground().getDisplayName());
        }
        else {
            presentation.setText("Background");
        }
        presentation.setDescription("Preview scene background");
        presentation.setIcon(AllIcons.Actions.Colors);
    }
}
