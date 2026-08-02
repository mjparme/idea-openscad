package com.javampire.openscad.editor;

import com.intellij.openapi.diagnostic.Logger;

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
        Boolean cached = supported;
        if (cached == null) {
            cached = detectSupport();
            supported = cached;
        }
        return cached;
    }

    private static boolean detectSupport() {
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
        catch (Throwable t) {
            LOG.warn("JCEF is not available for OpenSCAD preview", t);
            return false;
        }
    }
}
