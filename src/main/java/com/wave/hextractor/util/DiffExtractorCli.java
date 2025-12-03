package com.wave.hextractor.util;

import java.io.File;

/**
 * Simple CLI to extract differences between two ROMs into a .ext file.
 * Usage:
 *   java -cp .Hextractor.jar com.wave.hextractor.util.DiffExtractorCli <original> <modified> <output.ext>
 */
public class DiffExtractorCli {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java -cp .Hextractor.jar com.wave.hextractor.util.DiffExtractorCli <original> <modified> <output.ext> [--raw]");
            System.exit(1);
        }

        File original = new File(args[0]);
        File modified = new File(args[1]);
        File outExt = new File(args[2]);

        if (!original.exists()) {
            System.err.println("ERROR: Original ROM not found: " + original.getAbsolutePath());
            System.exit(2);
        }
        if (!modified.exists()) {
            System.err.println("ERROR: Modified ROM not found: " + modified.getAbsolutePath());
            System.exit(3);
        }

        boolean ignoreChecksums = true;
        if (args.length >= 4 && "--raw".equalsIgnoreCase(args[3])) {
            ignoreChecksums = false;
        }

        FileUtils.extractDiffAsExt(original, modified, outExt, ignoreChecksums);
        // CLI silencioso por defecto: no imprimir nada en stdout si todo va bien
        // Mantener solo errores por stderr arriba
    }
}
