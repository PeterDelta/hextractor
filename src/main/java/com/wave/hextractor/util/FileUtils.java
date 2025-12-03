
package com.wave.hextractor.util;

import com.wave.hextractor.object.HexTable;
import com.wave.hextractor.pojo.FileWithDigests;
import com.wave.hextractor.pojo.OffsetEntry;
import com.wave.hextractor.pojo.TableSearchResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.CRC32;

import static java.lang.String.format;
import static java.lang.String.valueOf;

public class FileUtils {
	// Digest algorithm names
	public static final String MD5_DIGEST = "MD5";
	public static final String SHA1_DIGEST = "SHA-1";

	// Date formatters (dummy, adjust as needed)
	public static final java.time.format.DateTimeFormatter GAME_DATE_DATE_FORMAT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
	public static final java.time.format.DateTimeFormatter GAME_YEAR_DATE_FORMAT = java.time.format.DateTimeFormatter.ofPattern("yyyy");

	// Comma constant for file name parsing
	public static final String COMMA_THE = ", The";

	// Extension to system map (dummy, adjust as needed)
	// public static final Map<String, String> EXTENSION_TO_SYSTEM = new HashMap<>();

	/**
	 * Returns the file extension of a File or String filename.
	 */
	public static String getFileExtension(File file) {
		return getFileExtension(file.getName());
	}
	public static String getFileExtension(String filename) {
		int dot = filename.lastIndexOf('.');
		return (dot >= 0) ? filename.substring(dot + 1) : "";
	}

	/**
	 * Returns the file path (parent directory) of a File or String absolute path.
	 */
	public static String getFilePath(File file) {
		return file.getParent();
	}
	public static String getFilePath(String absolutePath) {
		int idx = absolutePath.lastIndexOf(java.io.File.separator);
		return (idx >= 0) ? absolutePath.substring(0, idx) : "";
	}

	/**
	 * Compara dos archivos binarios y extrae los rangos de diferencias en formato .ext.
	 * Devuelve el número de rangos de diferencia encontrados.
	 * @param originalFile Archivo ROM original
	 * @param modFile Archivo ROM modificado
	 * @param outExtFile Archivo de salida .ext
	 * @return número de rangos de diferencias detectados
	 * @throws IOException Si ocurre un error de lectura/escritura
	 */
	public static int extractDiffAsExt(File originalFile, File modFile, File outExtFile) throws IOException {
		return extractDiffAsExt(originalFile, modFile, outExtFile, true);
	}

	/**
	 * Variante con control de ignorar checksums.
	 */
	public static int extractDiffAsExt(File originalFile, File modFile, File outExtFile, boolean ignoreChecksums) throws IOException {
		final int MAX_GAP = 100; // máximo de bytes sin cambios para seguir el mismo rango
		byte[] orig = Files.readAllBytes(originalFile.toPath());
		byte[] mod = Files.readAllBytes(modFile.toPath());
		int len = Math.min(orig.length, mod.length);

		// Calcular posiciones a ignorar (checksums por sistema)
		boolean[] ignored = ignoreChecksums ? getIgnoredChecksumPositions(originalFile.getName(), orig) : null;

		List<OffsetEntry> diffs = new ArrayList<>();
		int i = 0;
		while (i < len) {
			// Tratar bytes ignorados como si fueran iguales
			boolean isDiff = orig[i] != mod[i] && (ignored == null || !ignored[i]);
			if (isDiff) {
				int start = i;
				int lastDiff = i;
				int gap = 0;
				i++;
				while (i < len) {
					boolean innerDiff = orig[i] != mod[i] && (ignored == null || !ignored[i]);
					if (innerDiff) {
						lastDiff = i;
						gap = 0;
					} else {
						gap++;
						if (gap > MAX_GAP) break;
					}
					i++;
				}
				// El rango va desde start hasta lastDiff
				OffsetEntry entry = new OffsetEntry();
				entry.setStart(start);
				entry.setEnd(lastDiff);
				diffs.add(entry);
			} else {
				i++;
			}
		}
		// Generar el archivo .ext con los rangos en formato ~XX~...#N\n|N\n
		StringBuilder sb = new StringBuilder();
		for (OffsetEntry entry : diffs) {
						sb.append(Constants.ADDR_STR)
							.append(Utils.intToHexString(entry.getStart(), 8))
							.append("-")
							.append(Utils.intToHexString(entry.getEnd(), 8))
							.append(Constants.S_NEWLINE);
			// Línea con bytes originales
			sb.append(";");
			for (int j = entry.getStart(); j <= entry.getEnd(); j++) {
				sb.append("~");
				sb.append(Utils.byteToHexString(orig[j]));
				sb.append("~");
			}
			sb.append(Constants.S_NEWLINE);
			// Línea con bytes modificados
			int count = 0;
			for (int j = entry.getStart(); j <= entry.getEnd(); j++) {
				sb.append("~");
				sb.append(Utils.byteToHexString(mod[j]));
				sb.append("~");
				count++;
			}
			sb.append("#").append(count).append(Constants.S_NEWLINE);
			sb.append("|").append(count).append(Constants.S_NEWLINE);
		}
		writeFileAscii(outExtFile.getAbsolutePath(), sb.toString());
		return diffs.size();
	}

