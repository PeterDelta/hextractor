package com.wave.hextractor.util;

import com.wave.hextractor.Hextractor;

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
	private static final String PROG_CALL = "java -jar Hextractor.jar ";

	/** The Constant ORIGINALSCRIPT_FILE. */
	private static final String ORIGINALSCRIPT_FILE = "1.extractScript.bat";

	/** The Constant EXTRACTHEX_FILE. */
	private static final String EXTRACTHEX_FILE = "2.extractHex.bat";

	/** The Constant INSERT_FILE. */
	private static final String INSERT_FILE = "3.insertAll.bat";

	/** The Constant CREATEPATCH_FILE. */
	private static final String CREATEPATCH_FILE = "4.createPatch.bat";

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
	private static final String FILE_HEXTRACTOR = "Hextractor.jar";

	/** The Constant FILE_README. */
	private static final String FILE_README = "_readme.txt";

	/** The Constant LOG_GENERATING. */
	private static final String LOG_GENERATING = "Generating / Generando ";

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
	public static void createNewProject(String name, String fileName, String fileType, File projectFile) throws IOException {
		File projectFolder = createProjectFolder(name);
		String transfileName = TR_FILENAME_PREFIX + fileName;
		copyBaseFiles(projectFolder, name, projectFile);
		Utils.createFile(Utils.getJoinedFileName(projectFolder, ORIGINALSCRIPT_FILE), createOriginalScriptFile(name, fileName));
		Utils.createFile(Utils.getJoinedFileName(projectFolder, EXTRACTHEX_FILE), createExtractHexFile(name, transfileName));
		Utils.createFile(Utils.getJoinedFileName(projectFolder, INSERT_FILE), createInsertFile(name, fileName, fileType, transfileName));
		Utils.createFile(Utils.getJoinedFileName(projectFolder, CREATEPATCH_FILE), createCreatePatchFile(name, fileName, transfileName));
	}

	/**
	 * Creates the project.
	 *
	 * @param file the file
	 * @throws IOException the exception
	 */
	public static void createProject(File file) throws IOException {
		ProjectUtils.createNewProject(getProjectName(file.getName()), file.getName(), getFileType(file), file);
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
	 * Creates the original script file.
	 *
	 * @param name the name
	 * @param fileName the file name
	 * @return the string
	 */
	private static String createOriginalScriptFile(String name, String fileName) {
		StringBuilder fileContent = new StringBuilder();
		Utils.log(Utils.getMessage("consoleGeneratingOriginalScript", ORIGINALSCRIPT_FILE));
		fileContent.append(createBatchHeader());
		fileContent.append(getTfileName(fileName)).append(Constants.NEWLINE);
		fileContent.append(getScriptName(name)).append(Constants.NEWLINE);
		fileContent.append(PROG_CALL).append("-a "+ SCRIPTNAME_VAR +".tbl "+ TFILENAMENAME_VAR +" "+ SCRIPTNAME_VAR +".ext "+ SCRIPTNAME_VAR +".off").append(Constants.NEWLINE);
		fileContent.append(createBatchFooter());
		return fileContent.toString();
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
	private static String createInsertFile(String name, String fileName, String fileType, String transfileName) {
		StringBuilder fileContent = new StringBuilder();
		Utils.log(LOG_GENERATING + INSERT_FILE + "...");
		fileContent.append(createBatchHeader());
		fileContent.append(getTfileName(transfileName)).append(Constants.NEWLINE);
		fileContent.append(getSfileName(fileName)).append(Constants.NEWLINE);
		fileContent.append(getScriptName(name)).append(Constants.NEWLINE);
		fileContent.append("del " + TFILENAMENAME_VAR).append(Constants.NEWLINE);
		fileContent.append("copy " + SFILENAMENAME_VAR + " " + TFILENAMENAME_VAR).append(Constants.NEWLINE);
		// Bucle for en una sola línea
		fileContent.append("for %%F in (TR_*.ext) do (java -jar Hextractor.jar -h %SCRIPTNAME%.tbl \"%%F\" %T_FILENAME%)").append(Constants.NEWLINE);
		String checksumMode = getChecksumMode(fileName, fileType);
		if(checksumMode.length() > 0) {
			fileContent.append(PROG_CALL).append(checksumMode).append(" ").append(TFILENAMENAME_VAR).append(Constants.NEWLINE);
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
		Utils.log(LOG_GENERATING + CREATEPATCH_FILE + "...");
		fileContent.append(createBatchHeader());
		fileContent.append(getTfileName(transFileName)).append(Constants.NEWLINE);
		fileContent.append(getSfileName(fileName)).append(Constants.NEWLINE);
		fileContent.append(getScriptName(name)).append(Constants.NEWLINE);
		fileContent.append(PROG_CALL).append(Hextractor.CREATE_IPS_PATCH + " " + SFILENAMENAME_VAR + " " + TFILENAMENAME_VAR + " " + SCRIPTNAME_VAR +".ips").append(Constants.NEWLINE);
		fileContent.append(PROG_CALL).append(Hextractor.MODE_FILL_READ_ME + " ../.Hextractor/_readme.txt " + SCRIPTNAME_VAR + "_readme.txt " + SFILENAMENAME_VAR).append(Constants.NEWLINE);
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
	private static String createExtractHexFile(String name, String transfileName) {
		StringBuilder fileContent = new StringBuilder();
		Utils.log(LOG_GENERATING + EXTRACTHEX_FILE + "...");
		fileContent.append(createBatchHeader());
		fileContent.append(getTfileName(transfileName)).append(Constants.NEWLINE);
		fileContent.append(getScriptName(name)).append(Constants.NEWLINE);
		fileContent.append(createBatchFooter());
		return fileContent.toString();
	}

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
			System.err.println("Warning: No se pudo encontrar " + FILE_HEXTRACTOR + " en " + jarPath);
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
				rb.getString(KeyConstants.KEY_README_STEP5) + Constants.NEWLINE;
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
		// Replace extensions with canonical ones
		for(java.util.Map.Entry<String, java.util.List<String>> entry : Constants.FILE_TYPE_EXTENSIONS.entrySet()) {
			String canonicalExt = getCanonicalExtension(entry.getKey());
			for(String ext : entry.getValue()) {
				if(!"32x".equals(ext) || !Constants.FILE_TYPE_MEGADRIVE.equals(entry.getKey())) {
					projectName = projectName.replace("."+ext, "." + canonicalExt);
				}
			}
		}
		projectName = projectName.replace(" ", "");
		projectName = projectName.replaceAll("(\\(.*\\))", "");
		projectName = projectName.replaceAll("(\\[.*])", "");
		projectName = projectName.replaceAll("[^A-Za-z0-9]", "");
		return projectName.toLowerCase();
	}

	/**
	 * Gets the canonical extension for a file type.
	 *
	 * @param fileType the file type
	 * @return the canonical extension
	 */
	private static String getCanonicalExtension(String fileType) {
		switch(fileType) {
			case Constants.FILE_TYPE_MEGADRIVE: return "smd";
			case Constants.FILE_TYPE_SNES: return "sfc";
			case Constants.FILE_TYPE_NGB: return "gb";
			case Constants.FILE_TYPE_ZXTAP: return "tap";
			case Constants.FILE_TYPE_TZX: return "tzx";
			case Constants.FILE_TYPE_MASTERSYSTEM: return "sms";
			default: return "";
		}
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
