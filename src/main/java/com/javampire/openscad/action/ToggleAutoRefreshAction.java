package com.javampire.openscad.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.javampire.openscad.OpenSCADIcons;
import com.javampire.openscad.editor.OpenSCADPreviewFileEditor;
import org.jetbrains.annotations.NotNull;

/**
 * Can be called from preview toolbar. Toggles auto refresh on save.
 */
public class ToggleAutoRefreshAction extends OpenSCADAction {

    @Override
    public void update(@NotNull final AnActionEvent event) {
        final Presentation presentation = checkOpenSCADPrerequisites(event);
        if (presentation.isVisible()) {
            presentation.setText("Toggle Auto Refresh On Save");
            presentation.setDescription("Toggle auto refresh on save");
            final OpenSCADPreviewFileEditor previewEditor = event.getData(OpenSCADDataKeys.PREVIEW_EDITOR);
            if (previewEditor != null && Boolean.TRUE.equals(previewEditor.getEditorConfig().getAutoRefresh())) {
                presentation.setIcon(OpenSCADIcons.AUTO_REFRESH);
            } else {
                presentation.setIcon(OpenSCADIcons.AUTO_REFRESH_GRAYED);
            }
        }
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        final OpenSCADPreviewFileEditor previewEditor = event.getData(OpenSCADDataKeys.PREVIEW_EDITOR);
        if (previewEditor != null) {
            previewEditor.getEditorConfig().toggleAutoRefresh();
        }
    }
}