	/**
	 * Devuelve un array de posiciones a ignorar (true) para checksums según el sistema
	 * detectado por la extensión y/o cabecera.
	 */
	private static boolean[] getIgnoredChecksumPositions(String fileName, byte[] fileBytes) {
		int len = fileBytes.length;
		boolean[] ignored = new boolean[len];
		String ext = getFileExtension(fileName);
		if (ext != null) ext = ext.toLowerCase(Locale.ROOT);

		// Mega Drive: 0x18E-0x18F
		if (Constants.FILE_TYPE_EXTENSIONS.get(Constants.FILE_TYPE_MEGADRIVE) != null
				&& Constants.FILE_TYPE_EXTENSIONS.get(Constants.FILE_TYPE_MEGADRIVE).contains(ext)) {
			markIfInRange(ignored, len, SMDChecksumUtils.MEGADRIVE_CHECKSUM_LOCATION);
			markIfInRange(ignored, len, SMDChecksumUtils.MEGADRIVE_CHECKSUM_LOCATION + 1);
		}

		// Game Boy: Header 0x14D, ROM 0x14E-0x14F
		if (Constants.FILE_TYPE_EXTENSIONS.get(Constants.FILE_TYPE_NGB) != null
				&& Constants.FILE_TYPE_EXTENSIONS.get(Constants.FILE_TYPE_NGB).contains(ext)) {
			markIfInRange(ignored, len, 0x14D);
			markIfInRange(ignored, len, 0x14E);
			markIfInRange(ignored, len, 0x14F);
		}

		// SNES: checksum NOT (+44,+45) y checksum (+46,+47) en cabecera interna
		if (Constants.FILE_TYPE_EXTENSIONS.get(Constants.FILE_TYPE_SNES) != null
				&& Constants.FILE_TYPE_EXTENSIONS.get(Constants.FILE_TYPE_SNES).contains(ext)) {
			addSnesIgnoredPositions(ignored, fileBytes);
		}

		// Master System (Overseas): header en 0x7FF0, checksum en +0xA,+0xB
		if (Constants.FILE_TYPE_EXTENSIONS.get(Constants.FILE_TYPE_MASTERSYSTEM) != null
				&& Constants.FILE_TYPE_EXTENSIONS.get(Constants.FILE_TYPE_MASTERSYSTEM).contains(ext)) {
			addSmsIgnoredPositions(ignored, fileBytes);
		}

		return ignored;
	}

	private static void markIfInRange(boolean[] ignored, int len, int pos) {
		if (pos >= 0 && pos < len) {
			ignored[pos] = true;
		}
	}

	/**
	 * Marca posiciones de checksum para SNES teniendo en cuenta LoROM/HiROM y cabecera SMC.
	  */
	private static void addSnesIgnoredPositions(boolean[] ignored, byte[] fileBytes) {
		int len = fileBytes.length;
		final int LOROM_BASE = SNESChecksumUtils.SNES_LOROM_HEADER_OFF;
		final int HIROM_OFFSET = SNESChecksumUtils.SNES_HIROM_OFFSET;
		final int HEADER_LEN = SNESChecksumUtils.SNES_INT_HEADER_LEN;
		final int SMC_SIZE = SNESChecksumUtils.SNES_SMC_HEADER_SIZE;
		final int MBIT = SNESChecksumUtils.SNES_ROM_SIZE_1MBIT;
		final int CHECK_NOT_OFF = SNESChecksumUtils.SNES_CHECKSUMNOT_HEADER_OFF;
		final int CHECK_OFF = SNESChecksumUtils.SNES_CHECKSUM_HEADER_OFF;

		int headerSize = len % MBIT;
		int smcHeader = (headerSize == SMC_SIZE) ? SMC_SIZE : 0;

		int loHeaderStart = LOROM_BASE + smcHeader;
		int hiHeaderStart = LOROM_BASE + HIROM_OFFSET + smcHeader;

		boolean isLoValid = isLikelySnesHeader(Arrays.copyOfRange(fileBytes, loHeaderStart,
				Math.min(loHeaderStart + HEADER_LEN, len)), false);
		boolean isHiValid = false;
		if (!isLoValid && hiHeaderStart + HEADER_LEN <= len) {
			isHiValid = isLikelySnesHeader(Arrays.copyOfRange(fileBytes, hiHeaderStart,
					hiHeaderStart + HEADER_LEN), true);
		}

		int headerStart = isHiValid ? hiHeaderStart : loHeaderStart;
		if (headerStart + HEADER_LEN <= len) {
			markIfInRange(ignored, len, headerStart + CHECK_NOT_OFF);
			markIfInRange(ignored, len, headerStart + CHECK_NOT_OFF + 1);
			markIfInRange(ignored, len, headerStart + CHECK_OFF);
			markIfInRange(ignored, len, headerStart + CHECK_OFF + 1);
		}
	}

