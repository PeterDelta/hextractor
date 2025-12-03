package com.wave.hextractor.util;

import com.wave.hextractor.Hextractor;

import java.text.MessageFormat;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The Class ProjectUtils.
 */
public class ProjectUtils {

	/**
	 * Instantiates a new project utils.
	 */
	private ProjectUtils() {
	}

	/** The Constant ECHO_OFF. */
	private static final String ECHO_OFF = "@echo off";

	/** The Constant PAUSE. */
	private static final String PAUSE = "pause";

	/** The Constant PROG_CALL. */
	private static final String PROG_CALL = "java -jar .Hextractor.jar ";

	/** The Constant INSERT_FILE. */
	private static final String INSERT_FILE = "1.InsertAll.bat";

	/** The Constant COMPARE_ROMS_FILE. */
	private static final String COMPARE_ROMS_FILE = "2.Compare_ROMs.bat";

	/** The Constant CREATEPATCH_FILE. */
	private static final String CREATEPATCH_FILE = "3.CreatePatch.bat";

	/** The Constant TR_FILENAME_PREFIX. */
	private static final String TR_FILENAME_PREFIX = "TR_";

	/** The Constant YEAR. */
	private static final String YEAR = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

	/** The Constant SCRIPTNAME_VAR. */
	private static final String SCRIPTNAME_VAR = "%SCRIPTNAME%";

	/** The Constant TFILENAMENAME_VAR. */
	private static final String TFILENAMENAME_VAR = "%T_FILENAME%";

	/** The Constant SFILENAMENAME_VAR. */
	private static final String SFILENAMENAME_VAR = "%S_FILENAME%";

	/** The Constant FILE_HEXTRACTOR. */
	private static final String FILE_HEXTRACTOR = ".Hextractor.jar";

	/** The Constant FILE_README. */
	private static final String FILE_README = "_readme.txt";

	/**
	 * Creates the batch header.
	 *
	 * @return the batch header
	 */
	private static String createBatchHeader() {
		return ECHO_OFF + Constants.NEWLINE;
	}

	/**
	 * Creates the batch footer.
	 *
	 * @return the batch footer
	 */
	private static String createBatchFooter() {
		return PAUSE + Constants.NEWLINE;
	}

	/**
	 * Gets the tfile name.
	 *
	 * @param name the name
	 * @return the tfile name
	 */
	private static String getTfileName(String name) {
		return "set T_FILENAME=\"" + name + "\"";
	}

	/**
	 * Gets the sfile name.
	 *
	 * @param name the name
	 * @return the sfile name
	 */
	private static String getSfileName(String name) {
		return "set S_FILENAME=\"" + name + "\"";
	}

	/**
	 * Gets the script name.
	 *
	 * @param name the name
	 * @return the script name
	 */
	private static String getScriptName(String name) {
		return "set SCRIPTNAME=\"" + name + "\"";
	}

	/**
	 * Creates the new project.
	 *
	 * @param name the name
	 * @param fileName the file name
	 * @param fileType the file type
	 * @param projectFile the project file
	 * @throws IOException the exception
	 */
	/**
	 * Creates the new project.
	 *
	 * @param name the name
	 * @param fileName the file name
	 * @param fileType the file type
	 * @param projectFile the project file
	 * @throws IOException the exception
	 */
	public static void createNewProject(String name, String fileName, String fileType, File projectFile) throws IOException {
		File projectFolder = createProjectFolder(name);
		String transfileName = TR_FILENAME_PREFIX + fileName;
		// autoFixChecksum es true si el tipo de archivo NO es "Otros" (FILE_TYPE_OTHER)
		boolean autoFixChecksum = !Constants.FILE_TYPE_OTHER.equals(fileType);
		copyBaseFiles(projectFolder, name, projectFile);
		Utils.log(Utils.getMessage("consoleGeneratingFiles"));
		Utils.createFile(Utils.getJoinedFileName(projectFolder, INSERT_FILE), createInsertFile(name, fileName, fileType, transfileName, autoFixChecksum));
		Utils.createFile(Utils.getJoinedFileName(projectFolder, COMPARE_ROMS_FILE), createCompareRomsFile(name, fileName, transfileName));
		Utils.createFile(Utils.getJoinedFileName(projectFolder, CREATEPATCH_FILE), createCreatePatchFile(name, fileName, transfileName));
	}

