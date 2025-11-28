package com.wave.hextractor.gui;

import com.wave.hextractor.util.Constants;
import com.wave.hextractor.object.HexTable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HexViewer private mapping methods (ASCII/HEX position conversions).
 */
public class HexViewerMappingTest {

    private HexViewer createViewer() throws Exception {
        // Prepare minimal data: 16 columns * 16 rows bytes (current default)
        int visibleColumns = 16;
        int visibleRows = 16;
        byte[] data = new byte[visibleColumns * visibleRows];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (32 + (i % 64)); // printable range
        }
        Constructor<HexViewer> cons = HexViewer.class.getDeclaredConstructor(byte[].class, String.class, HexTable.class, String.class);
        cons.setAccessible(true);
        return cons.newInstance(data, "test.bin", new HexTable(0), "ascii.tbl");
    }

    private Object invoke(Object target, String name, Class<?>[] paramTypes, Object... params) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, params);
    }

    @Test
    public void testAsciiDocPosToByteIndexBoundaries() throws Exception {
        HexViewer viewer = createViewer();
        int visibleColumns = 16; // default bytes per row
        int visibleRows = 16;    // fixed
        int rowLenWithNl = visibleColumns + 1;
        int fullRows = visibleRows - 1;
        int threshold = rowLenWithNl * fullRows;

        // First char
        int b0 = (int) invoke(viewer, "mapAsciiDocPosToByteIndex", new Class[]{int.class}, 0);
        assertEquals(0, b0);

        // Last char of first row (before newline)
        int b1 = (int) invoke(viewer, "mapAsciiDocPosToByteIndex", new Class[]{int.class}, visibleColumns - 1);
        assertEquals(visibleColumns - 1, b1);

        // Newline position of first row (maps to previous byte)
        int bNl = (int) invoke(viewer, "mapAsciiDocPosToByteIndex", new Class[]{int.class}, visibleColumns);
        assertEquals(visibleColumns - 1, bNl);

        // First char second row
        int docSecondRow = rowLenWithNl; // index after newline
        int b2 = (int) invoke(viewer, "mapAsciiDocPosToByteIndex", new Class[]{int.class}, docSecondRow);
        assertEquals(visibleColumns, b2);

        // Last newline before last row
        int lastFullRowNewlineDocPos = rowLenWithNl * fullRows - 1; // last char of last full row
        int bBeforeLastRow = (int) invoke(viewer, "mapAsciiDocPosToByteIndex", new Class[]{int.class}, lastFullRowNewlineDocPos);
        assertEquals(visibleColumns * fullRows - 1, bBeforeLastRow);

        // Threshold start of last row
        int bStartLastRow = (int) invoke(viewer, "mapAsciiDocPosToByteIndex", new Class[]{int.class}, threshold);
        assertEquals(visibleColumns * fullRows, bStartLastRow);

        // Last char overall
        int lastDocPos = threshold + visibleColumns - 1;
        int bLast = (int) invoke(viewer, "mapAsciiDocPosToByteIndex", new Class[]{int.class}, lastDocPos);
        assertEquals(visibleColumns * fullRows + visibleColumns - 1, bLast);
    }

    @Test
    public void testByteIndexToAsciiDocPosRoundTrip() throws Exception {
        HexViewer viewer = createViewer();
        int visibleColumns = 16;
        int visibleRows = 16;
        int totalBytes = visibleColumns * visibleRows;
        for (int byteIndex = 0; byteIndex < totalBytes; byteIndex += 11) { // sample points
            int docPos = (int) invoke(viewer, "mapByteIndexToAsciiDocPos", new Class[]{int.class}, byteIndex);
            int back = (int) invoke(viewer, "mapAsciiDocPosToByteIndex", new Class[]{int.class}, docPos);
            assertEquals(byteIndex, back, "Round trip failed for byteIndex=" + byteIndex);
        }
    }

    @Test
    public void testByteIndexToHexDocPosPattern() throws Exception {
        HexViewer viewer = createViewer();
        // Adjacent bytes in same row differ by 3 chars
        int pos0 = (int) invoke(viewer, "mapByteIndexToHexDocPos", new Class[]{int.class}, 0);
        int pos1 = (int) invoke(viewer, "mapByteIndexToHexDocPos", new Class[]{int.class}, 1);
        assertEquals(3, pos1 - pos0);

        // First byte of second row should be rowLen + 1 (newline) more than first of first row
        int visibleColumns = 16;
        int rowLen = visibleColumns * 3;
        int posRow0 = (int) invoke(viewer, "mapByteIndexToHexDocPos", new Class[]{int.class}, 0);
        int posRow1 = (int) invoke(viewer, "mapByteIndexToHexDocPos", new Class[]{int.class}, visibleColumns);
        assertEquals(rowLen + 1, posRow1 - posRow0); // +1 for newline
    }
}