	/**
	 * Heurística sencilla para validar cabecera SNES según Lo/HiROM.
	 */
	private static boolean isLikelySnesHeader(byte[] header, boolean expectHiRom) {
		if (header == null || header.length < SNESChecksumUtils.SNES_INT_HEADER_LEN) return false;
		// Nombre ASCII (bytes > 0x1F)
		for (int i = SNESChecksumUtils.SNES_ROMNAME_HEADER_OFF;
			 i < SNESChecksumUtils.SNES_ROMNAME_HEADER_OFF + SNESChecksumUtils.SNES_ROMNAME_HEADER_LENGTH; i++) {
			int v = header[i] & 0xFF;
			if (v <= SNESChecksumUtils.SNES_HEADER_NAME_MIN_CHAR) return false;
		}
		// Map mode bit 1 indica HiROM
		int romType = header[SNESChecksumUtils.SNES_ROMNAME_MAP_MODE_OFF] & 0xFF;
		boolean hiBit = (romType & SNESChecksumUtils.SNES_HIROM_BIT) == SNESChecksumUtils.SNES_HIROM_BIT;
		return expectHiRom ? hiBit : !hiBit;
	}

	/**
	 * Marca posiciones de checksum para Master System (solo ROMs Overseas y cabecera "TMR SEGA").
	 */
	private static void addSmsIgnoredPositions(boolean[] ignored, byte[] fileBytes) {
		int len = fileBytes.length;
		final int HDR_LOC = 0x7FF0;
		final int HDR_SIZE = 0x10;
		if (HDR_LOC + HDR_SIZE > len) return;
		byte[] header = Arrays.copyOfRange(fileBytes, HDR_LOC, HDR_LOC + HDR_SIZE);
		boolean isOverseas = (header[0xF] >> 4) == 0x4 && new String(header, StandardCharsets.US_ASCII).startsWith("TMR SEGA");
		if (isOverseas) {
			markIfInRange(ignored, len, HDR_LOC + 0xA);
			markIfInRange(ignored, len, HDR_LOC + 0xB);
		}
	}

	/**
	 * Extracts Ascii data on packed 3 bytes to 4 characters</br>
	 * using the entries.
	 * @param inputFile .
	 * @param outputFile .
	 * @param entries .
	 * @throws IOException .
	 */
	public static void extractAscii3To4Data(String table, String inputFile, String outputFile, String entries)
			throws IOException {
		Utils.log(Utils.getMessage("consoleExtractingAscii3To4", inputFile, outputFile, entries, table));
		StringBuilder dataString = new StringBuilder();
		byte[] inputFileBytes = Files.readAllBytes(Paths.get(inputFile));
		HexTable hexTable = new HexTable(table);
		for (OffsetEntry entry : Utils.getOffsets(entries)) {
			byte[] origData = Arrays.copyOfRange(inputFileBytes, entry.getStart(), entry.getEnd() + 1);
			byte[] expData = Utils.getExpanded3To4Data(origData);
			byte[] compData = Utils.getCompressed4To3Data(expData);
			if (Utils.isDebug()) {
				Utils.log(Utils.getMessage("consoleOriginalData") + "     " + entry.getHexTarget() + " - "
						+ Utils.getHexArea(0, origData.length, origData));
				Utils.log("Expanded data     " + entry.getHexTarget() + " - "
						+ Utils.getHexArea(0, expData.length, expData));
				Utils.log("Recompressed data " + entry.getHexTarget() + " - "
						+ Utils.getHexArea(0, compData.length, compData));
			}
			if (!Arrays.equals(origData, compData)) {
				Utils.log(Utils.getMessage("consoleErrorRecompressedDataDifferent"));
			}
			String line = hexTable.toAscii(expData, false, true);
			dataString.append(Constants.COMMENT_LINE);
			dataString.append(line);
			dataString.append(Constants.S_NEWLINE);
			dataString.append(line);
			dataString.append(entry.getHexTarget());
			dataString.append(Constants.S_NEWLINE);
		}
		writeFileAscii(outputFile, dataString.toString());
	}

	/**
	 * Inserts ascii as hex from a 4 to 3 data.
	 */
	public static void insertHex4To3Data(String table, String inputFile, String outputFile) throws IOException {
		Utils.log(Utils.getMessage("consoleInsertingAsciiAsHex", inputFile, outputFile, table));
		byte[] outFileBytes = Files.readAllBytes(Paths.get(outputFile));
		HexTable hexTable = new HexTable(table);
		for (String entry : Utils.removeCommentsAndJoin(getAsciiFile(inputFile))) {
			String[] entryDataAndOffset = entry.split(Constants.ADDR_STR);
			OffsetEntry offEntry = OffsetEntry.fromHexRange(entryDataAndOffset[1]);
			byte[] compData = Utils.getCompressed4To3Data(hexTable.toHex(entryDataAndOffset[0]));
			System.arraycopy(compData, 0, outFileBytes, offEntry.getStart(), compData.length);
		}
		Files.write(Paths.get(outputFile), outFileBytes);
	}

	/**
	 * Writes a string as UTF8 in the destination file.
	 * <b>Overwrites the file if exists.</b>
	 *
	 * @param filename file to write.
	 * @param ascii string containing the text.
	 * @throws IOException the exception
	 */
	public static void writeFileAscii(String filename, String ascii) throws IOException {
		try (PrintWriter out = new PrintWriter(filename, Constants.UTF8_ENCODING)) {
			out.print(ascii);
		}
	}

	/**
	 * Insert hex data.
	 *
	 * @param firstFile the first file
	 * @param secondFile the second file
	 * @throws IOException the exception
	 */
	public static void insertHexData(String firstFile, String secondFile) throws IOException {
		Utils.log(Utils.getMessage("consoleInsertingHexFile", firstFile, secondFile));
		byte[] b = Files.readAllBytes(Paths.get(secondFile));
		Utils.loadHex(getAsciiFile(firstFile), b);
		Files.write(Paths.get(secondFile), b);
	}