	/**
	 * Creates the project.
	 *
	 * @param file the file
	 * @throws IOException the exception
	 */
	public static void createProject(File file) throws IOException {
		String baseName = getProjectName(file.getName());
		String newExtension = FileUtils.getFileExtension(file.getName());
		String finalName = baseName;
		
		// Buscar todas las carpetas que empiecen con el nombre base
		File currentDir = new File(".");
		File[] allFolders = currentDir.listFiles(f -> 
			f.isDirectory() && 
			(f.getName().equals(baseName) || f.getName().startsWith(baseName + "."))
		);
		
		boolean foundTargetFolder = false;
		
		if (allFolders != null && allFolders.length > 0) {
			// Buscar carpetas con ROMs
			for (File folder : allFolders) {
				File[] files = folder.listFiles();
				boolean hasRoms = false;
				String folderRomExtension = null;
				
				if (files != null) {
					// Buscar ROMs en la carpeta (ignorar TR_, .Hextractor.jar, .bat, .txt)
					for (File f : files) {
						if (f.isFile() && !f.getName().startsWith("TR_") && 
							!f.getName().equals(".Hextractor.jar") &&
						    !f.getName().endsWith(".bat") &&
						    !f.getName().endsWith(".txt")) {
							String ext = FileUtils.getFileExtension(f.getName());
							if (ext != null && isRomExtension(ext)) {
								hasRoms = true;
								folderRomExtension = ext;
								break;
							}
						}
					}
				}
				
				// Si la carpeta no tiene ROMs (está vacía o solo tiene archivos generados), usarla
				if (!hasRoms) {
					finalName = folder.getName();
					foundTargetFolder = true;
					break;
				}
				// Si tiene ROM con la misma extensión, sobrescribir ahí
				else if (folderRomExtension != null && folderRomExtension.equalsIgnoreCase(newExtension)) {
					finalName = folder.getName();
					foundTargetFolder = true;
					break;
				}
			}
			
			// Si todas las carpetas existentes tienen ROMs con extensiones diferentes, crear nueva con extensión
			if (!foundTargetFolder && newExtension != null && !newExtension.isEmpty()) {
				finalName = baseName + "." + newExtension;
			}
		}
		// Si no hay ninguna carpeta existente, usar el nombre base sin extensión (primera vez)
		
		ProjectUtils.createNewProject(finalName, file.getName(), getFileType(file), file);
	}

	/**
	 * Creates the project, honoring a user-selected file type when provided.
	 * If {@code selectedFileType} is null, empty, or not provided, the file type
	 * will be inferred from the file extension as usual.
	 *
	 * @param file the ROM file to base the project on
	 * @param selectedFileType the file type selected by the user (e.g. Constants.FILE_TYPE_OTHER)
	 * @throws IOException the exception
	 */
	public static void createProject(File file, String selectedFileType) throws IOException {
		String baseName = getProjectName(file.getName());
		String newExtension = FileUtils.getFileExtension(file.getName());
		String finalName = baseName;

		// Buscar todas las carpetas que empiecen con el nombre base
		File currentDir = new File(".");
		File[] allFolders = currentDir.listFiles(f ->
			f.isDirectory() &&
			(f.getName().equals(baseName) || f.getName().startsWith(baseName + "."))
		);

		boolean foundTargetFolder = false;

		if (allFolders != null && allFolders.length > 0) {
			// Buscar carpetas con ROMs
			for (File folder : allFolders) {
				File[] files = folder.listFiles();
				boolean hasRoms = false;
				String folderRomExtension = null;

				if (files != null) {
					// Buscar ROMs en la carpeta (ignorar TR_, .Hextractor.jar, .bat, .txt)
					for (File f : files) {
						if (f.isFile() && !f.getName().startsWith("TR_") &&
							!f.getName().equals(".Hextractor.jar") &&
							!f.getName().endsWith(".bat") &&
							!f.getName().endsWith(".txt")) {
							String ext = FileUtils.getFileExtension(f.getName());
							if (ext != null && isRomExtension(ext)) {
								hasRoms = true;
								folderRomExtension = ext;
								break;
							}
						}
					}
				}

				// Si la carpeta no tiene ROMs (está vacía o solo tiene archivos generados), usarla
				if (!hasRoms) {
					finalName = folder.getName();
					foundTargetFolder = true;
					break;
				}
				// Si tiene ROM con la misma extensión, sobrescribir ahí
				else if (folderRomExtension != null && folderRomExtension.equalsIgnoreCase(newExtension)) {
					finalName = folder.getName();
					foundTargetFolder = true;
					break;
				}
			}

			// Si todas las carpetas existentes tienen ROMs con extensiones diferentes, crear nueva con extensión
			if (!foundTargetFolder && newExtension != null && !newExtension.isEmpty()) {
				finalName = baseName + "." + newExtension;
			}
		}

		// Determinar el tipo de archivo a utilizar: el seleccionado por el usuario (si viene informado)
		// o el detectado por extensión como comportamiento por defecto.
		String effectiveFileType = (selectedFileType != null && !selectedFileType.isEmpty())
				? selectedFileType
				: getFileType(file);

		ProjectUtils.createNewProject(finalName, file.getName(), effectiveFileType, file);
	}
	
