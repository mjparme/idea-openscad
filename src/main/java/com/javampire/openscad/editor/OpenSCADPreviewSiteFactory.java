package com.javampire.openscad.editor;

import com.google.common.io.Resources;
import com.intellij.ide.browsers.*;
import com.intellij.ide.browsers.actions.WebPreviewVirtualFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.util.Url;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * Manage the creation/deletion of temporary files used for previewing scad file with the {@link OpenSCADPreviewFileEditor}.
 */
@Service(Service.Level.PROJECT)
public final class OpenSCADPreviewSiteFactory implements Disposable {

    private final static Logger LOG = Logger.getInstance(OpenSCADPreviewSiteFactory.class);
    private final static String HTML = "html";

    private final Project project;
    private VirtualFile htmlDir;

    public OpenSCADPreviewSiteFactory(@NotNull final Project project) {
        this.project = project;
    }

    public static OpenSCADPreviewSiteFactory getInstance(@NotNull final Project project) {
        return project.getService(OpenSCADPreviewSiteFactory.class);
    }


    public OpenSCADPreviewSite createSite(@NotNull final VirtualFile scadFile) {
        return WriteAction.compute(() -> {
            final OpenSCADPreviewSite previewSite = new OpenSCADPreviewSite(scadFile);
            createPreviewFile(previewSite);
            createHTMLFile(previewSite);
            Disposer.register(this, previewSite);
            return previewSite;
        });
    }

    private @Nullable VirtualFile getHtmlDir() {
        if (htmlDir == null || !new File(htmlDir.getPath()).exists()) {
            final VirtualFile outputDir = getOutputDir();
            if (outputDir == null) {
                LOG.error("Can not initialize preview output directory for project " + project.getName());
                return null;
            }

            htmlDir = outputDir.findChild(HTML);
            if (htmlDir != null && !new File(htmlDir.getPath()).exists()) {
                htmlDir.refresh(false, true);
                htmlDir = outputDir.findChild(HTML);
            }

            if (htmlDir == null || !new File(htmlDir.getPath()).exists()) {
                try {
                    htmlDir = outputDir.createChildDirectory(getInstance(project), HTML);
                }
                catch (IOException ioe) {
                    LOG.error("Can not initialize a temporary directory for scad file preview.", ioe);
                    htmlDir = null;
                    return null;
                }

                // Copy html and JS resources
                final VirtualFile jarHtmlRoot = VfsUtil.findFileByURL(getClass().getResource("/html"));
                try {
                    VfsUtil.copyDirectory(getInstance(project), jarHtmlRoot, htmlDir, null);
                }
                catch (final IOException ioe) {
                    LOG.error("Can not create copy resource file for scad file preview.", ioe);
                    htmlDir = null;
                }
            }
        }
        return htmlDir;
    }

    private void createPreviewFile(@NotNull final OpenSCADPreviewSite previewSite) {
        VirtualFile htmlDirTmp = getHtmlDir();
        if (htmlDirTmp == null) {
            LOG.error("Can not create preview file without an html output folder.");
            return;
        }

        final String previewFileRelativeName = computeSiteFilesRelativeName(previewSite.scadFile) + ".stl";
        try {
            previewSite.previewFile = getHtmlDir().findOrCreateChildData(this, previewFileRelativeName);
        }
        catch (final IOException ioe) {
            LOG.error("An error occurred while initializing preview file.", ioe);
        }
    }