	/**
	 * Insert ascii as hex.
	 *
	 * @param firstFile the first file
	 * @param secondFile the second file
	 * @param thirdFile the third file
	 * @throws IOException the exception
	 */
	public static void insertAsciiAsHex(String firstFile, String secondFile, String thirdFile) throws IOException {
		Utils.log(Utils.getMessage("consoleInsertingAsciiFile", secondFile, firstFile, thirdFile));
		HexTable hexTable = new HexTable(firstFile);
		String input = getAsciiFile(secondFile);
		byte[] outFileBytes = Files.readAllBytes(Paths.get(thirdFile));
		
		// Try to read original ROM for multi-mapping resolution
		byte[] originalRomBytes = null;
		java.nio.file.Path thirdPath = Paths.get(thirdFile).toAbsolutePath();
		String thirdFileName = thirdPath.getFileName().toString();
		if(thirdFileName.startsWith("TR_")) {
			String originalFileName = thirdFileName.substring(3);
			java.nio.file.Path parentPath = thirdPath.getParent();
			if(parentPath != null) {
				String originalRomPath = parentPath.resolve(originalFileName).toString();
				if(Files.exists(Paths.get(originalRomPath))) {
					originalRomBytes = Files.readAllBytes(Paths.get(originalRomPath));
				}
			}
		}
		
		String[] lines = input.split(Constants.S_NEWLINE);
		int totalBytesWritten = 0;
		int line = 0;
		while ( line < lines.length) {
			if (lines[line] != null && lines[line].contains(Constants.ADDR_STR)) {
				// Read entry
				OffsetEntry entry = new OffsetEntry(lines[line]);
				line++;
				// Read content (including end)
				StringBuilder content = new StringBuilder();
				// Put lines not starting with |
				while (!lines[line].contains(Constants.S_MAX_BYTES)) {
					if (lines[line] != null && lines[line].length() > 0
							&& !lines[line].contains(Constants.S_COMMENT_LINE)) {
						content.append(lines[line]);
						if (lines[line].contains(Constants.S_STR_NUM_CHARS)) {
							content.append(Constants.S_NEWLINE);
						}
					}
					line++;
				}
				// End line
				content.append(lines[line]).append(Constants.S_NEWLINE);

				// Process
				byte[] hex = hexTable.toHex(content.toString(), entry, originalRomBytes);
				if (Utils.isDebug()) {
					Utils.log(" TO OFFSET: " + Utils.intToHexString(entry.getStart(), Constants.HEX_ADDR_SIZE));
				}
				System.arraycopy(hex, 0, outFileBytes, entry.getStart(), hex.length);
				totalBytesWritten += hex.length;
			}
			line++;
		}
		Utils.log(Utils.getMessage("consoleTotalBytesWritten", 
			Utils.fillLeft(valueOf(totalBytesWritten), Constants.HEX_ADDR_SIZE),
			Utils.intToHexString(totalBytesWritten, Constants.HEX_ADDR_SIZE)));
		Files.write(Paths.get(thirdFile), outFileBytes);
	}

	/**
	 * Extracts the ascii from secondFile using table firstFile to thirdFile.
	 *
	 * @param firstFile the first file
	 * @param secondFile the second file
	 * @param thirdFile the third file
	 * @param offsetsArg the offsets arg
	 * @throws IOException the exception
	 */
	public static void extractAsciiFile(String firstFile, String secondFile, String thirdFile, String offsetsArg)
			throws IOException {
		Utils.log(Utils.getMessage("consoleExtractingAsciiFile", secondFile, firstFile, thirdFile));
		extractAsciiFile(new HexTable(firstFile), Files.readAllBytes(Paths.get(secondFile)), thirdFile, offsetsArg,
			true);
	    }
	/**
	 * Returns the ascii file with only Constants.NEWLINE as line separators.
	 * @param filename .
	 * @return .
	 * @throws IOException .
	 */
	public static String getAsciiFile(String filename) throws IOException {
		return String.join(String.valueOf(Constants.NEWLINE), java.nio.file.Files.readAllLines(java.nio.file.Paths.get(filename)));
	}

	/**
	 * Extracts the ascii file.
	 */
	private static void extractAsciiFile(HexTable hexTable, byte[] fileBytes, String outFile, String offsetsArg,
										 boolean showExtractions) throws IOException {
		if (offsetsArg != null && offsetsArg.length() > 0) {
			extractAsciiFile(hexTable, fileBytes, outFile, Utils.getOffsets(offsetsArg), showExtractions);
		}
	}

	/**
	 * Extracts the ascii file.
	 */
	public static void extractAsciiFile(HexTable hexTable, byte[] fileBytes, String outFile, List<OffsetEntry> offsets,
			boolean showExtractions) throws IOException {
		StringBuilder fileOut = new StringBuilder();
		if (offsets != null && !offsets.isEmpty()) {
			for (OffsetEntry entry : offsets) {
				fileOut.append(hexTable.toAscii(fileBytes, entry, showExtractions));
			}
		}
		writeFileAscii(outFile, fileOut.toString());
	}