	/**
	 * Checks if extension is a known ROM extension.
	 */
	private static boolean isRomExtension(String extension) {
		if (extension == null) return false;
		String ext = extension.toLowerCase();
		for (java.util.List<String> extensions : Constants.FILE_TYPE_EXTENSIONS.values()) {
			if (extensions.contains(ext)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates the project.
	 *
	 * @param file the file
	 * @throws IOException the exception
	 */
	public static void createProject(String file) throws IOException {
		ProjectUtils.createProject(new File(file));
	}

	/**
	 * Gets the file type.
	 *
	 * @param file the file
	 * @return the file type
	 */
	private static String getFileType(File file) {
		String res = Constants.FILE_TYPE_OTHER;
		if(file != null) {
			String extension =  FileUtils.getFileExtension(file);
			for(java.util.Map.Entry<String, java.util.List<String>> entry : Constants.FILE_TYPE_EXTENSIONS.entrySet()) {
				if(entry.getValue().contains(extension)) {
					res = entry.getKey();
					break;
				}
			}
		}
		return res;
	}

	/**
	 * Creates the compare ROMs file.
	 */
	private static String createCompareRomsFile(String name, String fileName, String transfileName) {
		StringBuilder b = new StringBuilder();
		// Header
		b.append(createBatchHeader());
		b.append("setlocal enabledelayedexpansion").append(Constants.NEWLINE).append(Constants.NEWLINE);
		// Title
		b.append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batCompareTitle").append(Constants.NEWLINE);
		b.append("echo.").append(Constants.NEWLINE).append(Constants.NEWLINE);
		// Drag & drop guard
		b.append("if \"%~1\"==\"\" (").append(Constants.NEWLINE);
		b.append("    ").append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batErrorDragFiles").append(Constants.NEWLINE);
		b.append("    echo.").append(Constants.NEWLINE);
		b.append("    ").append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batUsageHint").append(Constants.NEWLINE);
		b.append("    pause").append(Constants.NEWLINE);
		b.append("    exit /b 1").append(Constants.NEWLINE);
		b.append(")").append(Constants.NEWLINE).append(Constants.NEWLINE);
		// Determine dir
		b.append("set \"DIR=%~dp1\"").append(Constants.NEWLINE).append(Constants.NEWLINE);
		b.append("set ORIGINAL=").append(Constants.NEWLINE);
		b.append("set MODIFIED=").append(Constants.NEWLINE).append(Constants.NEWLINE);
		// Process args loop
		b.append(":process_args").append(Constants.NEWLINE);
		b.append("if \"%~1\"==\"\" goto find_pairs").append(Constants.NEWLINE).append(Constants.NEWLINE);
		b.append("set \"FILENAME=%~nx1\"").append(Constants.NEWLINE);
		b.append("set \"FULLPATH=%~f1\"").append(Constants.NEWLINE).append(Constants.NEWLINE);
		b.append("if \"!FILENAME:~0,3!\"==\"TR_\" (").append(Constants.NEWLINE);
		b.append("    set \"MODIFIED=!FULLPATH!\"").append(Constants.NEWLINE);
		b.append(") else (").append(Constants.NEWLINE);
		b.append("    set \"ORIGINAL=!FULLPATH!\"").append(Constants.NEWLINE);
		b.append(")").append(Constants.NEWLINE).append(Constants.NEWLINE);
		b.append("shift").append(Constants.NEWLINE);
		b.append("goto process_args").append(Constants.NEWLINE).append(Constants.NEWLINE);
		// Find pairs
		b.append(":find_pairs").append(Constants.NEWLINE);
		b.append("if \"!ORIGINAL!\"==\"\" (").append(Constants.NEWLINE);
		b.append("    if not \"!MODIFIED!\"==\"\" (").append(Constants.NEWLINE);
		b.append("        for %%f in (\"!MODIFIED!\") do set \"BASENAME=%%~nf\"").append(Constants.NEWLINE);
		b.append("        if \"!BASENAME:~0,3!\"==\"TR_\" set \"BASENAME=!BASENAME:~3!\"").append(Constants.NEWLINE);
		b.append("        for %%f in (\"!MODIFIED!\") do set \"EXT=%%~xf\"").append(Constants.NEWLINE);
		b.append("        set \"ORIGINAL=!DIR!!BASENAME!!EXT!\"").append(Constants.NEWLINE);
		b.append("        if not exist \"!ORIGINAL!\" (").append(Constants.NEWLINE);
		b.append("            ").append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batErrorOriginalNotFound \"!BASENAME!!EXT!\"").append(Constants.NEWLINE);
		b.append("            pause").append(Constants.NEWLINE);
		b.append("            exit /b 1").append(Constants.NEWLINE);
		b.append("        )").append(Constants.NEWLINE);
		b.append("    )").append(Constants.NEWLINE);
		b.append(")").append(Constants.NEWLINE).append(Constants.NEWLINE);
		b.append("if \"!MODIFIED!\"==\"\" (").append(Constants.NEWLINE);
		b.append("    if not \"!ORIGINAL!\"==\"\" (").append(Constants.NEWLINE);
		b.append("        for %%f in (\"!ORIGINAL!\") do set \"BASENAME=%%~nf\"").append(Constants.NEWLINE);
		b.append("        for %%f in (\"!ORIGINAL!\") do set \"EXT=%%~xf\"").append(Constants.NEWLINE);
		b.append("        set \"MODIFIED=!DIR!TR_!BASENAME!!EXT!\"").append(Constants.NEWLINE);
		b.append("        if not exist \"!MODIFIED!\" (").append(Constants.NEWLINE);
		b.append("            ").append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batErrorModifiedNotFound \"TR_!BASENAME!!EXT!\"").append(Constants.NEWLINE);
		b.append("            pause").append(Constants.NEWLINE);
		b.append("            exit /b 1").append(Constants.NEWLINE);
		b.append("        )").append(Constants.NEWLINE);
		b.append("    )").append(Constants.NEWLINE);
		b.append(")").append(Constants.NEWLINE).append(Constants.NEWLINE);
		// Names only
		b.append("for %%f in (\"!ORIGINAL!\") do set \"ORIGNAME=%%~nxf\"").append(Constants.NEWLINE);
		b.append("for %%f in (\"!MODIFIED!\") do set \"MODNAME=%%~nxf\"").append(Constants.NEWLINE);
		b.append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batOriginalRom \"!ORIGNAME!\"").append(Constants.NEWLINE);
		b.append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batModifiedRom \"!MODNAME!\"").append(Constants.NEWLINE);
		b.append("echo.").append(Constants.NEWLINE).append(Constants.NEWLINE);
		// Output name TR_#<folder>.ext
		b.append("set \"TMPDIR=!DIR!\"").append(Constants.NEWLINE);
		b.append("if \"!TMPDIR:~-1!\"==\"\\\" set \"TMPDIR=!TMPDIR:~0,-1!\"").append(Constants.NEWLINE);
		b.append("for %%p in (\"!TMPDIR!\") do set \"FOLDER=%%~nxp\"").append(Constants.NEWLINE);
		b.append("set \"OUTPUT=!DIR!TR_#!FOLDER!.ext\"").append(Constants.NEWLINE).append(Constants.NEWLINE);
		// Overwrite prompt default N
		b.append("if exist \"!OUTPUT!\" (").append(Constants.NEWLINE);
		b.append("    ").append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batWarningFileExists \"TR_#!FOLDER!.ext\"").append(Constants.NEWLINE);
		b.append("    ").append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batOverwritePrompt").append(Constants.NEWLINE);
		b.append("    set /p \"OVERWRITE=\"").append(Constants.NEWLINE);
		b.append("    if \"!OVERWRITE!\"==\"\" set \"OVERWRITE=N\"").append(Constants.NEWLINE);
		b.append("    if /i \"!OVERWRITE!\" neq \"S\" (").append(Constants.NEWLINE);
		b.append("        echo.").append(Constants.NEWLINE);
		b.append("        ").append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batOperationCancelled").append(Constants.NEWLINE);
		b.append("        pause").append(Constants.NEWLINE);
		b.append("        exit /b 0").append(Constants.NEWLINE);
		b.append("    )").append(Constants.NEWLINE);
		b.append("    echo.").append(Constants.NEWLINE);
		b.append(")").append(Constants.NEWLINE).append(Constants.NEWLINE);
		// Call CLI
		b.append("java -cp \".Hextractor.jar\" com.wave.hextractor.util.DiffExtractorCli \"!ORIGINAL!\" \"!MODIFIED!\" \"!OUTPUT!\"").append(Constants.NEWLINE).append(Constants.NEWLINE);
		// Error check
		b.append("if !errorlevel! neq 0 (").append(Constants.NEWLINE);
		b.append("    echo.").append(Constants.NEWLINE);
		b.append("    ").append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batErrorExtraction").append(Constants.NEWLINE);
		b.append("    pause").append(Constants.NEWLINE);
		b.append("    exit /b 1").append(Constants.NEWLINE);
		b.append(")").append(Constants.NEWLINE).append(Constants.NEWLINE);
		// Completed
		b.append(PROG_CALL).append(Hextractor.MODE_PRINT_MESSAGE).append(" batCompleted \"TR_#!FOLDER!.ext\"").append(Constants.NEWLINE);
		b.append("echo.").append(Constants.NEWLINE);
		b.append(createBatchFooter());
		return b.toString();
	}

	/**
	 * Creates the insert file.
	 *
	 * @param name the name
	 * @param fileName the file name
	 * @param fileType the file type
	 * @param transfileName the transfile name
	 * @return the string
	 */
	private static String createInsertFile(String name, String fileName, String fileType, String transfileName, boolean autoFixChecksum) {
		StringBuilder fileContent = new StringBuilder();
		fileContent.append(createBatchHeader());
		fileContent.append(getTfileName(transfileName)).append(Constants.NEWLINE);
		fileContent.append(getSfileName(fileName)).append(Constants.NEWLINE);
		fileContent.append(getScriptName(name)).append(Constants.NEWLINE);
		// Validación de archivos TR_*.ext
		fileContent.append("if not exist TR_*.ext (").append(Constants.NEWLINE);
		fileContent.append("    echo ").append(MessageFormat.format(Utils.getMessage("batErrorExtNotFound"), SCRIPTNAME_VAR)).append(Constants.NEWLINE);
		fileContent.append("    pause").append(Constants.NEWLINE);
		fileContent.append("    exit /b 1").append(Constants.NEWLINE);
		fileContent.append(")").append(Constants.NEWLINE);
		// Validación de archivo .tbl
		fileContent.append("if not exist ").append(SCRIPTNAME_VAR).append(".tbl (").append(Constants.NEWLINE);
		fileContent.append("    echo ").append(MessageFormat.format(Utils.getMessage("batErrorTblNotFound"), SCRIPTNAME_VAR)).append(Constants.NEWLINE);
		fileContent.append("    pause").append(Constants.NEWLINE);
		fileContent.append("    exit /b 1").append(Constants.NEWLINE);
		fileContent.append(")").append(Constants.NEWLINE);
		fileContent.append("del " + TFILENAMENAME_VAR).append(Constants.NEWLINE);
		fileContent.append("copy " + SFILENAMENAME_VAR + " " + TFILENAMENAME_VAR).append(Constants.NEWLINE);
		// Bucle for con separación entre archivos
		fileContent.append("for %%F in (TR_*.ext) do (").append(Constants.NEWLINE);
		fileContent.append("    java -jar .Hextractor.jar -h %SCRIPTNAME%.tbl \"%%F\" %T_FILENAME%").append(Constants.NEWLINE);
		fileContent.append("    echo.").append(Constants.NEWLINE);
		fileContent.append(")").append(Constants.NEWLINE);
		String checksumMode = getChecksumMode(fileName, fileType);
		if(autoFixChecksum && checksumMode.length() > 0) {
			fileContent.append(PROG_CALL).append(checksumMode).append(" ").append(TFILENAMENAME_VAR).append(Constants.NEWLINE);
			fileContent.append("echo.").append(Constants.NEWLINE);
		}
		fileContent.append(createBatchFooter());
		return fileContent.toString();
	}

	private static String getChecksumMode(String fileName, String fileType) {
		String checksumMode = Constants.EMPTY;
		if(Constants.FILE_TYPE_MEGADRIVE.equals(fileType) || fileName.endsWith(".32x")) {
			checksumMode = Hextractor.MODE_FIX_MEGADRIVE_CHECKSUM;
		}
		else {
			if(Constants.FILE_TYPE_NGB.equals(fileType)) {
				checksumMode = Hextractor.MODE_FIX_GAMEBOY_CHECKSUM;
			}
			else {
				if(Constants.FILE_TYPE_SNES.equals(fileType)) {
					checksumMode = Hextractor.MODE_FIX_SNES_CHECKSUM;
				}
				else {
					if(Constants.FILE_TYPE_ZXTAP.equals(fileType)) {
						checksumMode = Hextractor.MODE_FIX_ZXTAP_CHECKSUM;
					}
					else {
						if(Constants.FILE_TYPE_TZX.equals(fileType)) {
							checksumMode = Hextractor.MODE_FIX_ZXTZX_CHECKSUM;
						}
						else {
							if(Constants.FILE_TYPE_MASTERSYSTEM.equals(fileType)) {
								checksumMode = Hextractor.MODE_FIX_SMS_CHECKSUM;
							}
						}
					}
				}
			}
		}
		return checksumMode;
	}

	/**
	 * Creates the create patch file.
	 *
	 * @param name the name
	 * @param fileName the file name
	 * @param transFileName the trans file name
	 * @return the string
	 */
	private static String createCreatePatchFile(String name, String fileName, String transFileName) {
		StringBuilder fileContent = new StringBuilder();
		fileContent.append(createBatchHeader());
		fileContent.append(getTfileName(transFileName)).append(Constants.NEWLINE);
		fileContent.append(getSfileName(fileName)).append(Constants.NEWLINE);
		fileContent.append(getScriptName(name)).append(Constants.NEWLINE);
		fileContent.append(PROG_CALL).append(Hextractor.CREATE_IPS_PATCH + " " + SFILENAMENAME_VAR + " " + TFILENAMENAME_VAR + " " + SCRIPTNAME_VAR +".ips").append(Constants.NEWLINE);
		fileContent.append(PROG_CALL).append(Hextractor.MODE_FILL_READ_ME + " ../.Hextractor/_readme.txt " + SCRIPTNAME_VAR + "_readme.txt " + SFILENAMENAME_VAR).append(Constants.NEWLINE);
		fileContent.append("echo.").append(Constants.NEWLINE);
		fileContent.append(createBatchFooter());
		return fileContent.toString();
	}

	/**
	 * Creates the hex file.
	 *
	 * @param name the name
	 * @return the string
	 */
	/**
	 * Creates the extract hex file.
	 *
	 * @param name the file name
	 * @param transfileName the transfile name
	 * @return the string
	 */

	/**
	 * Copy base files.
	 *
	 * @param projectFolder the project folder
	 * @param name the name
	 * @param projectFile the project file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void copyBaseFiles(File projectFolder, String name, File projectFile) throws IOException {
		// Obtener la ruta del JAR ejecutable actual
		String jarPath = getExecutableJarPath();
		if(jarPath != null && new File(jarPath).exists()) {
			Utils.copyFileUsingStream(jarPath, Utils.getJoinedFileName(projectFolder, FILE_HEXTRACTOR));
		} else {
			// Si no se encuentra el JAR, intentar copiar desde el classpath
			System.err.println(MessageFormat.format("No se pudo encontrar {0} en {1}", FILE_HEXTRACTOR, jarPath));
		}
		
		// Crear el archivo README en lugar de copiarlo
		Utils.createFile(Utils.getJoinedFileName(projectFolder, name + FILE_README), createReadmeContent(name));
		
		if(projectFile != null) {
			Utils.copyFileUsingStream(projectFile.getAbsolutePath(), Utils.getJoinedFileName(projectFolder, projectFile.getName()));
			Utils.copyFileUsingStream(projectFile.getAbsolutePath(), Utils.getJoinedFileName(projectFolder, TR_FILENAME_PREFIX + projectFile.getName()));
		}
	}
	
	/**
	 * Creates the readme content.
	 *
	 * @param name the project name
	 * @return the readme content
	 */
	private static String createReadmeContent(String name) {
		ResourceBundle rb = ResourceBundle.getBundle(Constants.RB_NAME, Locale.getDefault(), new UTF8Control());
		return rb.getString(KeyConstants.KEY_README_PROJECT_TITLE).formatted(name) + Constants.NEWLINE +
				"=".repeat(50) + Constants.NEWLINE + Constants.NEWLINE +
				rb.getString(KeyConstants.KEY_README_GENERATED_WITH) + Constants.NEWLINE +
				rb.getString(KeyConstants.KEY_README_DATE).formatted(YEAR) + Constants.NEWLINE + Constants.NEWLINE +
				rb.getString(KeyConstants.KEY_README_USAGE) + Constants.NEWLINE +
				rb.getString(KeyConstants.KEY_README_STEP1) + Constants.NEWLINE +
				rb.getString(KeyConstants.KEY_README_STEP2) + Constants.NEWLINE +
				rb.getString(KeyConstants.KEY_README_STEP3) + Constants.NEWLINE +
				rb.getString(KeyConstants.KEY_README_STEP4) + Constants.NEWLINE +
				rb.getString(KeyConstants.KEY_README_STEP5) + Constants.NEWLINE +
				rb.getString(KeyConstants.KEY_README_STEP6) + Constants.NEWLINE;
	}
	
	/**
	 * Gets the path of the currently executing JAR file.
	 *
	 * @return the executable jar path
	 */
	private static String getExecutableJarPath() {
		try {
			String path = Hextractor.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			// En Windows, eliminar la barra inicial si existe (e.g., /C:/Users/... -> C:/Users/...)
			if(path.matches("^/[A-Za-z]:.*")) {
				path = path.substring(1);
			}
			// Convertir barras para Windows
			path = path.replace("/", File.separator);
			return path;
		} catch (Exception e) {
			Utils.logException(e);
			return null;
		}
	}

	/**
	 * Creates the project folder.
	 */
	private static File createProjectFolder(String name) throws IOException {
		File projectFolder = new File(name);
		if(!projectFolder.exists() && !projectFolder.mkdir()) {
			throw new IOException("Error generating: " + name + " directory." );
		}
		return projectFolder;
	}

	/**
	 * Gets the project name.
	 *
	 * @param fileName the file name
	 * @return the project name
	 */
	public static String getProjectName(String fileName) {
		String projectName = fileName;
		// Eliminar solo la extensión conocida, manteniendo el resto del nombre
		for(java.util.Map.Entry<String, java.util.List<String>> entry : Constants.FILE_TYPE_EXTENSIONS.entrySet()) {
			for(String ext : entry.getValue()) {
				if(projectName.toLowerCase().endsWith("." + ext.toLowerCase())) {
					projectName = projectName.substring(0, projectName.length() - ext.length() - 1);
					break;
				}
			}
		}
		// Solo eliminar caracteres absolutamente incompatibles con nombres de carpeta en Windows
		projectName = projectName.replaceAll("[<>:\"/\\\\|?*]", "");
		return projectName;
	}

	/**
	 * Gets the project name (based on the current directory).
	 *
	 * @return the project name
	 */
	public static String getProjectName() {
		try {
			return new File(Constants.CURRENT_DIR).getCanonicalFile().getName();
		} catch (IOException e) {
			Utils.logException(e);
			return Constants.EMPTY;
		}
	}

}
