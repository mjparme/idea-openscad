package com.javampire.openscad.action;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.javampire.openscad.OpenSCADLanguage;
import com.javampire.openscad.settings.OpenSCADSettings;

import java.io.File;
import java.io.IOException;

public class OpenSCADActionTest extends BasePlatformTestCase {

    public void testIsOpenScadVirtualFileByExtension() {
        final var file = myFixture.addFileToProject("model.scad", "cube(1);").getVirtualFile();
        assertTrue(OpenSCADAction.isOpenScadVirtualFile(file));
    }

    public void testIsOpenScadContextFromPsiFileInEditorPopup() throws IOException {
        configureExecutable();
        myFixture.configureByText("model.scad", "cube(1);");
        final AnActionEvent event = createEditorPopupEvent(myFixture.getFile());
        assertTrue(OpenSCADAction.isOpenScadContext(event));
    }

    public void testIsOpenScadContextFromVirtualFileWhenPsiMissing() throws IOException {
        configureExecutable();
        final var virtualFile = myFixture.addFileToProject("model.scad", "cube(1);").getVirtualFile();
        final AnActionEvent event = AnActionEvent.createFromDataContext(
                ActionPlaces.EDITOR_POPUP,
                null,
                SimpleDataContext.builder()
                        .add(CommonDataKeys.VIRTUAL_FILE, virtualFile)
                        .add(CommonDataKeys.PROJECT, myFixture.getProject())
                        .build()
        );
        assertTrue(OpenSCADAction.isOpenScadContext(event));
    }

    public void testOpenActionHiddenWithoutExecutable() {
        OpenSCADSettings.getInstance().setOpenSCADExecutable(null);
        myFixture.configureByText("model.scad", "cube(1);");
        final OpenAction action = new OpenAction();
        final AnActionEvent event = createEditorPopupEvent(myFixture.getFile());
        action.update(event);
        assertFalse(event.getPresentation().isVisible());
    }

    public void testOpenActionVisibleWithExecutable() throws IOException {
        configureExecutable();
        myFixture.configureByText("model.scad", "cube(1);");
        final OpenAction action = new OpenAction();
        final AnActionEvent event = createEditorPopupEvent(myFixture.getFile());
        action.update(event);
        assertTrue(event.getPresentation().isVisible());
        assertTrue(event.getPresentation().isEnabled());
    }

    private static AnActionEvent createEditorPopupEvent(com.intellij.psi.PsiFile psiFile) {
        return AnActionEvent.createFromDataContext(
                ActionPlaces.EDITOR_POPUP,
                null,
                SimpleDataContext.builder()
                        .add(CommonDataKeys.PSI_FILE, psiFile)
                        .add(CommonDataKeys.VIRTUAL_FILE, psiFile.getVirtualFile())
                        .add(CommonDataKeys.PROJECT, psiFile.getProject())
                        .build()
        );
    }

    private void configureExecutable() throws IOException {
        final File executable = File.createTempFile("openscad-test-", ".sh");
        assertTrue(executable.setExecutable(true));
        OpenSCADSettings.getInstance().setOpenSCADExecutable(executable.getAbsolutePath());
    }
}
