package com.javampire.openscad.editor;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.ReadAction.CannotReadException;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * JCEF availability check without a compile-time dependency on JBCefApp.
 * Preview requires the optional {@code com.intellij.modules.jcef} plugin module.
 */
final class JcefSupport {

    private static final Logger LOG = Logger.getInstance(JcefSupport.class);
    private static volatile Boolean supported;

    private JcefSupport() {
    }

    static boolean isSupported() {
        final Boolean cached = supported;
        if (cached != null) {
            return cached;
        }
        final Boolean detected = tryDetectSupport();
        if (detected == null) {
            return false;
        }
        supported = detected;
        return detected;
    }

    /**
     * @return definitive support result, or {@code null} when the check must be retried later
     */
    @Nullable
    private static Boolean tryDetectSupport() {
        try {
            return ReadAction.compute(JcefSupport::detectSupportUnderReadLock);
        }
        catch (CannotReadException e) {
            LOG.debug("JCEF support check deferred until read lock is available");
            return null;
        }
        catch (RuntimeException e) {
            if (e.getCause() instanceof CannotReadException) {
                LOG.debug("JCEF support check deferred until read lock is available");
                return null;
            }
            throw e;
        }
    }

    private static boolean detectSupportUnderReadLock() {
        try {
            final Class<?> jcefApp = Class.forName(
                    "com.intellij.ui.jcef.JBCefApp",
                    true,
                    OpenSCADPreviewFileEditor.class.getClassLoader()
            );
            final boolean result = Boolean.TRUE.equals(jcefApp.getMethod("isSupported").invoke(null));
            if (!result) {
                LOG.warn("JCEF module is loaded but JBCefApp.isSupported() returned false");
            }
            return result;
        }
        catch (ClassNotFoundException e) {
            LOG.warn("JCEF is not available for OpenSCAD preview: module not loaded", e);
            return false;
        }
        catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof CannotReadException cannotReadException) {
                throw cannotReadException;
            }
            LOG.warn("JCEF is not available for OpenSCAD preview", e);
            return false;
        }
    }
}