	/**
	 * Cleans the firstFile extraction data to the secondFile as only
	 * printable chars.
	 * @param firstFile extraction file.
	 * @param secondFile ascii text.
	 * @throws IOException .
	 */
	public static void cleanAsciiFile(String firstFile, String secondFile) throws IOException {
		Utils.log("Cleaning ascii extraction \"" + firstFile + "\" to \"" + secondFile + "\".");
		writeFileAscii(secondFile,
				Utils.getLinesCleaned(getAsciiFile(firstFile).split(Constants.S_NEWLINE)).toString());
	}

	/**
	 * Generates a table file for the input string if found on the rom.
	 *
	 * @param firstFile .
	 * @param outFilePrefix .
	 * @param searchString .
	 * @throws IOException the exception
	 */
	public static void searchRelative8Bits(String firstFile, String outFilePrefix, String searchString)
			throws IOException {
		Utils.log(Utils.getMessage("consoleSearchingRelativeString", searchString, firstFile, outFilePrefix + ".001"));
		List<TableSearchResult> hexTables = searchRelative8Bits(Files.readAllBytes(Paths.get(firstFile)), searchString);
		int tablesFound = 1;
		List<HexTable> usedTables = new ArrayList<>();
		for (TableSearchResult t : hexTables) {
			if (!usedTables.contains(t.getHexTable())) {
				writeFileAscii(outFilePrefix + "." + Utils.fillLeft(valueOf(tablesFound), 3),
						t.getHexTable().toAsciiTable());
				usedTables.add(t.getHexTable());
			}
			tablesFound++;
		}
	}

	/**
	 * Searches tables that meet the letter correlation for the target phrase.
	 *
	 * @param fileBytes the file bytes
	 * @param searchString the search string
	 * @return list of tables.
	 */
	private static List<TableSearchResult> searchRelative8Bits(byte[] fileBytes, String searchString) {
		List<TableSearchResult> res = new ArrayList<>();
		int wordLength = searchString.length();
		if (wordLength < Constants.MIN_SEARCH_WORD_LENGTH) {
			throw new IllegalArgumentException(
					"Minimal word length / Longitud minima de palabra : " + Constants.MIN_SEARCH_WORD_LENGTH);
		}
		byte[] searchBytes = searchString.getBytes(StandardCharsets.US_ASCII);
		int i = 0;
		while (i < fileBytes.length - wordLength) {
			int displacement = searchBytes[0] - fileBytes[i] & Constants.MASK_8BIT;
			if (equivalentChars(displacement, searchBytes, Arrays.copyOfRange(fileBytes, i, i + wordLength))) {
				TableSearchResult tr = new TableSearchResult();
				HexTable ht = new HexTable(displacement);
				tr.setHexTable(ht);
				tr.setOffset(i);
				tr.setWord(searchString);
				if (!res.contains(tr)) {
					res.add(tr);
				}
				i += wordLength - 1;
			}
			if (res.size() > 999) {
				break;
			}
			i++;
		}
		return res;
	}

	/**
	 * Searches relative but * can be expanded to up to expansion number of chars.
	 * @param fileBytes the file bytes
	 * @param searchString the search string
	 * @param expansion number of chars * can represent
	 * @return list of tables.
	 */
	public static List<TableSearchResult> multiSearchRelative8Bits(byte[] fileBytes, String searchString, int expansion) {
		Set<TableSearchResult>  res = new HashSet<>();
		StringBuilder replacement = new StringBuilder();
		if(searchString.contains(Constants.STR_ASTER)) {
			for(int i = 0; i < expansion; i++) {
				replacement.append(Constants.STR_ASTER);
				res.addAll(searchRelative8Bits(fileBytes,
						searchString.replaceAll(Constants.REGEX_STR_ASTER, replacement.toString())));
			}
		}
		else {
			res.addAll(searchRelative8Bits(fileBytes, searchString));
		}
		return new ArrayList<>(res);
	}

	/**
	 * Gets the offsets for the string on the file using the table.
	 * @param fileBytes .
	 * @param hexTable .
	 * @param searchString .
	 * @param ignoreCase .
	 * @return .
	 * @throws IllegalArgumentException .
	 */
	private static List<Integer> findString(byte[] fileBytes, HexTable hexTable, String searchString, boolean ignoreCase) {
		List<Integer> res = new ArrayList<>();
		int wordLength = searchString.length();
		if (ignoreCase) {
			searchString = searchString.toUpperCase();
		}
		if (wordLength < Constants.MIN_SEARCH_WORD_LENGTH) {
			throw new IllegalArgumentException(
					"Minimal word length / Longitud minima de palabra : " + Constants.MIN_SEARCH_WORD_LENGTH);
		}
		int i = 0;
		while (i < fileBytes.length - wordLength) {
			String word = hexTable.toAscii(Arrays.copyOfRange(fileBytes, i, i + wordLength), true);
			if (ignoreCase) {
				word = word.toUpperCase();
			}
			if (areEqual(searchString, wordLength, word)) {
				if (!res.contains(i)) {
					res.add(i);
				}
				i += wordLength - 1;
			}
			if (res.size() > 999) {
				break;
			}
			i++;
		}
		return res;
	}

	private static boolean areEqual(String searchString, int wordLength, String word) {
		boolean areEqual = true;
		for (int j = 0; j < wordLength; j++) {
			if (searchString.charAt(j) != Constants.CHR_ASTER && searchString.charAt(j) != word.charAt(j)) {
				areEqual = false;
				break;
			}
		}
		return areEqual;
	}

