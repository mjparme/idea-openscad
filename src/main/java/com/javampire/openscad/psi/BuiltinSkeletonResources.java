package com.javampire.openscad.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.javampire.openscad.completion.OpenSCADCompletionContributor;
import com.javampire.openscad.psi.stub.module.OpenSCADModuleIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.CRC32;

/**
 * Loads bundled builtin skeleton resources and coordinates cache invalidation.
 */
public final class BuiltinSkeletonResources {

    public static final String MODULES_RESOURCE = "/com/javampire/openscad/skeletons/builtin_modules.scad";
    public static final String FUNCTIONS_RESOURCE = "/com/javampire/openscad/skeletons/builtin_functions.scad";

    private BuiltinSkeletonResources() {
    }

    @Nullable
    public static VirtualFile findVirtualFile(@NotNull final String resourcePath) {
        final URL resourceUrl = BuiltinSkeletonResources.class.getResource(resourcePath);
        return resourceUrl != null ? VfsUtil.findFileByURL(resourceUrl) : null;
    }

    /**
     * Content hash of a bundled resource. Unlike {@link VirtualFile#getModificationStamp()}, this
     * changes when the plugin JAR is rebuilt even if the archive timestamp is unchanged.
     */
    public static long contentHash(@NotNull final String resourcePath) {
        try (InputStream inputStream = BuiltinSkeletonResources.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return -1;
            }
            final CRC32 crc32 = new CRC32();
            final byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                crc32.update(buffer, 0, read);
            }
            return crc32.getValue();
        }
        catch (IOException ignored) {
            return -1;
        }
    }

    public static void invalidateAll(@Nullable final Project project) {
        BuiltinSkeletons.clearCaches();
        OpenSCADCompletionContributor.clearBuiltinModuleCompletionCache();
        if (project != null && !project.isDisposed()) {
            FileBasedIndex.getInstance().requestRebuild(OpenSCADModuleIndex.MODULE);
        }
    }
}
