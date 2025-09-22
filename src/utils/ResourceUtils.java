package utils;

import java.io.File;
import java.net.URISyntaxException;

public final class ResourceUtils {

    private ResourceUtils() {}

    /**
     * Devuelve una ruta File hacia un recurso empaquetado junto a la app cuando
     * se ejecuta dentro de un bundle creado por jpackage.
     * Prioriza, en este orden:
     * 1) Contenido dentro de Contents/app (ruta relativa al JAR principal)
     * 2) Ruta directa relativa al directorio de trabajo (modo desarrollo)
     */
    public static File resolveAppResource(String relativePathFromAppRoot) {
        // 1) Intentar relativo al JAR (Contents/app)
        try {
            File jarDir = new File(ResourceUtils.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParentFile();
            if (jarDir != null) {
                File candidate = new File(jarDir, relativePathFromAppRoot);
                if (candidate.exists()) {
                    return candidate;
                }
            }
        } catch (URISyntaxException ignored) {}

        // 2) Fallback: relativo al cwd (modo desarrollo)
        File dev = new File(relativePathFromAppRoot);
        return dev;
    }
}