	/**
	 * Searches but * can be expanded to up to expansion number of chars.
	 * @param fileBytes .
	 * @param hexTable .
	 * @param searchString .
	 * @param ignoreCase .
	 * @param expansion .
	 * @return .
	 * @throws IllegalArgumentException .
	 */
	public static List<TableSearchResult> multiFindString(byte[] fileBytes, HexTable hexTable, String searchString,
			boolean ignoreCase, int expansion) {
		List<TableSearchResult> res = new ArrayList<>();
		if (searchString.contains(Constants.STR_ASTER)) {
			StringBuilder replacement = new StringBuilder();
			for (int i = 0; i < expansion; i++) {
				replacement.append(Constants.STR_ASTER);
				String searchStrRep = searchString.replaceAll(Constants.REGEX_STR_ASTER, replacement.toString());
				res.addAll(toTableResults(hexTable, searchStrRep,
						findString(fileBytes, hexTable, searchStrRep, ignoreCase)));
			}
		}
		else {
			res.addAll(
					toTableResults(hexTable, searchString, findString(fileBytes, hexTable, searchString, ignoreCase)));
		}
		return new ArrayList<>(res);
	}

	/**
	 * To table results.
	 * @return the list
	 */
	private static List<TableSearchResult> toTableResults(HexTable hexTable, String searchString,
														  List<Integer> list) {
		List<TableSearchResult> searchRes = new ArrayList<>();
		for (Integer res : list) {
			TableSearchResult tsr = new TableSearchResult();
			tsr.setHexTable(hexTable);
			tsr.setOffset(res);
			tsr.setWord(searchString);
			searchRes.add(tsr);
		}
		return searchRes;
	}

	/**
	 * Equivalent chars.
	 *
	 * @param displacement the displacement
	 * @param searchBytes the search bytes
	 * @param fileBytes the file bytes
	 * @return true, if successful
	 */
	private static boolean equivalentChars(int displacement, byte[] searchBytes, byte[] fileBytes) {
		boolean res = true;
		for (int i = 0; i < searchBytes.length; i++) {
			if (searchBytes[i] != Constants.BYTE_ASTER
					&& (searchBytes[i] & Constants.MASK_8BIT) != (fileBytes[i] + displacement & Constants.MASK_8BIT)) {
				res = false;
				break;
			}
		}
		return res;
	}

	/**
	 * Searches all the strings on the rom for the given table</br>
	 * for the default dictionary name (EngDict.txt).
	 *
	 * @param tableFile the table file
	 * @param dataFile the data file
	 * @param numIgnoredChars the num ignored chars
	 * @param endChars the end chars
	 * @throws IOException the exception
	 */
	public static void searchAllStrings(String tableFile, String dataFile, int numIgnoredChars, String endChars)
			throws IOException {
		searchAllStrings(tableFile, dataFile, numIgnoredChars, endChars, Constants.DEFAULT_DICT);
	}

	/**
	 * Searches all the strings on the rom for the given table.
	 *
	 * @param tableFile the table file
	 * @param dataFile the data file
	 * @param numIgnoredChars the num ignored chars
	 * @param endChars the end chars
	 * @param dictFile the dict file
	 * @throws IOException the exception
	 */
	public static void searchAllStrings(String tableFile, String dataFile, int numIgnoredChars, String endChars,
			String dictFile) throws IOException {
		String extractFile = dataFile + Constants.EXTRACT_EXTENSION;
		// Java 21 Text Block
		Utils.log("""
				Extracting all strings from "%s%s%s" and "%s%s"
				 using "%s"
				 numIgnoredChars: %d
				 endChars: %s
				 dictionary: %s""".formatted(
				   dataFile, Constants.FILE_SEPARATOR, extractFile,
				extractFile, Constants.OFFSET_EXTENSION,
				tableFile, numIgnoredChars, endChars, dictFile));
		searchAllStrings(new HexTable(tableFile), Files.readAllBytes(Paths.get(dataFile)), numIgnoredChars, endChars,
				dictFile, dataFile + Constants.EXTRACT_EXTENSION);
	}

	/**
	 * Searches all the strings on the rom for the given table.
	 */
	public static void searchAllStrings(HexTable hexTable, byte[] fileBytes, int numIgnoredChars, String endChars,
										String dictFile, String extractFile) throws IOException {
		String entries = hexTable.getAllEntries(fileBytes,
				Constants.MIN_NUM_CHARS_WORD, numIgnoredChars, Arrays.asList(endChars.toUpperCase()
						.replace(Constants.SPACE_STR, Constants.EMPTY).split(Constants.OFFSET_CHAR_SEPARATOR)),
				dictFile);
		if (entries != null && entries.length() > 0) {
			extractAsciiFile(hexTable, fileBytes, extractFile, entries, false);
		}
	}

	/**
	 * Extracts all the offsets of a given extraction file, useful after cleaning invalid entries of
	 * search all strings.
	 * @param extractFile file to search.
	 * @param extractFileArgs output file.
	 * @throws IOException io error.
	 */
	public static void cleanExtractedFile(String extractFile, String extractFileArgs) throws IOException {
		Utils.log("Getting offsets from \"" + extractFile + Constants.FILE_SEPARATOR + extractFileArgs + "\"");
		writeFileAscii(extractFileArgs, cleanExtractedFile(extractFile));
	}

