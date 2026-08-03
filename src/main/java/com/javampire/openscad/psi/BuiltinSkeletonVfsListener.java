package com.javampire.openscad.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class BuiltinSkeletonVfsListener implements BulkFileListener {

    @Override
    public void after(@NotNull final List<? extends VFileEvent> events) {
        boolean skeletonChanged = false;
        for (final VFileEvent event : events) {
            if (event instanceof VFileContentChangeEvent || event instanceof VFileDeleteEvent) {
                final VirtualFile file = event.getFile();
                if (file != null && isSkeletonFile(file)) {
                    skeletonChanged = true;
                    break;
                }
            }
        }
        if (!skeletonChanged) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            for (final Project project : ProjectManager.getInstance().getOpenProjects()) {
                BuiltinSkeletonResources.invalidateAll(project);
            }
        });
    }

    private static boolean isSkeletonFile(@NotNull final VirtualFile file) {
        final String name = file.getName();
        return "builtin_modules.scad".equals(name) || "builtin_functions.scad".equals(name);
    }
}