    private void createHTMLFile(@NotNull final OpenSCADPreviewSite previewSite) {
        VirtualFile htmlDirTmp = getHtmlDir();
        if (htmlDirTmp == null) {
            LOG.error("Can not create html file without an html output folder.");
            return;
        }
        if (previewSite.previewFile == null) {
            LOG.error("Can not create html file without a preview STL file.");
            return;
        }

        // Creating a dedicated html file based on template one
        final String htmlFileName = previewSite.previewFile.getNameWithoutExtension() + ".html";
        final String htmlFileContent = generateHtmlFileContent(previewSite.previewFile);
        if (htmlFileContent == null) {
            return;
        }
        VirtualFile htmlFile;
        try {
            htmlFile = htmlDirTmp.findOrCreateChildData(this, htmlFileName);
            htmlFile.setBinaryContent(htmlFileContent.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException ioe) {
            LOG.error("Can not modify index html file for scad file preview.", ioe);
            return;
        }

        // Creating the WebPreviewVirtualFile from the html file
        final PsiElement htmlPsiFile = PsiManager.getInstance(project).findFile(htmlFile);
        if (htmlPsiFile == null) {
            LOG.error("Can not find html psi file.");
            return;
        }
        final OpenInBrowserRequest browserRequest = OpenInBrowserRequestKt.createOpenInBrowserRequest(
                htmlPsiFile,
                false
        );
        if (browserRequest != null) {
            browserRequest.setReloadMode(WebBrowserManager.getInstance().getWebPreviewReloadMode());
            try {
                Collection<Url> urls = WebBrowserService.getInstance().getUrlsToOpen(browserRequest, false);
                if (!urls.isEmpty()) {
                    Url url = urls.iterator().next();
                    previewSite.htmlFile = new WebPreviewVirtualFile(htmlFile, url);
                }
            } catch (final WebBrowserUrlProvider.BrowserException e) {
                LOG.error("An error occurred while getting internal preview url.", e);
            }
        }
    }

    private @Nullable String generateHtmlFileContent(@NotNull final VirtualFile previewFile) {
        final Color previewBackgroundColor = EditorColorsManager.getInstance().getSchemeForCurrentUITheme().getDefaultBackground();
        final String previewBackgroundHex = String.format("#%02X%02X%02X", previewBackgroundColor.getRed(), previewBackgroundColor.getGreen(), previewBackgroundColor.getBlue());
        try {
            return Resources.toString(getClass().getResource("/html/demo.html"), StandardCharsets.UTF_8)
                    .replaceAll("(demo\\.stl)", previewFile.getName())
                    .replaceAll("transparent", previewBackgroundHex);
        } catch (final IOException e) {
            LOG.error("An error occurred while getting template html file content.", e);
            return null;
        }
    }

    private @Nullable VirtualFile getOutputDir() {
        // Basic case, using compiler output path
        final CompilerProjectExtension compilerProjectExtension = CompilerProjectExtension.getInstance(project);
        if (compilerProjectExtension != null) {
            VirtualFile compilerOutputVirtualFile = compilerProjectExtension.getCompilerOutput();
            if (compilerOutputVirtualFile == null) {
                final String compilerOutputUrl = compilerProjectExtension.getCompilerOutputUrl();
                if (compilerOutputUrl != null) {
                    compilerOutputVirtualFile = createOutputDir(compilerOutputUrl);
                }
            }
            if (compilerOutputVirtualFile != null) {
                return compilerOutputVirtualFile;
            }
        }

        final Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length == 0) {
            return createOutputDirFromProjectBase();
        }

        // Current language probably does not have a compiler, getting an existing excluded temp folder
        final Module module = modules[0];
        final VirtualFile[] rootFiles = ModuleRootManager.getInstance(module).getExcludeRoots();
        if (rootFiles.length > 0) {
            return rootFiles[0];
        }

        // No existing temp folder, creating one from excluded paths
        final String[] excludeRootUrls = ModuleRootManager.getInstance(module).getExcludeRootUrls();
        if (excludeRootUrls.length > 0) {
            return createOutputDir(excludeRootUrls[0]);
        }

        // Last resort, using custom folder
        return createOutputDirFromProjectBase();
    }

    private @Nullable VirtualFile createOutputDirFromProjectBase() {
        final String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        final String DEFAULT_OUTPUT_FOLDER = "out";
        VirtualFile customFolder = project.getWorkspaceFile().findChild(DEFAULT_OUTPUT_FOLDER);
        if (customFolder == null) {
            customFolder = createOutputDir(basePath + "/" + DEFAULT_OUTPUT_FOLDER);
        }
        return customFolder;
    }

    private @Nullable VirtualFile createOutputDir(@Nullable final String compilerOutputUrl) {
        if (compilerOutputUrl == null) {
            return null;
        }
        final File compilerOutputFile = new File(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(compilerOutputUrl)));
        compilerOutputFile.mkdirs();
        return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(compilerOutputFile.toPath());
    }

    private String computeSiteFilesRelativeName(@NotNull final VirtualFile scadFile) {
        final VirtualFile moduleRoot = getModuleContentRoot(scadFile);
        if (moduleRoot == null) {
            return scadFile.getNameWithoutExtension();
        }
        return scadFile.getPath()
                .substring(0, scadFile.getPath().length() - 5)
                .replace(moduleRoot.getPath(), "")
                .replaceAll("^[/\\\\]*", "")
                .replaceAll("[ ./\\\\]", "_");
    }

    private @Nullable VirtualFile getModuleContentRoot(@NotNull final VirtualFile scadFile) {
        final Module currentModule = FileIndexFacade.getInstance(project).getModuleForFile(scadFile);
        if (currentModule == null) {
            return null;
        }
        VirtualFile moduleRoot = null;
        for (final VirtualFile rootVirtualFile : ModuleRootManager.getInstance(currentModule).getContentRoots()) {
            if (scadFile.getPath().contains(rootVirtualFile.getPath())) {
                moduleRoot = rootVirtualFile;
                break;
            }
        }
        return moduleRoot;
    }

    @Override
    public void dispose() {
        if (htmlDir == null) {
            return;
        }
        try {
            FileUtils.deleteDirectory(new File(htmlDir.getPath()));
        } catch (final IOException ioe) {
            LOG.warn("An error occurred while deleting temporary scad preview folder.", ioe);
        }
    }
}