	/**
	 * Extracts all the offsets of a given extraction file, useful after cleaning invalid entries of
	 * search all strings.
	 * @param extractFile file to search.
	 * @throws IOException io error.
	 */
	public static String cleanExtractedFile(String extractFile) throws IOException {
		Utils.log("Getting offsets from \"" + extractFile);
		StringBuilder fileArgs = new StringBuilder();
		String[] lines = getAsciiFile(extractFile).split(Constants.S_NEWLINE);
		for (String line : lines) {
			line = line.trim();
			if (line.startsWith(Constants.ADDR_STR)) {
				fileArgs.append(line.substring(Constants.ADDR_STR.length()));
				fileArgs.append(Constants.OFFSET_STR_SEPARATOR);
			}
		}
		List<OffsetEntry> entries = Utils.getOffsets(fileArgs.toString());
		Collections.sort(entries);
		fileArgs.setLength(0);
		for (OffsetEntry entry : entries) {
			if (fileArgs.length() > 0) {
				fileArgs.append(Constants.OFFSET_STR_SEPARATOR);
			}
			fileArgs.append(entry.toEntryString());
		}
		return fileArgs.toString();
	}

	/**
	 * Extracts HEX data from the inputFile to the outputFile</br>
	 * using the entries.
	 * @param inputFile .
	 * @param outputFile .
	 * @param entries .
	 * @throws IOException .
	 */
	public static void extractHexData(String inputFile, String outputFile, String entries) throws IOException {
		Utils.log(Utils.getMessage("consoleExtractingHex", inputFile, outputFile, entries));
		StringBuilder hexDataString = new StringBuilder();
		byte[] inputFileBytes = Files.readAllBytes(Paths.get(inputFile));
		for (OffsetEntry entry : Utils.getHexOffsets(entries)) {
			hexDataString.append(entry.getHexComment());
			hexDataString.append(Constants.S_NEWLINE);
			hexDataString.append(entry.getHexString(inputFileBytes));
			hexDataString.append(entry.getHexTarget());
			hexDataString.append(Constants.S_NEWLINE);
		}
		writeFileAscii(outputFile, hexDataString.toString());
	}

	/**
	 * Returns offsets as a unique line.
	 *
	 * @param fileName the file
	 * @return the clean offsets
	 * @throws FileNotFoundException the exception
	 */
	public static String getCleanOffsets(String fileName) throws IOException {
		return getCleanOffsetsString(getAsciiFile(fileName));
	}

	/**
	 * Returns offsets as a unique line from a string.
	 *
	 * @return the clean offsets
	 */
	public static String getCleanOffsetsString(String string) {
		return string.replaceAll(Constants.S_NEWLINE, Constants.EMPTY).replaceAll(Constants.S_CRETURN, Constants.EMPTY);
	}

	/**
	 * Check if the line lengths are ok.
	 *
	 * @param toCheckFile the to check file
	 * @throws FileNotFoundException the exception
	 */
	public static void checkLineLength(String toCheckFile) throws IOException {
		Utils.log("Checking file lines of \"" + toCheckFile);
		Map<String, String> dictionary = Utils.extractDictionary(Files.readAllLines(Paths.get(toCheckFile)));
		dictionary.entrySet().stream().filter(x -> !Utils.checkLineLength(x.getKey(), x.getValue())).forEach(e -> {
			Utils.log(Utils.getMessage("consoleErrorInLines"));
			Utils.log(e.getKey());
			Utils.log(e.getValue());
		});
	}

	/**
	 * Separates the string based on the table entry of the first character,
	 * adds newline after the desired chars.
	 */
	public static void separateCharLength(String file, String table, String outFile) throws IOException {
		Utils.log("Separating string from \"" + file + Constants.FILE_SEPARATOR + outFile + "\"" + "\n using table: \"" + table
				+ "\"");
		writeFileAscii(outFile, separateCharLength(getAsciiFile(file), new HexTable(table)));
	}

	/**
	 * Separates the string based on the table entry of the first character,
	 * adds newline after the desired chars.
	 *
	 * @param text the text
	 * @param table the table
	 * @return the string
	 */
	private static String separateCharLength(String text, HexTable table) {
		StringBuilder res = new StringBuilder();
		int i = 0;
		while(i < text.length()) {
			String lenChar = text.substring(i, i + 1);
			int strLen = table.toHex(lenChar)[0];
			if (strLen == 0) {
				res.append(lenChar);
				i++;
			} else {
				res.append(Constants.S_NEWLINE).append(lenChar).append(Constants.S_NEWLINE);
				res.append(text, i + 1, Math.min(i + 1 + strLen, text.length()));
				i += strLen + 1;
			}
		}
		return res.toString();
	}

	/**
	 * Checks if all files exist.
	 *
	 * @param files the args
	 * @return true, if successful
	 */
	public static boolean allFilesExist(String[] files) {
		return Arrays.stream(files).map(File::new).allMatch(x -> x.exists() && !x.isDirectory());
	}

