package com.javampire.openscad.action;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.javampire.openscad.settings.OpenSCADSettings;

import java.io.File;
import java.io.IOException;

public class OpenSCADEditorPopupMenuGroupTest extends BasePlatformTestCase {

    public void testEditorPopupMenuVisibleForScadFileWithExecutable() throws IOException {
        configureExecutable();
        myFixture.configureByText("model.scad", "cube(1);");

        final OpenSCADEditorPopupMenuGroup group = createPopupMenuGroup();
        final AnActionEvent event = createEditorPopupEvent(myFixture.getFile());
        group.update(event);

        assertTrue("OpenSCAD submenu should be visible in editor popup", event.getPresentation().isVisible());
        assertTrue("OpenSCAD submenu should be enabled when a child action applies", event.getPresentation().isEnabled());
    }

    public void testEditorPopupMenuHiddenWithoutExecutable() {
        OpenSCADSettings.getInstance().setOpenSCADExecutable(null);
        myFixture.configureByText("model.scad", "cube(1);");

        final OpenSCADEditorPopupMenuGroup group = createPopupMenuGroup();
        final AnActionEvent event = createEditorPopupEvent(myFixture.getFile());
        group.update(event);

        assertFalse("OpenSCAD submenu should be hidden without an executable", event.getPresentation().isVisible());
    }

    public void testEditorPopupMenuHiddenForNonScadFile() throws IOException {
        configureExecutable();
        myFixture.configureByText("readme.txt", "not openscad");

        final OpenSCADEditorPopupMenuGroup group = createPopupMenuGroup();
        final AnActionEvent event = createEditorPopupEvent(myFixture.getFile());
        group.update(event);

        assertFalse("OpenSCAD submenu should be hidden for non-.scad files", event.getPresentation().isVisible());
    }

    private static OpenSCADEditorPopupMenuGroup createPopupMenuGroup() {
        final OpenSCADEditorPopupMenuGroup group = new OpenSCADEditorPopupMenuGroup();
        group.add(new OpenAction());
        group.add(new ExportAction());
        group.add(new RefreshPreviewAction());
        return group;
    }

    private static AnActionEvent createEditorPopupEvent(com.intellij.psi.PsiFile psiFile) {
        return AnActionEvent.createEvent(
                SimpleDataContext.builder()
                        .add(CommonDataKeys.PSI_FILE, psiFile)
                        .add(CommonDataKeys.VIRTUAL_FILE, psiFile.getVirtualFile())
                        .add(CommonDataKeys.PROJECT, psiFile.getProject())
                        .build(),
                null,
                ActionPlaces.EDITOR_POPUP,
                ActionUiKind.POPUP,
                null
        );
    }

    private void configureExecutable() throws IOException {
        final File executable = File.createTempFile("openscad-test-", ".sh");
        assertTrue(executable.setExecutable(true));
        OpenSCADSettings.getInstance().setOpenSCADExecutable(executable.getAbsolutePath());
    }
}
