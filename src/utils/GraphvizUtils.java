package utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GraphvizUtils {

    private GraphvizUtils() {}

    /**
     * Intenta localizar el binario 'dot' de Graphviz en rutas comunes de macOS y PATH.
     * Devuelve el comando a ejecutar (ruta absoluta si se encuentra, o "dot" como fallback).
     */
    public static String resolveDotExecutable() {
        // Rutas comunes con Homebrew/MacPorts/instaladores
        List<String> candidates = new ArrayList<>(Arrays.asList(
            "/opt/homebrew/bin/dot",   // Apple Silicon Homebrew
            "/usr/local/bin/dot",      // Intel Homebrew
            "/opt/local/bin/dot",      // MacPorts
            "/usr/bin/dot"             // Sistema (si existiese)
        ));

        for (String path : candidates) {
            File f = new File(path);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }
        // Fallback: confiar en PATH del entorno
        return "dot";
    }

    /**
     * Devuelve un PATH extendido que incluye rutas típicas para Finder (que inicia con PATH reducido).
     */
    public static String extendedPathEnv(String currentPath) {
        String extra = "/opt/homebrew/bin:/usr/local/bin:/opt/local/bin";
        if (currentPath == null || currentPath.isEmpty()) {
            return extra;
        }
        if (currentPath.contains("/opt/homebrew/bin") || currentPath.contains("/usr/local/bin") || currentPath.contains("/opt/local/bin")) {
            return currentPath;
        }
        return currentPath + ":" + extra;
    }
}