	/**
	 * Replaces bytes on baseFile starting at offset for the ones on replacementFile.
	 */
	public static void replaceFileData(String baseFile, String replacementFile, Integer offset) throws IOException {
		Utils.log("Replacing bytes on file: '" + baseFile + "' on offset (dec): " + offset + " with file: '" + replacementFile + "'");
		byte[] baseData = Files.readAllBytes(Paths.get(baseFile));
		byte[] replacementData = Files.readAllBytes(Paths.get(replacementFile));
		System.arraycopy(replacementData, 0, baseData, offset, replacementData.length);
		Files.write(Paths.get(baseFile), baseData);
	}

	/**
	 * Outputs the file SHA1, MD5 and CRC32 (in hex), with file name and bytes
	 * FILE
	 * MD5: XXXXXXXXXXXXX
	 * SHA1: XXXXXXXXXXXXX
	 * CRC32: XXXXXXXXXXXXX
	 * XXXXXXX bytes
	 */
	public static void outputFileDigests(String file) throws IOException {
		Utils.log(getFileDigests(getFileWithDigests(file)));
	}
	
	/**
	 * Gets the file SHA1, MD5 and CRC32 (in hex), with file name and bytes
	 * FILE
	 * MD5: XXXXXXXXXXXXX
	 * SHA1: XXXXXXXXXXXXX
	 * CRC32: XXXXXXXXXXXXX
	 * XXXXXXX bytes
	 */
	private static String getFileDigests(FileWithDigests fileWithDigests) {
		StringBuilder fileDigests = new StringBuilder(fileWithDigests.name()).append(System.lineSeparator());
		fileDigests.append("MD5: ").append(fileWithDigests.md5() != null ? fileWithDigests.md5().toUpperCase() : "").append(System.lineSeparator());
		fileDigests.append("SHA1: ").append(fileWithDigests.sha1() != null ? fileWithDigests.sha1().toUpperCase() : "").append(System.lineSeparator());
		fileDigests.append("CRC32: ").append(fileWithDigests.crc32() != null ? fileWithDigests.crc32().toUpperCase() : "").append(System.lineSeparator());
		return fileDigests.toString();
	}

	/**
	 * Returns the file with the digests
	 */
	static FileWithDigests getFileWithDigests(String fileName) throws IOException {
		File file = new File(fileName);
		byte[] bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
		String crc32 = getCrc32Hex(bytes);
		String md5 = getDigestHex(bytes, MD5_DIGEST);
		String sha1 = getDigestHex(bytes, SHA1_DIGEST);
		return new FileWithDigests(file.getName(), bytes, md5, sha1, crc32);
	}

	private static String getCrc32Hex(byte[] bytes) {
		CRC32 crc32 = new CRC32();
		crc32.update(bytes);
		return format("%08X", crc32.getValue()).toLowerCase();
	}

	private static String getDigestHex(byte[] bytes, String digest) {
		String res = "";
		try {
			res = Utils.bytesToHex(MessageDigest.getInstance(digest).digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			Utils.logException(e);
		}
		return res;
	}

	/**
	 * Fills the variables {GAME}, {SYSTEM} and {HASHES} from the file settings based on 
	 * the extension.
	 */
	public static void fillGameData(String emptyDataFile, String filledDataFile, String fileName) throws IOException {
		Utils.log(Utils.getMessage("consoleFillingGameDataFrom", emptyDataFile)); 
		Utils.log(Utils.getMessage("consoleTo") + " \"" + filledDataFile + "\"");
		Utils.log(Utils.getMessage("consoleForFile", fileName));
		String readmeFile = getAsciiFile(emptyDataFile);
		FileWithDigests fileWithDigests = getFileWithDigests(fileName);
		readmeFile = readmeFile.replaceAll("\\{GAME}", getGameName(fileWithDigests.name()));
		readmeFile = readmeFile.replaceAll("\\{SYSTEM}", getGameSystem(fileWithDigests.name()));
		readmeFile = readmeFile.replaceAll("\\{HASHES}", getFileDigests(fileWithDigests));
		readmeFile = readmeFile.replaceAll("\\{DATE}", LocalDate.now().format(GAME_DATE_DATE_FORMAT));
		readmeFile = readmeFile.replaceAll("\\{YEAR}", LocalDate.now().format(GAME_YEAR_DATE_FORMAT));
		writeFileAscii(filledDataFile, readmeFile);
	}

	static String getGameSystem(String fileName) {
		String system = Constants.EXTENSION_TO_SYSTEM.get(getFileExtension(fileName).toLowerCase());
		if(system == null) {
			system = "XXXX";
		}
		return system;
	}

	static String getGameName(String fileName) {
		String cleanFileName = fileName.replaceAll("\\[.*]", "").replaceAll("\\(.*\\)", "");
		int dot = cleanFileName.lastIndexOf('.');
		int cut = cleanFileName.length();
		if(dot > -1 && dot < cut) {
			cut = dot;
		}
		int comma = cleanFileName.indexOf(COMMA_THE);
		if(comma > -1 && comma < cut) {
			cleanFileName = "The " + cleanFileName.substring(0, comma) + cleanFileName.substring(comma + COMMA_THE.length(), cut);
		}
		else {
			cleanFileName = cleanFileName.substring(0, cut);
		}
		return cleanFileName.trim();
	}

	/**
	 * Reads all bytes from a file.
	 *
	 * @param filePath the file path
	 * @return the byte array
	 * @throws IOException if an I/O error occurs
	 */
	public static byte[] readFileBytes(String filePath) throws IOException {
		return Files.readAllBytes(Paths.get(filePath));
	}

}
