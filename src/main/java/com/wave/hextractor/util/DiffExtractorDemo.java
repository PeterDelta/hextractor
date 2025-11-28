package com.wave.hextractor.util;

import java.io.File;

public class DiffExtractorDemo {
    public static void main(String[] args) throws Exception {
        // Cambia las rutas por las de tus archivos reales
        File original = new File("C:/ruta/a/rom_original.gb");
        File mod = new File("C:/ruta/a/rom_modificada.gb");
        File outExt = new File("C:/ruta/a/diferencias.ext");

        FileUtils.extractDiffAsExt(original, mod, outExt);
        System.out.println("¡Extracción completada! Revisa el archivo: " + outExt.getAbsolutePath());
    }
}
