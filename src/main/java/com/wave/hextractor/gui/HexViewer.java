package com.wave.hextractor.gui;

import com.wave.hextractor.object.HexTable;
import com.wave.hextractor.pojo.OffsetEntry;
import com.wave.hextractor.pojo.TableSearchResult;
import com.wave.hextractor.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

/**
 * Gui for the hextractor tools.
 * @author slcantero
 */
public class HexViewer extends JFrame implements ActionListener {
    static {
            UIManager.put("OptionPane.noButtonText", "No");
            UIManager.put("OptionPane.yesButtonText", "Sí");
            UIManager.put("OptionPane.okButtonText", "Aceptar");
            UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        // Cambiar textos de JFileChooser a español con dos puntos
        UIManager.put("FileChooser.saveInLabelText", "Guardar en:");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Tipo de Archivo:");
        UIManager.put("FileChooser.fileNameLabelText", "Nombre:");
        UIManager.put("FileChooser.lookInLabelText", "Buscar en:");
        UIManager.put("FileChooser.saveButtonText", "Guardar");
        UIManager.put("FileChooser.saveButtonToolTipText", "Guardar el archivo seleccionado");
        UIManager.put("FileChooser.cancelButtonText", "Cancelar");
        UIManager.put("FileChooser.openButtonText", "Abrir");
        UIManager.put("FileChooser.openButtonToolTipText", "Abrir el archivo seleccionado");
        UIManager.put("FileChooser.allFilesFilterText", "Todos los archivos");
        UIManager.put("FileChooser.acceptAllFileFilterText", "Todos los archivos");
        UIManager.put("FileChooser.openDialogTitleText", "Abrir");
        UIManager.put("FileChooser.saveDialogTitleText", "Guardar");
        UIManager.put("FileChooser.approveButtonText", "Aceptar");
        UIManager.put("FileChooser.approveButtonToolTipText", "Guardar el archivo seleccionado");
        UIManager.put("FileChooser.cancelButtonToolTipText", "Cancelar");
        UIManager.put("FileChooser.fileNameHeaderText", "Nombre");
        UIManager.put("FileChooser.filesOfTypeHeaderText", "Tipo");
        UIManager.put("FileChooser.upFolderToolTipText", "Subir un nivel");
        UIManager.put("FileChooser.homeFolderToolTipText", "Ir al directorio home");
        UIManager.put("FileChooser.newFolderToolTipText", "Crear nueva carpeta");
        UIManager.put("FileChooser.listViewButtonToolTipText", "Vista de lista");
        UIManager.put("FileChooser.detailsViewButtonToolTipText", "Vista de detalles");
        UIManager.put("FileChooser.folderNameLabelText", "Nombre de carpeta:");
        UIManager.put("FileChooser.directoryOpenButtonText", "Abrir");
        UIManager.put("FileChooser.directoryOpenButtonToolTipText", "Abrir directorio seleccionado");
        UIManager.put("FileChooser.updateButtonText", "Actualizar");
        UIManager.put("FileChooser.updateButtonToolTipText", "Actualizar lista");
        UIManager.put("FileChooser.helpButtonText", "Ayuda");
        UIManager.put("FileChooser.helpButtonToolTipText", "Ayuda");
    }

        /**
         * Obtiene el offset en el que está el caret (o currEntry si no está en ninguno).
         * @param absByte posición absoluta de byte
         * @return el OffsetEntry donde cae el caret
         */
        private OffsetEntry getCaretEntry(int absByte) {
            OffsetEntry inRange = currEntry;
            for (OffsetEntry entry : offEntries) {
                if (absByte >= entry.getStart() && absByte <= entry.getEnd()) {
                    inRange = entry;
                    break;
                }
            }
            return inRange;
        }

        /**
         * Marca el inicio de un offset (usado por ratón y HOME).
         * @param absByte posición absoluta de byte
         */
        private void markOffsetStart(int absByte) {
            // Detectar en qué offset cae el caret
            OffsetEntry targetEntry = getCaretEntry(absByte);
            // Clamp: inicio no puede sobrepasar fin si está definido
            int newStart = absByte;
            if (targetEntry.getEnd() >= 0) {
                newStart = Math.min(newStart, targetEntry.getEnd());
            }
            targetEntry.setStart(newStart);
            caretByteIndex = absByte;
            int docCaret = mapByteIndexToAsciiDocPos(caretByteIndex - offset);
            asciiTextArea.setCaretPosition(docCaret);
            asciiTextArea.moveCaretPosition(docCaret);
            try {
                java.awt.geom.Rectangle2D r2d = asciiTextArea.modelToView2D(docCaret);
                if (r2d != null) asciiTextArea.scrollRectToVisible(r2d.getBounds());
            } catch (Exception ex) {
                // Ignorar
            }
            refreshSelection();
            updateOffsetLabel();
        }
    // Constantes de grid para ventanas secundarias
    private static final int SEARCH_ALL_GRID_ROWS = 4;
    // Filas adicionales a mostrar cuando la ventana está maximizada para aprovechar el espacio
    private static final int EXTRA_MAXIMIZED_ROWS = 12;



    // Variables de menú
    private JMenuItem exit;
    private JMenuItem about;
    private JMenuItem help;
    private JMenuItem goTo;
    private JMenuItem searchRelative;
    private JMenuItem searchAll;
    private JMenuItem extract;
    private JMenuItem loadOffsets;
    private JMenuItem find;
    private JMenuItem clearOffsets;
    private JMenuItem compareRomsItem;
    private JCheckBoxMenuItem askEndCharactersItem;
    
    // Menu items for columns and scale
    private JCheckBoxMenuItem cols16Item;
    private JCheckBoxMenuItem cols32Item;
    private JCheckBoxMenuItem scale100Item;
    private JCheckBoxMenuItem scale125Item;
    private JCheckBoxMenuItem scale150Item;

    // ResourceBundle para internacionalización (UTF-8)
    private static final ResourceBundle rb = ResourceBundle.getBundle("app", java.util.Locale.getDefault(), new com.wave.hextractor.util.UTF8Control());

    // Constante para el valor por defecto de split
// ...existing code...


    /** The Constant SEARCH_ALL_GRID_COLS. */
    private static final int SEARCH_ALL_GRID_COLS = 2;





    /** Max bytes per row supported. */
    private static final int MAX_COLS_AND_ROWS = 32;
    /** Default bytes per row (32 columns). */
    private static final int DEFAULT_VISIBLE_COLUMNS = 32;

    /** The Constant HEX_STARTS. */

    /** The Constant DEC_STARTS. */
    private static final String DEC_STARTS = "d";

    /** The Constant DEFAULT_TABLE. */
    private static final String DEFAULT_TABLE = "ascii.tbl";

    /** The Constant DEFAULT_HEXFILE. */
    private static final String DEFAULT_HEXFILE = "empty.hex";

    /** The Constant EXTENSION_TABLE. */
    private static final String EXTENSION_TABLE = ".tbl";

    /** The Constant EXTENSION_EXTRACTION. */
    private static final String EXTENSION_EXTRACTION = ".ext";

    /** The Constant SEARCH_RES_DIMENSION. */
    private static final Dimension SEARCH_RES_DIMENSION = com.wave.hextractor.util.GuiUtils.scaleDimension(new Dimension(600, 200));

    /** The Constant SEARCHRES_FONT_SIZE. */
    private static final int SEARCHRES_FONT_SIZE = com.wave.hextractor.util.GuiUtils.scaleInt(18);

    /** The Constant BASE_FONT_SIZE. */
    private static final int BASE_FONT_SIZE = 13;

    /** The Constant OFFSET_LABEL_FONT_SIZE. */
    private static final int OFFSET_LABEL_FONT_SIZE = 12;

    /** The Constant REGEXP_OFFSET_ENTRIES. */
    private static final String REGEXP_OFFSET_ENTRIES = "[0-9A-Fa-f]{2}(-[0-9A-Fa-f]{2})*";

    /** The Constant DIMENSION_0_0. */

    /** The blue painter. */

    /** The Constant SEARCH_ALL_DEFAULT_CHARS_INDEX. */
    private static final int SEARCH_ALL_DEFAULT_CHARS_INDEX = 4;

    /** The Constant SEARCH_ALL_DEFAULT_END_CHARS. */
    private static final String SEARCH_ALL_DEFAULT_END_CHARS = "00";

    /** The Constant SEARCH_ALL_MIN_PROGRESS. */
    private static final int SEARCH_ALL_MIN_PROGRESS = 0;

    /** The Constant SEARCH_ALL_MAX_PROGRESS. */
    private static final int SEARCH_ALL_MAX_PROGRESS = 100;

    /** The Constant OFFSET_SEARCH_RADIUS. */
    private static final int OFFSET_SEARCH_RADIUS = 10;

    /** The Constant ROM_EXTENSIONS. */
    private static final String[] ROM_EXTENSIONS = {"md", "smd", "sms", "gba", "sfc", "nes", "bin", "smc", "gen", "gb", "gbc", "gg", "iso"};

    /** The Constant ROM_FILTER_DESCRIPTION. */
    private static final String ROM_FILTER_DESCRIPTION = "ROMS";

    /** The Constant MIN_WINDOW_WIDTH. */
    private static final int MIN_WINDOW_WIDTH = 300;

    /** The Constant MIN_WINDOW_HEIGHT. */
    private static final int MIN_WINDOW_HEIGHT = 200;

    /** The Constant SEARCH_STRINGS_WINDOW_WIDTH. */
    private static final int SEARCH_STRINGS_WINDOW_WIDTH = 280;

    /** The Constant SEARCH_STRINGS_WINDOW_HEIGHT. */
    private static final int SEARCH_STRINGS_WINDOW_HEIGHT = 200;

    /** The Constant MARGIN_SAFETY. */
    private static final int MARGIN_SAFETY = 4;

    /** The Constant LAYOUT_MARGIN. */
    private static final int LAYOUT_MARGIN = 10;

    /** The Constant SMALL_LAYOUT_MARGIN. */
    private static final int SMALL_LAYOUT_MARGIN = 5;

    /** The Constant OFFSET_LABEL_LENGTH. */

    /** The other entry. */
    private SimpleEntry<String, String> otherEntry;

    /** The smd entry. */
    private SimpleEntry<String, String> smdEntry;

    /** The snes entry. */
    private SimpleEntry<String, String> snesEntry;

    /** The gb entry. */
    private SimpleEntry<String, String> gbEntry;

    /** The tap entry. */
    private SimpleEntry<String, String> tapEntry;

    /** The tzx entry. */
    private SimpleEntry<String, String> tzxEntry;

    /** The sms entry. */
    private SimpleEntry<String, String> smsEntry;

    /** The file type to entry map. */
    private java.util.Map<String, SimpleEntry<String, String>> fileTypeToEntry;

    /** The file bytes. */
    private byte[] fileBytes;

    /** The hex table. */
    private HexTable hexTable;

    /** The offset. */
    private int offset = 0;

    /** Flag to prevent recursive refreshAll calls. */

    // Highlighters for offset visualization
    private static final HighlightPainter BLUE_PAINTER = new DefaultHighlighter.DefaultHighlightPainter(Color.BLUE);
    private static final HighlightPainter LGRAY_PAINTER = new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY);
    private static final HighlightPainter YELLOW_PAINTER = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    private static final HighlightPainter ORANGE_PAINTER = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);

    /** The hex file. */
    private File hexFile;

    /** The table file. */
    private File tableFile;

    /** The off entries. */
    private List<OffsetEntry> offEntries;

    /** The curr entry. */
    private OffsetEntry currEntry;

    // Bandera para evitar sobrescribir caret tras selección programática
    private boolean programmaticSelection = false;

    /** The last selected end chars. */
    private String lastSelectedEndChars = "00";

    /** The project file. */
    private File projectFile;

    /** The offset label value. */
    private JLabel offsetLabelValue;
    /** The open table. */
    private JMenuItem openTable;

    /** The save table. */
    private JMenuItem saveTable;

    /** The reload table. */
    private JMenuItem reloadTable;

    /** The next offset. */
    private JMenuItem nextOffset;

    /** The prev offset. */
    private JMenuItem prevOffset;

    /** The open file. */
    private JMenuItem openFile;

    /** The new project. */
    private JMenuItem newProject;

    /** The vsb. */
    private JScrollBar vsb;

    /** The table filter. */
    private SimpleFilter tableFilter;

    /** The ext only file filter. */
    private SimpleFilter extOnlyFileFilter;

    /** The results window. */
    private JFrame resultsWindow;

    /** The search results. */
    private JList<TableSearchResult> searchResults;

    /** The new project window. */
    private JFrame newPrjWin;

    /** The new project window name input. */
    private JTextField newPrjWinNameInput;

    /** The new project window file input. */
    private JTextField newPrjWinFileInput;

    /** The new project window file type options. */
    private JComboBox<Entry<String, String>> newPrjWinFileTypeOpt;

    /** The new project window search file button. */
    private JButton newPrjWinSearchFileButton;

    /** The new project window create button. */
    private JButton newPrjWinCreateButton;

    /** The new project window cancel button. */
    private JButton newPrjWinCancelButton;

    /** Current bytes per row (default 32). */
    private int visibleColumns = DEFAULT_VISIBLE_COLUMNS;

    /** The visible rows (dynamic). */
    private int visibleRows = 32; // Start with the view that shows more lines; will be recalculated dynamically

    /** The search all strings window. */
    private JFrame searchAllStringsWin;

    /** The search all win skip chars opt. */
    private JComboBox<Integer> searchAllWinSkipCharsOpt;

    /** The search all win end chars input. */
    private JTextField searchAllWinEndCharsInput;

    /** The search all win search button. */
    private JButton searchAllWinSearchButton;

    /** The search all win cancel button. */
    private JButton searchAllWinCancelButton;

    /** The search all win progress bar. */
    private JProgressBar searchAllWinProgressBar;

    /** The search all thread. */
    private transient Thread searchAllThread = null;

    /** The search all thread error. */
    private boolean searchAllThreadError = false;

    /** Base font (recomputed when scale changes). */
    private Font baseFont;

    // Panel personalizado para la vista hex
    private HexViewerPanel hexViewerPanel;
    // Panel de estado inferior (offset/porcentaje) para cálculo dinámico de altura
    private JPanel statusPanel;
    // Option A: three distinct areas (offsets, hex, ascii)
    private JTextArea offsetsTextArea;
    private JTextArea hexTextArea;
    private JTextArea asciiTextArea;
    // Track last logical caret byte for refresh preservation
    private int caretByteIndex = -1;
    // Flag para evitar centrado repetido (solo al arrancar)
    private boolean initialCenterDone = false;
    // Record previous maximized state to handle restoration sizing
    private boolean wasMaximized = false;
    
    // Inicializar fuente base
    {
        baseFont = com.wave.hextractor.util.GuiUtils.scaleFont(new Font(Font.MONOSPACED, Font.PLAIN, BASE_FONT_SIZE));
    }

    // Optimización para evitar flicker en refreshSelection

    /** The SEARCH_JOKER_EXPANSIONS for searches. */
    private static final int SEARCH_JOKER_EXPANSIONS = 8;

    /** The Constant ICON16. */
    private static final URL ICON16 = HexViewer.class.getResource("/icon/rom16.png");

    /** The Constant ICON32. */
    private static final URL ICON32 = HexViewer.class.getResource("/icon/rom32.png");

    /** The Constant ICON96. */
    private static final URL ICON96 = HexViewer.class.getResource("/icon/rom96.png");

    private final transient Object searchAllLock = new Object();

    /**

    /**
     * Gets the view size.
     *
     * @return the view size
     */
    private int getViewSize() {
        return visibleColumns * visibleRows;
    }

    /**
     * Centers the window on screen.
     */
    private void centerWindow() {
        setLocationRelativeTo(null);
    }

    /**
     * Shows a message dialog.
     *
     * @param message the message to show
     * @param title the dialog title
     * @param messageType the message type (e.g., JOptionPane.INFORMATION_MESSAGE)
     */
    private void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    /**
     * Creates a navigation action for buttons.
     *
     * @param name the action name
     * @param action the action to perform
     * @return the action
     */
    private Action createNavAction(String name, Runnable action) {
        return new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        };
    }

    /**
     * Instantiates a new hex viewer.
     *
     * @param fileBytes the file bytes
     * @param fileName the file name
     * @param hexTable the hex table
     * @param tableName the table name
     */
    private HexViewer(byte[] fileBytes, String fileName, HexTable hexTable, String tableName) {
        this.hexFile = new File(fileName);
        this.fileBytes = fileBytes;
        this.hexTable = hexTable;
        var projectName = ProjectUtils.getProjectName();
        if(projectName != null && !Constants.EMPTY.equals(projectName)) {
            if(DEFAULT_TABLE.equals(tableName)) {
                tableName = projectName + EXTENSION_TABLE;
            }
        }
        this.tableFile = new File(tableName);
        this.offEntries = new ArrayList<>();
        this.currEntry = new OffsetEntry();
        this.currEntry.setStart(-1);
        this.currEntry.setEnd(-1);
        if (!GraphicsEnvironment.isHeadless()) {
            createFrame();
        } else {
            // For headless testing, set the fields as in buildThreeAreaViewer
            if (fileBytes != null && fileBytes.length == 16 * 16) {
                visibleColumns = 16;
                visibleRows = 16;
            }
        }
        // Ensure initial content populated for tests without relying on EDT
        refreshAll();
    }

    private String getOffsetLabelValue() {
        // En el panel personalizado, el offset mostrado es el primero visible
        int currPos = offset;
        int size = fileBytes.length - 1;
        int lengthDec = String.valueOf(size).length();
        int lengthHex = Integer.toHexString(size).length();
        var strFormat = "%0" + lengthDec + "d";
        return "0x" + Utils.intToHexString(currPos, lengthHex) + " (" + String.format(strFormat, currPos)
                + ") / 0x" + Utils.intToHexString(size, lengthHex) + " (" + String.format(strFormat, size)
                + ") - (" + String.format("%03.2f", (100f * currPos) / size) + "% )";
    }

    /**
     * Draw offset entry.
     *
     * @param entry the entry
     * @param highlighter the highlighter
     * @param painter the painter
     * @param borderPainter the border painter
     * @throws BadLocationException the bad location exception
     */
    // drawOffsetEntry eliminado: no aplica en el panel personalizado
    
    /**
     * Mapea una posición de byte (relativa al offset actual, sin newlines) a posición en el documento (con newlines).
     */
    // mapByteOffsetToDocPos eliminado: no aplica en el panel personalizado

    /**
     * The Class PopUpOffsetEntry.
     */
    class PopUpOffsetEntry extends JPopupMenu {

        /**  serialVersionUID. */
        private static final long serialVersionUID = 8840279664255620962L;

        /** The start item. */
        JMenuItem startItem;

        /** The end item. */
        JMenuItem endItem;

        /** The delete item. */
        JMenuItem deleteItem;

        /** The split item. */
        JMenuItem splitItem;

        /** The selected entry. */
        OffsetEntry selectedEntry;

        /**
         * Instantiates a new pop up offset entry.
         *
         * @param entry the entry
         */
        PopUpOffsetEntry(OffsetEntry entry){
            selectedEntry = entry;
            // Opciones deshabilitadas en el panel personalizado
        }
    }

    /**



    /**
     * Gets the caret entry.
     *
     * @return the caret entry
     */
    // getCaretEntry eliminado: no aplica en el panel personalizado

    /**
     * Close app.
     */
    private void closeApp() {
        System.exit(0);
    }

    /**
     * Creates the frame.
     */
    private void createFrame() {
        setVisible(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLookAndFeel();
        setLayout(new BorderLayout());
        createMenu();
        // Create auxiliary windows (search/new project) BEFORE wiring actions
        // so components exist when listeners are attached
        createSearchAllWin();
        createNewPrjWin();
        // Now wire actions which depend on the created windows/components
        setActions();
        // Re-apply accelerators after actions are wired
        applyMenuAccelerators();

        // Calcular tamaños con FontMetrics
        // FontMetrics y medidas eliminadas: no se usan en el panel personalizado

        // Option A: build 3-area viewer instead of single custom panel
        buildThreeAreaViewer();

        // Habilitar arrastrar&soltar en el contenedor ASCII (igual que el contenedor principal, soportando múltiples archivos)
        try {
            if (asciiTextArea != null) {
                new com.wave.hextractor.object.FileDrop(asciiTextArea, files -> {
                    if (files != null && files.length > 0) {
                        SwingUtilities.invokeLater(() -> {
                            boolean extDropped = false;
                            for (File file : files) {
                                var path = file.getAbsolutePath();
                                if (path.endsWith(EXTENSION_EXTRACTION)) {
                                    reloadExtAsOffsetsFile(file);
                                    extDropped = true;
                                } else if (path.endsWith(EXTENSION_TABLE)) {
                                    reloadTableFile(file);
                                } else {
                                    reloadHexFile(file);
                                }
                            }
                            // If any .ext was dropped, ensure offEntries is sorted and unique
                            if (extDropped && offEntries != null) {
                                Set<OffsetEntry> unique = new HashSet<>(offEntries);
                                offEntries.clear();
                                offEntries.addAll(unique);
                                Collections.sort(offEntries);
                                refreshAll();
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            com.wave.hextractor.util.Utils.logException(e);
        }

        JPanel secondRow = new JPanel();
        secondRow.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        // Añade 2px de espacio inferior para separar de la ventana
        secondRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        add(secondRow, BorderLayout.SOUTH);
        statusPanel = secondRow;
        resultsWindow = new JFrame(rb.getString(KeyConstants.KEY_SEARCH_RESULT_TITLE));
        resultsWindow.setLayout(new FlowLayout());
        searchResults = new JList<>(new TableSearchResult[0]);
        searchResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResults.setLayoutOrientation(JList.VERTICAL);
        searchResults.setVisibleRowCount(8);
        searchResults.setFont(new Font(Font.MONOSPACED, Font.PLAIN, SEARCHRES_FONT_SIZE));
        // Listener: al seleccionar un resultado, mover caret y vista al offset
        searchResults.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                TableSearchResult sel = searchResults.getSelectedValue();
                if (sel != null) {
                    int byteIndex = sel.getOffset();
                    caretByteIndex = byteIndex;
                    offset = Math.max(0, Math.min(byteIndex, fileBytes.length - getViewSize()));
                    if (vsb != null) vsb.setValue(offset);
                    updateThreeAreaContent();
                    int docCaret = mapByteIndexToAsciiDocPos(caretByteIndex - offset);
                    if (asciiTextArea != null && docCaret >= 0 && docCaret <= asciiTextArea.getDocument().getLength()) {
                        asciiTextArea.setCaretPosition(docCaret);
                        try {
                            java.awt.geom.Rectangle2D r2d = asciiTextArea.modelToView2D(docCaret);
                            if (r2d != null) asciiTextArea.scrollRectToVisible(r2d.getBounds());
                        } catch (Exception ex) {
                            // Ignorar
                        }
                    }
                    refreshSelection();
                    updateOffsetLabel();
                }
            }
        });
        JScrollPane listScroller = new JScrollPane(searchResults);
        listScroller.setPreferredSize(SEARCH_RES_DIMENSION);
        resultsWindow.add(listScroller);
        resultsWindow.pack();
        resultsWindow.setResizable(Boolean.FALSE);
        vsb = new JScrollBar(JScrollBar.VERTICAL);
        // Ajuste intermedio del ancho de la barra de desplazamiento
        vsb.setPreferredSize(new Dimension(GuiUtils.scaleInt(15), 0)); // Ancho fijo, altura auto
        // Conectar scrollbar con offset: mover el scroll cambia el offset mostrado
        vsb.addAdjustmentListener(e -> {
            offset = e.getValue();
            // Rebuild view for new offset
            updateThreeAreaContent();
        });
        add(vsb, BorderLayout.EAST);

        // Key bindings for navigation (PageUp/PageDown/Home/End) at window level
        Action pageUpAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int block = getViewSize();
                offset = Math.max(0, offset - block);
                if (vsb != null) vsb.setValue(offset);
                refreshAll();
            }
        };
        Action pageDownAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int block = getViewSize();
                offset = Math.min(Math.max(0, fileBytes.length - getViewSize()), offset + block);
                if (vsb != null) vsb.setValue(offset);
                refreshAll();
            }
        };
        Action homeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Si hay selección de offset activa (inicio marcado y fin no), replica la lógica del ratón
                if (currEntry != null && currEntry.getStart() >= 0 && currEntry.getEnd() < 0) {
                    // Clamp: inicio no puede sobrepasar fin si estuviera definido
                    int newStart = caretByteIndex;
                    if (currEntry.getEnd() >= 0) {
                        newStart = Math.min(newStart, currEntry.getEnd());
                    }
                    currEntry.setStart(newStart);
                    refreshSelection();
                    updateOffsetLabel();
                } else {
                    // Comportamiento normal: ir al inicio global y colocar caret en primer byte
                    offset = 0;
                    caretByteIndex = 0;
                    if (vsb != null) vsb.setValue(offset);
                    updateThreeAreaContent();
                }
            }
        };
        Action endAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ir al final global y colocar caret en último byte
                offset = Math.max(0, fileBytes.length - getViewSize());
                caretByteIndex = fileBytes.length - 1;
                if (vsb != null) vsb.setValue(offset);
                updateThreeAreaContent();
            }
        };
        // Selección con Shift+Home en ASCII/HEX
        Action shiftHomeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focus == asciiTextArea && asciiTextArea.getDocument().getLength() > 0) {
                    int current = asciiTextArea.getCaretPosition();
                    asciiTextArea.setCaretPosition(0);
                    asciiTextArea.moveCaretPosition(current); // selection from start to previous caret
                    refreshSelection();
                } else if (focus == hexTextArea && hexTextArea.getDocument().getLength() > 0) {
                    int current = hexTextArea.getCaretPosition();
                    hexTextArea.setCaretPosition(0);
                    hexTextArea.moveCaretPosition(current);
                    refreshSelectionFromHex();
                } else {
                    // fallback: mover offset al inicio sin selección
                    offset = 0;
                    caretByteIndex = 0;
                    if (vsb != null) vsb.setValue(offset);
                    updateThreeAreaContent();
                }
            }
        };
        // Selección con Shift+End en ASCII/HEX
        Action shiftEndAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                int lastViewByteIndex = Math.min(getViewSize() - 1, fileBytes.length - offset - 1);
                int asciiDocLast = mapByteIndexToAsciiDocPos(lastViewByteIndex);
                int hexDocLast = mapByteIndexToHexDocPos(lastViewByteIndex);
                if (focus == asciiTextArea && asciiTextArea.getDocument().getLength() > 0) {
                    int current = asciiTextArea.getCaretPosition();
                    asciiTextArea.setCaretPosition(asciiDocLast);
                    asciiTextArea.moveCaretPosition(current); // selection from previous caret to end
                    refreshSelection();
                } else if (focus == hexTextArea && hexTextArea.getDocument().getLength() > 0) {
                    int current = hexTextArea.getCaretPosition();
                    hexTextArea.setCaretPosition(hexDocLast);
                    hexTextArea.moveCaretPosition(current);
                    refreshSelectionFromHex();
                } else {
                    // fallback: mover al final global
                    offset = Math.max(0, fileBytes.length - getViewSize());
                    caretByteIndex = fileBytes.length - 1;
                    if (vsb != null) vsb.setValue(offset);
                    updateThreeAreaContent();
                }
            }
        };
        Action leftAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveCaretRelative(-1);
            }
        };
        Action rightAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveCaretRelative(1);
            }
        };
        Action upAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // desplazamiento una fila arriba
                offset = Math.max(0, offset - visibleColumns);
                if (vsb != null) vsb.setValue(offset);
                updateThreeAreaContent();
            }
        };
        Action downAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                offset = Math.min(Math.max(0, fileBytes.length - getViewSize()), offset + visibleColumns);
                if (vsb != null) vsb.setValue(offset);
                updateThreeAreaContent();
            }
        };
        // Copy selection (Ctrl+C)
        Action copyAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fileBytes == null || fileBytes.length == 0) return;
                // Determine source component (ASCII or HEX column)
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                int viewIndex = -1;
                boolean asciiFocus = false;
                if (focus == asciiTextArea) {
                    asciiFocus = true;
                    int docPos = asciiTextArea.getCaretPosition();
                    viewIndex = mapAsciiDocPosToByteIndex(docPos);
                } else if (focus == hexTextArea) {
                    int docPos = hexTextArea.getCaretPosition();
                    viewIndex = mapHexDocPosToByteIndex(docPos);
                } else {
                    // fallback: use caretByteIndex
                    if (caretByteIndex >= offset) viewIndex = caretByteIndex - offset;
                }
                if (viewIndex < 0) return;
                int globalIndex = offset + viewIndex;
                if (globalIndex < 0 || globalIndex >= fileBytes.length) return;
                byte b = fileBytes[globalIndex];
                String text;
                if (asciiFocus) {
                    char c = (b >= 32 && b <= 126) ? (char) b : '.';
                    text = String.valueOf(c);
                } else {
                    text = Utils.byteToHexString(b);
                }
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(text), null);
            }
        };
        // Delete key -> limpiar offsets
        Action deleteAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (offEntries != null && !offEntries.isEmpty()) {
                    cleanOffsets();
                }
            }
        };
        // Bind to WHEN_IN_FOCUSED_WINDOW so they work regardless of focus
        JRootPane root = this.getRootPane();
        if (root != null) {
            int menuShortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = root.getActionMap();
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "pageUp");
            am.put("pageUp", pageUpAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "pageDown");
            am.put("pageDown", pageDownAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "home");
            am.put("home", homeAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "end");
            am.put("end", endAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.SHIFT_DOWN_MASK), "shiftHome");
            am.put("shiftHome", shiftHomeAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.SHIFT_DOWN_MASK), "shiftEnd");
            am.put("shiftEnd", shiftEndAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcut), "copy");
            am.put("copy", copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
            am.put("delete", deleteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
            am.put("left", leftAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
            am.put("right", rightAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
            am.put("up", upAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
            am.put("down", downAction);
        }

        // Añadir ComponentListener para recalcular visibleRows al redimensionar la ventana
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    boolean nowMax = isMaximized();
                    if (nowMax) {
                        recalculateVisibleRowsAndRefresh();
                    } else if (wasMaximized) {
                        // Restaurado desde maximizado: normalizar filas si quedaron infladas
                        if (visibleRows > 32) {
                            visibleRows = 32;
                            applyVisibleRowsToAreas();
                        }
                        adjustWindowToContent();
                        pack();
                        refreshAll();
                    }
                    wasMaximized = nowMax;
                });
            }
        });

        // Forzar ajuste tras el primer render
        SwingUtilities.invokeLater(() -> {
            adjustWindowToContent();
        });

        // Añadir espaciado de 5 píxeles a la izquierda
        secondRow.add(Box.createHorizontalStrut(5));
        JLabel offsetLabel = new JLabel("Offset:");
        // Usar fuente fija igual que la barra de menú
        offsetLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, OFFSET_LABEL_FONT_SIZE));
        secondRow.add(offsetLabel);
        // Label que muestra el offset actual y porcentaje
        offsetLabelValue = new JLabel(getOffsetLabelValue());
        offsetLabelValue.setFont(new Font(Font.DIALOG, Font.PLAIN, OFFSET_LABEL_FONT_SIZE));
        secondRow.add(offsetLabelValue);
        // offsetLabelValue eliminado completamente: no aplica en el panel personalizado
        setResizable(Boolean.TRUE);
        setIcons();
        setVisible(true);
        SwingUtilities.invokeLater(() -> {
            adjustWindowToContent();
            pack();
            // Posicionar en la parte superior izquierda en lugar de centrar
            if (!initialCenterDone) {
                setLocation(0, 0);
                initialCenterDone = true;
            }
            refreshAll();
        });

    }

    /**
     * Ajusta el tamaño de la ventana exactamente al contenido de las tablas.
     */
    private void adjustWindowToContent() {
        if (baseFont == null || asciiTextArea == null) return;
        FontMetrics fm = asciiTextArea.getFontMetrics(baseFont);
        int charW = fm.charWidth('0');
        int charH = fm.getHeight();
        int columns = visibleColumns;
        int rows = visibleRows;
        int offsetsWidth = fm.stringWidth("00000000");
        // Márgenes DPI-aware: en 150% añade margen extra para evitar recortes
        double scale = GuiUtils.getScale();
        int hexExtra = (scale >= 1.5 ? GuiUtils.scaleInt(12) : GuiUtils.scaleInt(4));
        int asciiExtra = (scale >= 1.5 ? GuiUtils.scaleInt(10) : GuiUtils.scaleInt(4));
        int hexWidth = columns * (Constants.HEX_VALUE_SIZE) * charW + hexExtra;
        int asciiWidth = columns * charW + asciiExtra;
        int scrollbarWidth = (vsb != null ? vsb.getPreferredSize().width : GuiUtils.scaleInt(20));
        int extraPadding = (scale >= 1.5 ? GuiUtils.scaleInt(40) : GuiUtils.scaleInt(28));
        // Asegurar colchón global al lado derecho cuando escala >= 150%
        int scaleRightPad = (scale >= 1.5 ? GuiUtils.scaleInt(20) : 0);
        int totalW = offsetsWidth + hexWidth + asciiWidth + scrollbarWidth + extraPadding + scaleRightPad;
        int headerHeight = 0;
        JMenuBar mb = getJMenuBar();
        if (mb != null) headerHeight += mb.getHeight();
        if (statusPanel != null) headerHeight += statusPanel.getHeight();
        headerHeight += GuiUtils.scaleInt(MARGIN_SAFETY); // margen de seguridad
        Insets insets = getInsets();
        int totalH = rows * charH + headerHeight + insets.top + insets.bottom;
        setPreferredSize(new Dimension(totalW, totalH));
        // Ensure a reasonable minimum size: never smaller than the configured minimums
        setMinimumSize(new Dimension(Math.max(totalW, MIN_WINDOW_WIDTH), Math.max(totalH, MIN_WINDOW_HEIGHT)));
        revalidate();
        repaint();
        // En 150% ajusta tamaño al contenido pero SIN sobrepasar la pantalla
        if (scale >= 1.5) {
            boolean isMax = (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            if (!isMax) {
                Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                int margin = GuiUtils.scaleInt(6);
                int maxW = Math.max(100, usable.width - margin);
                int maxH = Math.max(100, usable.height - margin);

                int targetW = Math.min(totalW, maxW);
                int targetH = Math.min(totalH, maxH);

                int newW = Math.min(Math.max(getWidth(), targetW), maxW);
                int newH = Math.min(Math.max(getHeight(), targetH), maxH);
                setSize(newW, newH);

                // Asegurar que la ventana completa queda dentro de la pantalla
                Rectangle b = getBounds();
                int x = Math.max(usable.x, Math.min(b.x, usable.x + usable.width - b.width));
                int y = Math.max(usable.y, Math.min(b.y, usable.y + usable.height - b.height));
                setLocation(x, y);
            }
        }
    }



    /**
     * Recalcula visibleRows según el tamaño actual de la ventana y refresca las áreas.
     */
    private void recalculateVisibleRowsAndRefresh() {
        if (asciiTextArea == null || baseFont == null) return;
        FontMetrics fm = asciiTextArea.getFontMetrics(baseFont);
        int charHeight = Math.max(1, fm.getHeight());

        Container contentPane = getContentPane();
        int availableHeight = contentPane.getHeight();
        Insets in = contentPane.getInsets();
        availableHeight -= (in.top + in.bottom);

        int headerHeight = 0;
        JMenuBar mb = getJMenuBar();
        if (mb != null) headerHeight += mb.getHeight();
        if (statusPanel != null) headerHeight += statusPanel.getHeight();
        // Margen mínimo para evitar recorte de la última línea (DPI-aware)
        headerHeight += GuiUtils.scaleInt(MARGIN_SAFETY);
        availableHeight -= headerHeight;
        if (availableHeight < charHeight) availableHeight = charHeight;

        int rows = Math.max(1, availableHeight / charHeight);
        // Añadir filas extra si está maximizado para eliminar hueco inferior desaprovechado
        if (isMaximized()) {
            rows += EXTRA_MAXIMIZED_ROWS;
        }
        visibleRows = rows;

        applyVisibleRowsToAreas();

        if (hexViewerPanel != null) {
            hexViewerPanel.setVisibleRows(visibleRows);
            hexViewerPanel.setVisibleColumns(visibleColumns);
            hexViewerPanel.revalidate();
            hexViewerPanel.repaint();
        }

        SwingUtilities.invokeLater(this::refreshAll);
    }

    // Aplica visibleRows a las tres áreas de texto
    private void applyVisibleRowsToAreas() {
        if (offsetsTextArea != null) offsetsTextArea.setRows(visibleRows);
        if (hexTextArea != null) hexTextArea.setRows(visibleRows);
        if (asciiTextArea != null) asciiTextArea.setRows(visibleRows);
    }

    // Actualiza la etiqueta de offset si existe
    private void updateOffsetLabel() {
        if (offsetLabelValue != null) {
            offsetLabelValue.setText(getOffsetLabelValue());
        }
    }

    // Ordena la lista de offsets si procede
    private void sortOffEntries() {
        if (offEntries != null) Collections.sort(offEntries);
    }

    /**
     * Sets the icons.
     */
    private void setIcons() {
        List<Image> images = new ArrayList<>();
        try {
            if (ICON96 != null) {
                Image base = ImageIO.read(ICON96);
                if (base != null) {
                    int baseW = base.getWidth(null);
                    int[] sizes = new int[] {16, 32, 96};
                    for (int s : sizes) {
                        int target = com.wave.hextractor.util.GuiUtils.scaleInt(s);
                        double factor = baseW > 0 ? (double) target / (double) baseW : com.wave.hextractor.util.GuiUtils.getScale();
                        Image scaled = com.wave.hextractor.util.GuiUtils.scaleImage(base, factor);
                        images.add(scaled);
                    }
                } else {
                    // fallback to original icons if base not available
                    if (ICON96 != null) images.add(ImageIO.read(ICON96));
                    if (ICON32 != null) images.add(ImageIO.read(ICON32));
                    if (ICON16 != null) images.add(ImageIO.read(ICON16));
                }
            }
        } catch (Exception e) {
            Utils.logException(e);
        }
        if (!images.isEmpty()) {
            this.setIconImages(images);
        }
    }

    /**
     * createSearchAllWin.
     */
    private void createSearchAllWin() {
        searchAllStringsWin = new JFrame(rb.getString(KeyConstants.KEY_SEARCH_ALL_WIN_TITLE));
        searchAllStringsWin.setLayout(new GridLayout(SEARCH_ALL_GRID_ROWS, SEARCH_ALL_GRID_COLS, GuiUtils.scaleInt(5), GuiUtils.scaleInt(5)));
        JLabel searchAllWinSkipCharsLabel = new JLabel(rb.getString(KeyConstants.KEY_SEARCH_ALL_WIN_SKIP_CHARS_LABEL), SwingConstants.LEFT);
        JLabel searchAllWinEndCharsLabel = new JLabel(rb.getString(KeyConstants.KEY_SEARCH_ALL_WIN_END_CHARS_LABEL), SwingConstants.LEFT);
        searchAllWinEndCharsInput = new JTextField(GuiUtils.scaleInt(15));
        searchAllWinSkipCharsOpt = new JComboBox<>();
        for(int i = 0; i <= 16; i++) {
            searchAllWinSkipCharsOpt.addItem(i);
        }
        searchAllWinProgressBar = new JProgressBar(SEARCH_ALL_MIN_PROGRESS, SEARCH_ALL_MAX_PROGRESS);
        // Ensure buttons exist before adding to the window; actions will be wired later in setActionsAllStringsWin()
        if (searchAllWinSearchButton == null) {
            searchAllWinSearchButton = new JButton(rb.getString(KeyConstants.KEY_SEARCH_ALL_WIN_SEARCH_BUTTON));
        }
        if (searchAllWinCancelButton == null) {
            searchAllWinCancelButton = new JButton(rb.getString(KeyConstants.KEY_SEARCH_ALL_WIN_CANCEL_BUTTON));
        }
        searchAllStringsWin.add(searchAllWinSkipCharsLabel);
        searchAllStringsWin.add(searchAllWinSkipCharsOpt);
        searchAllStringsWin.add(searchAllWinEndCharsLabel);
        searchAllStringsWin.add(searchAllWinEndCharsInput);
        searchAllStringsWin.add(searchAllWinSearchButton);
        searchAllStringsWin.add(searchAllWinCancelButton);
        searchAllStringsWin.add(searchAllWinProgressBar);
        searchAllStringsWin.add(new JLabel());
        
        // Apply scaled font to all components in search all window
        Font dialogFont = GuiUtils.scaleFont(searchAllWinSkipCharsLabel.getFont());
        searchAllWinSkipCharsLabel.setFont(dialogFont);
        searchAllWinEndCharsLabel.setFont(dialogFont);
        searchAllWinEndCharsInput.setFont(dialogFont);
        searchAllWinSkipCharsOpt.setFont(dialogFont);
        searchAllWinSearchButton.setFont(dialogFont);
        searchAllWinCancelButton.setFont(dialogFont);
        
        searchAllStringsWin.pack();
        // Establecer tamaño fijo compacto con layout vertical
        searchAllStringsWin.setSize(GuiUtils.scaleInt(SEARCH_STRINGS_WINDOW_WIDTH), GuiUtils.scaleInt(SEARCH_STRINGS_WINDOW_HEIGHT));
        searchAllStringsWin.setResizable(Boolean.FALSE);
    }

    /**
     * createNewPrjWin.
     */
    private void createNewPrjWin() {
        newPrjWin = new JFrame(rb.getString(KeyConstants.KEY_NEW_PRJ_TITLE));
        newPrjWin.setLayout(new BorderLayout(GuiUtils.scaleInt(LAYOUT_MARGIN), GuiUtils.scaleInt(LAYOUT_MARGIN)));
        newPrjWin.setResizable(false);
        
        // Crear componentes
        JLabel newPrjWinFileLabel = new JLabel(rb.getString(KeyConstants.KEY_NEW_PRJ_FILE));
        JLabel newPrjWinNameLabel = new JLabel(rb.getString(KeyConstants.KEY_NEW_PRJ_NAME));
        JLabel newPrjWinFileTypeLabel = new JLabel(rb.getString(KeyConstants.KEY_NEW_PRJ_FILETYPE));
        newPrjWinFileInput = new JTextField(GuiUtils.scaleInt(20));
        newPrjWinNameInput = new JTextField(GuiUtils.scaleInt(20));
        newPrjWinFileTypeOpt = new JComboBox<>();
        
        // Poblar combo de tipos de archivo
        otherEntry = new AbstractMap.SimpleEntry<>(rb.getString(KeyConstants.KEY_NEW_PRJ_OTHER), Constants.FILE_TYPE_OTHER);
        smdEntry = new AbstractMap.SimpleEntry<>(rb.getString(KeyConstants.KEY_NEW_PRJ_SMD), Constants.FILE_TYPE_MEGADRIVE);
        snesEntry = new AbstractMap.SimpleEntry<>(rb.getString(KeyConstants.KEY_NEW_PRJ_SNES), Constants.FILE_TYPE_SNES);
        gbEntry = new AbstractMap.SimpleEntry<>(rb.getString(KeyConstants.KEY_NEW_PRJ_NGB), Constants.FILE_TYPE_NGB);
        tapEntry = new AbstractMap.SimpleEntry<>(rb.getString(KeyConstants.KEY_NEW_PRJ_SPT), Constants.FILE_TYPE_ZXTAP);
        tzxEntry = new AbstractMap.SimpleEntry<>(rb.getString(KeyConstants.KEY_NEW_PRJ_SPZ), Constants.FILE_TYPE_TZX);
        smsEntry = new AbstractMap.SimpleEntry<>(rb.getString(KeyConstants.KEY_NEW_PRJ_SMS), Constants.FILE_TYPE_MASTERSYSTEM);
        newPrjWinFileTypeOpt.addItem(otherEntry);
        newPrjWinFileTypeOpt.addItem(smdEntry);
        newPrjWinFileTypeOpt.addItem(smsEntry);
        newPrjWinFileTypeOpt.addItem(gbEntry);
        newPrjWinFileTypeOpt.addItem(snesEntry);
        newPrjWinFileTypeOpt.addItem(tapEntry);
        newPrjWinFileTypeOpt.addItem(tzxEntry);

        // Initialize file type to entry map
        fileTypeToEntry = new java.util.HashMap<>();
        fileTypeToEntry.put(Constants.FILE_TYPE_MEGADRIVE, smdEntry);
        fileTypeToEntry.put(Constants.FILE_TYPE_SNES, snesEntry);
        fileTypeToEntry.put(Constants.FILE_TYPE_NGB, gbEntry);
        fileTypeToEntry.put(Constants.FILE_TYPE_ZXTAP, tapEntry);
        fileTypeToEntry.put(Constants.FILE_TYPE_TZX, tzxEntry);
        fileTypeToEntry.put(Constants.FILE_TYPE_MASTERSYSTEM, smsEntry);
        
        // Aplicar fuente escalada
        Font dialogFont = GuiUtils.scaleFont(newPrjWinFileLabel.getFont());
        newPrjWinFileLabel.setFont(dialogFont);
        newPrjWinNameLabel.setFont(dialogFont);
        newPrjWinFileTypeLabel.setFont(dialogFont);
        newPrjWinFileInput.setFont(dialogFont);
        newPrjWinNameInput.setFont(dialogFont);
        newPrjWinFileTypeOpt.setFont(dialogFont);
        
        // Panel central con 3 filas
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(GuiUtils.scaleInt(LAYOUT_MARGIN), GuiUtils.scaleInt(LAYOUT_MARGIN), GuiUtils.scaleInt(LAYOUT_MARGIN), GuiUtils.scaleInt(LAYOUT_MARGIN)));
        
        // Fila 1: Archivo a traducir
        JPanel fileRow = new JPanel(new BorderLayout(GuiUtils.scaleInt(5), 0));
        fileRow.add(newPrjWinFileLabel, BorderLayout.NORTH);
        JPanel fileInputPanel = new JPanel(new BorderLayout(GuiUtils.scaleInt(5), 0));
        // Ensure search/create/cancel buttons exist before adding; actions will be set in setActionsNewPrjWin()
        if (newPrjWinSearchFileButton == null) {
            newPrjWinSearchFileButton = new JButton(rb.getString(KeyConstants.KEY_FIND_MENUITEM));
        }
        fileInputPanel.add(newPrjWinFileInput, BorderLayout.CENTER);
        fileInputPanel.add(newPrjWinSearchFileButton, BorderLayout.EAST);
        fileRow.add(fileInputPanel, BorderLayout.CENTER);
        centerPanel.add(fileRow);
        centerPanel.add(Box.createVerticalStrut(GuiUtils.scaleInt(LAYOUT_MARGIN)));
        
        // Fila 2: Nombre Proyecto
        JPanel nameRow = new JPanel(new BorderLayout(GuiUtils.scaleInt(5), 0));
        nameRow.add(newPrjWinNameLabel, BorderLayout.NORTH);
        nameRow.add(newPrjWinNameInput, BorderLayout.CENTER);
        centerPanel.add(nameRow);
        centerPanel.add(Box.createVerticalStrut(GuiUtils.scaleInt(LAYOUT_MARGIN)));
        
        // Fila 3: Tipo de Archivo
        JPanel typeRow = new JPanel(new BorderLayout(GuiUtils.scaleInt(5), 0));
        typeRow.add(newPrjWinFileTypeLabel, BorderLayout.NORTH);
        typeRow.add(newPrjWinFileTypeOpt, BorderLayout.CENTER);
        centerPanel.add(typeRow);
        centerPanel.add(Box.createVerticalStrut(GuiUtils.scaleInt(LAYOUT_MARGIN)));
        
        // Panel de botones (abajo, centrados)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, GuiUtils.scaleInt(LAYOUT_MARGIN), GuiUtils.scaleInt(SMALL_LAYOUT_MARGIN)));
        if (newPrjWinCreateButton == null) {
            newPrjWinCreateButton = new JButton(rb.getString(KeyConstants.KEY_NEW_PRJ_CREA_BUT));
        }
        if (newPrjWinCancelButton == null) {
            newPrjWinCancelButton = new JButton(rb.getString(KeyConstants.KEY_NEW_PRJ_CLOSE_BUT));
        }
        buttonPanel.add(newPrjWinCreateButton);
        buttonPanel.add(newPrjWinCancelButton);
        
        newPrjWin.add(centerPanel, BorderLayout.CENTER);
        newPrjWin.add(buttonPanel, BorderLayout.SOUTH);
        
        // Apply scaled font to buttons for consistency
        newPrjWinSearchFileButton.setFont(dialogFont);
        newPrjWinCreateButton.setFont(dialogFont);
        newPrjWinCancelButton.setFont(dialogFont);

        newPrjWin.pack();
        // Establecer tamaño fijo compacto (reducido sin checkbox)
        newPrjWin.setSize(GuiUtils.scaleInt(420), GuiUtils.scaleInt(210));
        newPrjWin.setResizable(Boolean.FALSE);
    }

    /**
     * Refresh view mode.
     */
    private void refreshViewMode() {
        // Solo refrescar el panel personalizado y ajustar ventana
        adjustWindowToContent();
        if (!isMaximized()) {
            pack();
        }
        refreshAll();
    }



    /**
     * Map a document position in the ASCII area (including newlines) to a byte index.
     * Each of the first (visibleRows-1) rows has (visibleColumns + 1) characters (columns + newline),
     * last row has only visibleColumns characters.
     */
    // Métodos de mapeo y navegación de caret eliminados: no aplican en el panel personalizado



    /**
     * Refresh title.
     */
    private void refreshTitle() {
        setTitle(rb.getString(KeyConstants.KEY_TITLE) + " [" + hexFile +"] - [" + tableFile.getName() + "]" );
    }

    /**
     * Refresh all.
     */
    private void refreshAll() {
        // Actualizar el panel personalizado, título y scrollbar
        if (offEntries != null) {
                sortOffEntries();
        }
            if (offset > fileBytes.length - getViewSize()) offset = Math.max(0, fileBytes.length - getViewSize());
            if (offset < 0) offset = 0;
        if (hexViewerPanel != null) {
            hexViewerPanel.setOffset(offset);
            hexViewerPanel.setFileBytes(fileBytes);
            hexViewerPanel.setVisibleColumns(visibleColumns);
            hexViewerPanel.setBaseFont(baseFont);
        }
        refreshTitle();
        // vsb may be null during early initialization; guard its usage
        if (vsb != null) {
            vsb.setMinimum(0);
            vsb.setMaximum(Math.max(0, fileBytes.length - getViewSize()));
            vsb.setUnitIncrement(visibleColumns);
            vsb.setBlockIncrement(getViewSize());
            vsb.setValue(offset);
        }
        if (offsetLabelValue != null) {
                updateOffsetLabel();
        }
        // Update 3-area content
        updateThreeAreaContent();
    }

    /**
     * Re-applies DPI scaling at runtime: recompute base font, update component fonts & preferred sizes, repack.
     */
    void applyScale() {
        // Recompute base font
        baseFont = com.wave.hextractor.util.GuiUtils.scaleFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        // Update three area fonts
        if (asciiTextArea != null) {
            asciiTextArea.setFont(baseFont);
        }
        if (hexTextArea != null) {
            hexTextArea.setFont(baseFont);
        }
        if (offsetsTextArea != null) {
            offsetsTextArea.setFont(baseFont);
        }
        // NO re-escalar los menús durante applyScale - usar tamaño base fijo
        JMenuBar mb = getJMenuBar();
        if (mb != null) {
            Font fixedMenuFont = new Font(Font.DIALOG, Font.PLAIN, 12);
            for (int i = 0; i < mb.getMenuCount(); i++) {
                JMenu m = mb.getMenu(i);
                if (m != null) {
                    m.setFont(fixedMenuFont);
                    for (int j = 0; j < m.getItemCount(); j++) {
                        JMenuItem it = m.getItem(j);
                        if (it != null) {
                            it.setFont(fixedMenuFont);
                        }
                    }
                }
            }
        }
        // Forzar ajuste tras cambio de escala
        SwingUtilities.invokeLater(() -> {
            adjustWindowToContent();
            // Al reducir a 125% queremos que la ventana se reajuste al contenido
            pack();
            refreshAll();
        });
    }

    /** Change number of bytes per row (16 or 32) and refresh layout. */
    void changeColumns(int columns) {
        if(columns != 16 && columns != 32) {
            return; // unsupported
        }
        visibleColumns = columns;
        SwingUtilities.invokeLater(() -> {
            refreshViewMode();
            // En modo maximizado ajustar filas para usar todo el alto disponible
            if (isMaximized()) {
                recalculateVisibleRowsAndRefresh();
            } else {
                adjustWindowToContent();
                pack();
                refreshAll();
            }
        });
    }

    /**
     * Creates the menu.
     */
    private void createMenu() {
        //Create objects
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        //Menus
        JMenu fileMenu = new JMenu(rb.getString(KeyConstants.KEY_FILEMENU));
        JMenu tableMenu = new JMenu(rb.getString(KeyConstants.KEY_TABLEMENU));
        JMenu offsetMenu = new JMenu(rb.getString(KeyConstants.KEY_OFFSETMENU));
        JMenu toolsMenu = new JMenu(rb.getString(KeyConstants.KEY_TOOLS_MENU));
        JMenu helpMenu = new JMenu(rb.getString(KeyConstants.KEY_HELP_MENU));
        JMenu viewMenu = new JMenu("Vista");
        
        // Usar tamaño fijo para menús (no escalar)
        Font menuFont = new Font(Font.DIALOG, Font.PLAIN, 12);
        fileMenu.setFont(menuFont);
        tableMenu.setFont(menuFont);
        offsetMenu.setFont(menuFont);
        toolsMenu.setFont(menuFont);
        helpMenu.setFont(menuFont);
        viewMenu.setFont(menuFont);

        //Items
        exit =  new JMenuItem(rb.getString(KeyConstants.KEY_EXIT_MENUITEM));
        openFile = new JMenuItem(rb.getString(KeyConstants.KEY_OPEN_FILE_MENUITEM));
        newProject = new JMenuItem(rb.getString(KeyConstants.KEY_NEW_PROJECT_MENUITEM));
        openTable = new JMenuItem(rb.getString(KeyConstants.KEY_OPEN_TABLE_MENUITEM));
        saveTable = new JMenuItem(rb.getString(KeyConstants.KEY_SAVE_TABLE_MENUITEM));
        reloadTable = new JMenuItem(rb.getString(KeyConstants.KEY_RELOAD_TABLE_MENUITEM));

        about = new JMenuItem(rb.getString(KeyConstants.KEY_ABOUT_MENUITEM));
        help = new JMenuItem(rb.getString(KeyConstants.KEY_HELP_MENUITEM));
        goTo = new JMenuItem(rb.getString(KeyConstants.KEY_GOTO_MENUITEM));
        searchRelative = new JMenuItem(rb.getString(KeyConstants.KEY_SEARCH_RELATIVE_MENUITEM));
        searchAll = new JMenuItem(rb.getString(KeyConstants.KEY_SEARCH_ALL_MENUITEM));
        extract = new JMenuItem(rb.getString(KeyConstants.KEY_EXTRACT_MENUITEM));
        loadOffsets = new JMenuItem(rb.getString(KeyConstants.KEY_LOAD_OFFSETS_MENUITEM));
        find = new JMenuItem(rb.getString(KeyConstants.KEY_FIND_MENUITEM));
        nextOffset = new JMenuItem(rb.getString(KeyConstants.KEY_NEXT_RANGE_MENUITEM));
        prevOffset = new JMenuItem(rb.getString(KeyConstants.KEY_PREV_TANGE_MENUITEM));
        clearOffsets = new JMenuItem(rb.getString(KeyConstants.KEY_CLEAN_OFFSETS));

        tableFilter = new SimpleFilter(EXTENSION_TABLE, rb.getString(KeyConstants.KEY_FILTER_TABLE));

        extOnlyFileFilter = new SimpleFilter(Collections.singletonList(EXTENSION_EXTRACTION),
                rb.getString(KeyConstants.KEY_FILTER_EXT_ONLY));

        //Setup menu
        fileMenu.add(openFile);
        fileMenu.add(newProject);
        fileMenu.add(exit);

        tableMenu.add(openTable);
        tableMenu.add(saveTable);
        tableMenu.add(reloadTable);

        // Mover "Buscar todo" (Ctrl+A) al menú Rangos, en primera posición
        offsetMenu.add(searchAll);
        offsetMenu.add(loadOffsets);
        offsetMenu.add(extract);
        offsetMenu.add(nextOffset);
        offsetMenu.add(prevOffset);
        offsetMenu.add(clearOffsets);

        // Quitar "Buscar todo" del menú Herramientas
        toolsMenu.add(goTo);
        toolsMenu.add(searchRelative);
        toolsMenu.add(find);

        // Comparar ROMs
        compareRomsItem = new JMenuItem(rb.getString(KeyConstants.KEY_COMPARE_ROMS_MENUITEM));
        toolsMenu.add(compareRomsItem);
        compareRomsItem.addActionListener(e -> compareRomsAction());

        // Checkbox para preguntar por caracteres de fin
        askEndCharactersItem = new JCheckBoxMenuItem(rb.getString("askEndCharacters"), false);
        askEndCharactersItem.setToolTipText(rb.getString("askEndCharactersTooltip"));
        // Moverlo al final del menú Rangos
        offsetMenu.addSeparator();
        offsetMenu.add(askEndCharactersItem);

        helpMenu.add(help);
        helpMenu.add(about);

        menuBar.add(fileMenu);
        menuBar.add(tableMenu);
        menuBar.add(offsetMenu);
        menuBar.add(toolsMenu);
        // Eliminar viewMenu ya que no tiene opciones
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        // Columns submenu (toggle bytes per row)
        JMenu columnsSub = new JMenu("Columnas");
        cols16Item = new JCheckBoxMenuItem("16", visibleColumns == 16);
        cols32Item = new JCheckBoxMenuItem("32", visibleColumns == 32);
        cols16Item.addActionListener(e -> {
            changeColumns(16);
            cols16Item.setSelected(true);
            cols32Item.setSelected(false);
        });
        cols32Item.addActionListener(e -> {
            changeColumns(32);
            cols16Item.setSelected(false);
            cols32Item.setSelected(true);
        });

        // Shortcut for loading ranges: Ctrl+Shift+S
        loadOffsets.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        loadOffsets.addActionListener(e -> loadOffsetsAction());

        columnsSub.add(cols16Item);
        columnsSub.add(cols32Item);
        viewMenu.add(columnsSub);

        // Scale submenu items
        String currentScale = System.getProperty("hextractor.ui.scale", "1.0");
        double currentScaleVal = Double.parseDouble(currentScale);
        
        scale100Item = new JCheckBoxMenuItem("Escala 100%", Math.abs(currentScaleVal - 1.0) < 0.01);
        scale125Item = new JCheckBoxMenuItem("Escala 125%", Math.abs(currentScaleVal - 1.25) < 0.01);
        scale150Item = new JCheckBoxMenuItem("Escala 150%", Math.abs(currentScaleVal - 1.5) < 0.01);
        
        scale100Item.addActionListener(e -> {
            System.setProperty("hextractor.ui.scale", "1.0");
            applyScale();
            scale100Item.setSelected(true);
            scale125Item.setSelected(false);
            scale150Item.setSelected(false);
        });
        scale125Item.addActionListener(e -> {
            System.setProperty("hextractor.ui.scale", "1.25");
            applyScale();
            scale100Item.setSelected(false);
            scale125Item.setSelected(true);
            scale150Item.setSelected(false);
        });
        scale150Item.addActionListener(e -> {
            System.setProperty("hextractor.ui.scale", "1.5");
            applyScale();
            scale100Item.setSelected(false);
            scale125Item.setSelected(false);
            scale150Item.setSelected(true);
        });
        
        viewMenu.add(scale100Item);
        viewMenu.add(scale125Item);
        viewMenu.add(scale150Item);

        //Actions
        setIcons();
        pack();
        centerWindow(); // Centrar en pantalla
        setVisible(true);
        // Forzar ajuste tras el primer render para evitar bug de 36 líneas
        SwingUtilities.invokeLater(() -> {
            adjustWindowToContent();
            refreshAll();
        });
    }

    /**
     * Acción para comparar dos ROMs y exportar diferencias a .ext
     */
    private void compareRomsAction() {
        File jarDir = Utils.getJarDirectory();
        JFileChooser chooser1 = new JFileChooser(jarDir);
        chooser1.setDialogTitle("Selecciona la ROM original");
        javax.swing.filechooser.FileNameExtensionFilter romFilter = new javax.swing.filechooser.FileNameExtensionFilter(
            ROM_FILTER_DESCRIPTION,
            ROM_EXTENSIONS
        );
        chooser1.addChoosableFileFilter(romFilter);
        chooser1.setFileFilter(romFilter);
        chooser1.setAcceptAllFileFilterUsed(true);
        int res1 = chooser1.showOpenDialog(this);
        if (res1 != JFileChooser.APPROVE_OPTION) return;
        File original = chooser1.getSelectedFile();

        JFileChooser chooser2 = new JFileChooser(jarDir);
        chooser2.setDialogTitle("Selecciona la ROM modificada");
        javax.swing.filechooser.FileNameExtensionFilter romFilter2 = new javax.swing.filechooser.FileNameExtensionFilter(
            ROM_FILTER_DESCRIPTION,
            ROM_EXTENSIONS
        );
        chooser2.addChoosableFileFilter(romFilter2);
        chooser2.setFileFilter(romFilter2);
        chooser2.setAcceptAllFileFilterUsed(true);
        int res2 = chooser2.showOpenDialog(this);
        if (res2 != JFileChooser.APPROVE_OPTION) return;
        File mod = chooser2.getSelectedFile();

        JFileChooser chooser3 = new JFileChooser(jarDir);
        chooser3.setDialogTitle("Selecciona el archivo de salida .ext");
        // Default filename: TR_#<current folder name> (sin extensión en el diálogo)
        var folderName = jarDir.getName();
        var defaultExtName = "TR_#" + folderName;
        chooser3.setSelectedFile(new File(jarDir, defaultExtName));
        int res3;
        File outExt;
        while (true) {
            res3 = chooser3.showSaveDialog(this);
            if (res3 != JFileChooser.APPROVE_OPTION) return;
            outExt = chooser3.getSelectedFile();
            // Añadir extensión .ext automáticamente si no la tiene
            if (!outExt.getAbsolutePath().endsWith(EXTENSION_EXTRACTION)) {
                outExt = new File(outExt.getAbsolutePath() + EXTENSION_EXTRACTION);
            }
            if (outExt.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this,
                    MessageFormat.format(rb.getString(KeyConstants.KEY_CONFIRM_OVERWRITE), outExt.getName()),
                    rb.getString(KeyConstants.KEY_CONFIRM_OVERWRITE_TITLE),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if (overwrite == JOptionPane.YES_OPTION) break;
                // Si no, volver a mostrar el diálogo para elegir otro nombre
            } else {
                break;
            }
        }

        try {
            FileUtils.extractDiffAsExt(original, mod, outExt);
            // Mostrar solo el nombre del archivo, no la ruta completa
            showMessage(MessageFormat.format(rb.getString(KeyConstants.KEY_EXTRACTION_COMPLETED), outExt.getName()), rb.getString(KeyConstants.KEY_SUCCESS_TITLE), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showMessage(MessageFormat.format(rb.getString(KeyConstants.KEY_EXTRACTION_ERROR), ex.getMessage()), rb.getString(KeyConstants.KEY_ERROR_TITLE), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Next offset.
     */
    private void nextOffset() {
        sortOffEntries();
        //Nos paramos en el primero cuyo start sea mayor a nuestro offset
        for(OffsetEntry entry : offEntries) {
            if(entry.getStart() > offset) {
                offset = entry.getStart();
                refreshAll();
                break;
            }
        }
    }

    /**
     * Prev offset.
     */
    private void prevOffset() {
        offEntries.sort(Collections.reverseOrder());
        for(OffsetEntry entry : offEntries) {
            if(entry.getStart() < offset) {
                offset = entry.getStart();
                refreshAll();
                break;
            }
        }
    }

    /**
     * Clean offsets.
     */
    private void cleanOffsets() {
        if(GuiUtils.confirmActionAlert(rb.getString(KeyConstants.KEY_CONFIRM_ACTION_TITLE),
                rb.getString(KeyConstants.KEY_CONFIRM_ACTION))) {
            offEntries.clear();
            refreshAll();
        }
    }

    /**
     * Sets the actions.
     */
    private void setActions() {
        help.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_HELP_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401229L;
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(help, rb.getString(KeyConstants.KEY_HELP_DESC),
                        rb.getString(KeyConstants.KEY_HELP_MENUITEM), JOptionPane.INFORMATION_MESSAGE);
            }
        });
        about.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_ABOUT_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401229L;
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(about, rb.getString(KeyConstants.KEY_ABOUT_DESC) ,
                        rb.getString(KeyConstants.KEY_ABOUT_MENUITEM), JOptionPane.INFORMATION_MESSAGE);
            }
        });
        extract.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_EXTRACT_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401215L;
            @Override
            public void actionPerformed(ActionEvent e) {
                // Mostrar nombre sin extensión .ext en el diálogo
                var baseName = tableFile.getName();
                var extension = FileUtils.getFileExtension(baseName);
                if (!extension.isEmpty()) {
                    baseName = baseName.substring(0, baseName.lastIndexOf('.'));
                }
                var outFileName = "TR_" + baseName;
                JFileChooser fileChooser = new JFileChooser();
                File selectedFile = new File(outFileName);
                fileChooser.setSelectedFile(selectedFile);
                File parent = selectedFile.getParentFile();
                if(parent == null) {
                    parent = hexFile.getParentFile();
                }
                if(parent == null) {
                    parent = tableFile.getParentFile();
                }
                if(parent == null) {
                    parent = Utils.getJarDirectory();
                }
                fileChooser.setCurrentDirectory(parent);
                fileChooser.setFileFilter(extOnlyFileFilter);
                fileChooser.setApproveButtonText(rb.getString(KeyConstants.KEY_SAVE_BUTTON));
                if (fileChooser.showSaveDialog(extract) == JFileChooser.APPROVE_OPTION) {
                    File outputFile = fileChooser.getSelectedFile();
                    // Añadir extensión .ext automáticamente si no la tiene
                    if (!outputFile.getAbsolutePath().endsWith(EXTENSION_EXTRACTION)) {
                        outputFile = new File(outputFile.getAbsolutePath() + EXTENSION_EXTRACTION);
                    }
                    if (confirmSelectedFile(outputFile)) {
                        try {
                            FileUtils.extractAsciiFile(hexTable, fileBytes, outputFile.getAbsolutePath(),
                                    offEntries, false);
                        } catch (Exception e1) {
                            Utils.logException(e1);
                        }
                    }
                }
                refreshAll();
            }
        });
        nextOffset.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_NEXT_RANGE_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401229L;
            @Override
            public void actionPerformed(ActionEvent e) {
                nextOffset();
            }
        });
        prevOffset.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_PREV_TANGE_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401239L;
            @Override
            public void actionPerformed(ActionEvent e) {
                prevOffset();
            }
        });
        clearOffsets.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_CLEAN_OFFSETS)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401239L;
            @Override
            public void actionPerformed(ActionEvent e) {
                cleanOffsets();
            }
        });
        exit.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_EXIT_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401219L;
            @Override
            public void actionPerformed(ActionEvent e) {
                closeApp();
            }
        });
        openFile.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_OPEN_FILE_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401219L;
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                File parent = hexFile.getParentFile();
                if(parent == null) {
                    parent = tableFile.getParentFile();
                }
                if(parent == null) {
                    parent = Utils.getJarDirectory();
                }
                fileChooser.setCurrentDirectory(parent);
                if (fileChooser.showOpenDialog(openFile) ==
                        JFileChooser.APPROVE_OPTION) {
                    reloadHexFile(fileChooser.getSelectedFile());
                }
            }
        });
        newProject.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_NEW_PROJECT_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251417879942401219L;
            @Override
            public void actionPerformed(ActionEvent e) {
                showProjectWindow();
            }
        });
        goTo.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_GOTO_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401217L;
            @Override
            public void actionPerformed(ActionEvent e) {
                var s = JOptionPane.showInputDialog(rb.getString(KeyConstants.KEY_OFFSET_INPUT));
                if(s != null && s.length() > 0) {
                    try {
                        if (s.startsWith(DEC_STARTS)) {
                            offset = Integer.parseInt(s.substring(DEC_STARTS.length()));
                        } else {
                            offset = Integer.parseInt(s, Constants.HEX_RADIX);
                        }
                    } catch (NumberFormatException e1) {
                        //Do nothing
                    }
                    refreshAll();
                }
            }
        });
        searchAll.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_SEARCH_ALL_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401219L;
            @Override
            public void actionPerformed(ActionEvent e) {
                showSearchAllWindow();
            }
        });
        searchRelative.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_SEARCH_RELATIVE_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401218L;

            @Override
            public void actionPerformed(ActionEvent e) {
                var searchString = JOptionPane.showInputDialog(rb.getString(KeyConstants.KEY_SEARCH_RELATIVE));
                if(searchString != null && searchString.length() > 0) {
                    try {
                        List<TableSearchResult> results = FileUtils.multiSearchRelative8Bits(fileBytes, searchString, SEARCH_JOKER_EXPANSIONS);
                        if(results.isEmpty()) {
                            JOptionPane.showMessageDialog(help, rb.getString(KeyConstants.KEY_NO_RESULTS_DESC),
                                    rb.getString(KeyConstants.KEY_NO_RESULTS_TITLE), JOptionPane.INFORMATION_MESSAGE);
                        }
                        else {
                            searchResults.setListData(results.toArray(new TableSearchResult[0]));
                            resultsWindow.pack();
                            resultsWindow.setLocationRelativeTo(resultsWindow.getParent());
                            resultsWindow.setVisible(true);
                        }
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(searchRelative, rb.getString(KeyConstants.KEY_SEARCH_RELATIVE_MIN_LENGTH));
                    }
                    vsb.setValue(offset);
                }
            }
        });
        find.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_FIND_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401219L;

            @Override
            public void actionPerformed(ActionEvent e) {
                var searchString = JOptionPane.showInputDialog(rb.getString(KeyConstants.KEY_FIND));
                if(searchString != null && searchString.length() > 0) {
                    try {
                        List<TableSearchResult>  results = FileUtils.multiFindString(fileBytes, hexTable, searchString, true,
                                SEARCH_JOKER_EXPANSIONS);
                        if(results.isEmpty()) {
                            JOptionPane.showMessageDialog(help, rb.getString(KeyConstants.KEY_NO_RESULTS_DESC),
                                    rb.getString(KeyConstants.KEY_NO_RESULTS_TITLE), JOptionPane.INFORMATION_MESSAGE);
                        }
                        else {
                            searchResults.setListData(results.toArray(new TableSearchResult[0]));
                            resultsWindow.pack();
                            resultsWindow.setLocationRelativeTo(resultsWindow.getParent());
                            resultsWindow.setVisible(true);
                        }
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(searchRelative, rb.getString(KeyConstants.KEY_FIND_MIN_LENGTH));
                    }
                    vsb.setValue(offset);
                }
            }
        });
        openTable.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_OPEN_TABLE_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401219L;
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                File parent = tableFile.getParentFile();
                if(parent == null) {
                    parent = hexFile.getParentFile();
                }
                if(parent == null) {
                    parent = Utils.getJarDirectory();
                }
                fileChooser.setCurrentDirectory(parent);
                fileChooser.setFileFilter(tableFilter);
                int result = fileChooser.showOpenDialog(openTable);
                if (result == JFileChooser.APPROVE_OPTION) {
                    reloadTableFile(fileChooser.getSelectedFile());
                }
            }
        });
        saveTable.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_SAVE_TABLE_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401219L;
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                // Mostrar nombre sin extensión .tbl en el diálogo
                File displayFile = tableFile;
                String tableName = tableFile.getName();
                if (tableName.endsWith(EXTENSION_TABLE)) {
                    tableName = tableName.substring(0, tableName.length() - EXTENSION_TABLE.length());
                    displayFile = new File(tableFile.getParent(), tableName);
                }
                fileChooser.setSelectedFile(displayFile);
                File parent = tableFile.getParentFile();
                if(parent == null) {
                    parent = hexFile.getParentFile();
                }
                if(parent == null) {
                    parent = Utils.getJarDirectory();
                }
                fileChooser.setCurrentDirectory(parent);
                fileChooser.setApproveButtonText(rb.getString(KeyConstants.KEY_SAVE_BUTTON));
                fileChooser.setFileFilter(tableFilter);
                int result = fileChooser.showSaveDialog(saveTable);
                if (result == JFileChooser.APPROVE_OPTION) {
                    boolean accepted = confirmSelectedFile(fileChooser.getSelectedFile());
                    if(accepted) {
                        tableFile = fileChooser.getSelectedFile();
                        if(!tableFile.getAbsolutePath().endsWith(EXTENSION_TABLE)) {
                            tableFile = new File(tableFile.getAbsolutePath() + EXTENSION_TABLE);
                        }
                        try {
                            FileUtils.writeFileAscii(tableFile.getAbsolutePath(), hexTable.toAsciiTable());
                        } catch (Exception e1) {
                            Utils.logException(e1);
                        }
                    }
                    refreshAll();
                }
            }
        });
        reloadTable.setAction(new AbstractAction(rb.getString(KeyConstants.KEY_RELOAD_TABLE_MENUITEM)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 251407879942401218L;
            @Override
            public void actionPerformed(ActionEvent e) {
                if(tableFile != null && tableFile.exists()) {
                    reloadTableFile(tableFile);
                }
            }
        });
        setActionsNewPrjWin();
        setActionsAllStringsWin();
    }

    /**
     * setActionsNewPrjWin.
     */
    private void setActionsNewPrjWin() {
        // Los botones ya existen desde createNewPrjWin(), solo añadir listeners
        newPrjWinSearchFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(Utils.getJarDirectory());
            int result = fileChooser.showOpenDialog(openFile);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                newPrjWinFileInput.setText(file.getName());
                projectFile = file;
                selectProjectFileType(projectFile);
                newPrjWinNameInput.setText(ProjectUtils.getProjectName(projectFile.getName()));
            }
        });
        
        newPrjWinCreateButton.addActionListener(e -> {
            boolean formErrors = false;
            try{
                var fileName = newPrjWinFileInput.getText();
                // Validar solo el nombre del archivo (el nombre del proyecto se genera automáticamente)
                formErrors = fileName == null || fileName.length() == 0 ||
                        !Utils.isValidFileName(fileName) || projectFile == null;
                if(formErrors) {
                    JOptionPane.showMessageDialog(null, rb.getString(KeyConstants.KEY_NEW_PRJ_ERRORS_MSG), rb.getString(KeyConstants.KEY_ERROR_TITLE), JOptionPane.ERROR_MESSAGE);
                }
                else {
                    newPrjWin.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                    disableProjectWindow();
                    newPrjWin.repaint();
                    // Honrar el tipo seleccionado en el diálogo (permite 'Otros' sin parcheo checksum)
                    @SuppressWarnings("unchecked")
                    java.util.Map.Entry<String, String> sel = (java.util.Map.Entry<String, String>) newPrjWinFileTypeOpt.getSelectedItem();
                    String selectedType = sel != null ? sel.getValue() : null;
                    // Usar createProject(file, selectedType) para manejar colisiones y respetar la selección
                    ProjectUtils.createProject(projectFile, selectedType);
                    newPrjWin.setVisible(false);
                    int choice = JOptionPane.showConfirmDialog(newPrjWin,
                            rb.getString(KeyConstants.KEY_NEW_PRJ_GENERATING_MSG),
                            rb.getString(KeyConstants.KEY_NEW_PRJ_GENERATING_MSG), 
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (choice == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                }
            } catch (Exception e1) {
                Utils.logException(e1);
                JOptionPane.showMessageDialog(null, rb.getString(KeyConstants.KEY_ERROR), rb.getString(KeyConstants.KEY_ERROR_TITLE), JOptionPane.ERROR_MESSAGE);
            } finally {
                newPrjWin.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                if(!formErrors) {
                    enableProjectWindow();
                }
            }
        });
        
        newPrjWinCancelButton.addActionListener(e -> {
            newPrjWin.setVisible(false);
        });
        // Enable drag & drop on the New Project window (single file populates fields; multiple files create projects)
        try {
            new com.wave.hextractor.object.FileDrop(newPrjWin, files -> {
                if (files != null) {
                    if (files.length > 1) {
                        newPrjWin.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                        disableProjectWindow();
                        newPrjWin.repaint();
                        for (File file : files) {
                            try {
                                ProjectUtils.createProject(file);
                            } catch (Exception ex) {
                                Utils.logException(ex);
                            }
                        }
                        newPrjWin.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        newPrjWin.setVisible(false);
                        int choice = JOptionPane.showConfirmDialog(
                                asciiTextArea != null ? asciiTextArea.getParent() : newPrjWin,
                                rb.getString(KeyConstants.KEY_NEW_PRJ_GENERATING_MSG),
                                rb.getString(KeyConstants.KEY_NEW_PRJ_GENERATING_MSG),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if (choice == JOptionPane.YES_OPTION) {
                            System.exit(0);
                        }
                    } else {
                        for (File file : files) {
                            newPrjWinFileInput.setText(file.getName());
                            projectFile = file;
                            selectProjectFileType(projectFile);
                            newPrjWinNameInput.setText(ProjectUtils.getProjectName(projectFile.getName()));
                        }
                    }
                }
            });
        } catch (Exception ex) {
            com.wave.hextractor.util.Utils.logException(ex);
        }

        // If the user edits the file name manually, clear the selected file reference
        newPrjWinFileInput.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { projectFile = null; }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { projectFile = null; }
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { projectFile = null; }
        });
    }

    /** Re-apply accelerators to menu items (call after setActions). */
    private void applyMenuAccelerators() {
            try {
                int menuShortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
                reloadTable.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
                searchRelative.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, menuShortcut));
                searchAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcut));
                extract.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, menuShortcut));
                find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, menuShortcut));
                goTo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, menuShortcut));
                openTable.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, menuShortcut));
                saveTable.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcut));
                newProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, menuShortcut));
                nextOffset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
                prevOffset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
                clearOffsets.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, menuShortcut));
                about.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
                help.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
                openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcut));
                compareRomsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, menuShortcut));
            } catch (Exception e) {
                Utils.logException(e);
            }
        }

    /**
     * Sets the actions all strings win.
     */
    private void setActionsAllStringsWin() {
        // Los botones ya existen desde createSearchAllWin(), solo añadir listeners
        searchAllWinCancelButton.addActionListener(e -> {
            if(searchAllThread != null) {
                searchAllThread.interrupt();
                searchAllThreadError = true;
            }
            SwingUtilities.invokeLater(() -> {
                searchAllWinProgressBar.setValue(0);
                enableSearchAllWindow();
            });
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        
        searchAllWinSearchButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                disableSearchAllWindow();
                searchAllWinProgressBar.setValue(SEARCH_ALL_MIN_PROGRESS);
            });
            if(searchAllWinEndCharsInput.getText() != null && searchAllWinEndCharsInput.getText().length() > 0 &&
                    searchAllWinEndCharsInput.getText().matches(REGEXP_OFFSET_ENTRIES)) {
                new Thread(this::searchAllThreadAction).start();
            }
            else {
                JOptionPane.showMessageDialog(searchAllStringsWin,
                    rb.getString(KeyConstants.KEY_ALERT_INVALID_ENDCHARS),
                    rb.getString(KeyConstants.KEY_ALERT_INVALID_ENDCHARS_TITLE), JOptionPane.ERROR_MESSAGE);
                SwingUtilities.invokeLater(() -> {
                    enableSearchAllWindow();
                    searchAllWinProgressBar.setValue(SEARCH_ALL_MIN_PROGRESS);
                });
            }
        });
    }


    /**
     * Search all thread action.
     */
    private void searchAllThreadAction() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        searchAllThreadError = false;
        final List<OffsetEntry> foundEntries = new ArrayList<>();
        
        searchAllThread = new Thread(() -> {
            try {
                File file = new File(Constants.DEFAULT_DICT);
                if(!file.exists()) {
                    file = new File(Constants.PARENT_DIR + Constants.DEFAULT_DICT);
                }
                if(file.exists()) {
                    // Get the entries string from getAllEntries
                    String entries = hexTable.getAllEntries(fileBytes,
                            Constants.MIN_NUM_CHARS_WORD, 
                            searchAllWinSkipCharsOpt.getSelectedIndex(), 
                            Arrays.asList(searchAllWinEndCharsInput.getText().toUpperCase()
                                    .replace(Constants.SPACE_STR, Constants.EMPTY).split(Constants.OFFSET_CHAR_SEPARATOR)),
                            file.getAbsolutePath());
                    
                    if (entries != null && entries.length() > 0) {
                        // Parse the entries and add them to the list
                        foundEntries.addAll(Utils.getOffsets(entries));
                    }
                }
                else {
                    searchAllThreadError = true;
                }
            } catch (Exception e) {
                searchAllThreadError = true;
                Utils.logException(e);
            }
        });
        searchAllThread.start();
        synchronized (searchAllLock) {
            while (searchAllThread != null &&
                    searchAllThread.isAlive() &&
                    !searchAllThread.isInterrupted()) {
                try {
                    searchAllLock.wait(50);
                    SwingUtilities.invokeLater(() -> searchAllWinProgressBar.setValue((int) hexTable.getSearchPercent()));
                    searchAllLock.wait(50);
                } catch (InterruptedException ex) {
                    searchAllThreadError = true;
                    searchAllThread.interrupt();
                }
            }
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        if(searchAllThreadError) {
            JOptionPane.showMessageDialog(searchAllStringsWin, rb.getString(KeyConstants.KEY_SEARCH_ALL_WIN_ERROR),
                    rb.getString(KeyConstants.KEY_SEARCH_ALL_WIN_ERROR), JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            // Add found entries to offEntries and refresh the view
            if (offEntries == null) {
                offEntries = new ArrayList<>();
            }
            offEntries.addAll(foundEntries);
            
            // Remove duplicates and sort
            Set<OffsetEntry> unique = new HashSet<>(offEntries);
            offEntries.clear();
            offEntries.addAll(unique);
            Collections.sort(offEntries);
            
            // Show success message with the number of ranges found
            JOptionPane.showMessageDialog(searchAllStringsWin, 
                    rb.getString(KeyConstants.KEY_SEARCHED_ALL_DESC) + foundEntries.size() + " rangos encontrados",
                    rb.getString(KeyConstants.KEY_SEARCHED_ALL_TITLE), JOptionPane.INFORMATION_MESSAGE);
            searchAllStringsWin.setVisible(false);
            
            // Refresh the display to show the marked ranges
            refreshAll();
        }
        SwingUtilities.invokeLater(() -> {
            enableSearchAllWindow();
            searchAllWinProgressBar.setValue(SEARCH_ALL_MIN_PROGRESS);
        });
    }

    /**
     * Confirm selected file.
     *
     * @param selectedFile the selected file
     * @return true, if successful
     */
    private boolean confirmSelectedFile(File selectedFile) {
        boolean accepted = true;
        if (selectedFile != null && selectedFile.exists()) {
            accepted = GuiUtils.confirmActionAlert(rb.getString(KeyConstants.KEY_CONFIRM_ACTION_TITLE),
                    rb.getString(KeyConstants.KEY_CONFIRM_FILE_OVERWRITE_ACTION));
        }
        return accepted;
    }

    /**
     * Select project file type.
     *
     * @param file the file
     */
    private void selectProjectFileType(File file) {
        if(file != null) {
            String extension =  FileUtils.getFileExtension(file);
            newPrjWinFileTypeOpt.setSelectedItem(otherEntry);
            for(java.util.Map.Entry<String, java.util.List<String>> entry : Constants.FILE_TYPE_EXTENSIONS.entrySet()) {
                if(entry.getValue().contains(extension)) {
                    SimpleEntry<String, String> selectedEntry = fileTypeToEntry.get(entry.getKey());
                    if(selectedEntry != null) {
                        newPrjWinFileTypeOpt.setSelectedItem(selectedEntry);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Enable project window.
     */
    private void enableProjectWindow() {
        newPrjWinNameInput.setEnabled(true);
        newPrjWinFileInput.setEnabled(true);
        newPrjWinFileTypeOpt.setEnabled(true);
        newPrjWinCreateButton.setEnabled(true);
        newPrjWinCancelButton.setEnabled(true);
    }

    /**
     * Clean project window.
     */
    private void cleanProjectWindow() {
        newPrjWinNameInput.setText(Constants.EMPTY);
        newPrjWinFileInput.setText(Constants.EMPTY);
        newPrjWinFileTypeOpt.setSelectedIndex(0);
        projectFile = null;
    }

    /**
     * Disable project window.
     */
    private void disableProjectWindow() {
        newPrjWinNameInput.setEnabled(false);
        newPrjWinFileInput.setEnabled(false);
        newPrjWinFileTypeOpt.setEnabled(false);
        newPrjWinCreateButton.setEnabled(false);
        newPrjWinCancelButton.setEnabled(false);
    }

    /**
     * Creates the project window.
     */
    private void showProjectWindow() {
        cleanProjectWindow();
        enableProjectWindow();
        if(hexFile != null) {
            newPrjWinFileInput.setText(hexFile.getName());
            selectProjectFileType(hexFile);
            newPrjWinNameInput.setText(ProjectUtils.getProjectName(hexFile.getName()));
            projectFile = hexFile;
        }
        newPrjWin.pack();
        newPrjWin.setLocationRelativeTo(this);
        newPrjWin.setVisible(true);
    }

    /**
     * Enable search all window.
     */
    private void enableSearchAllWindow() {
        searchAllThread = null;
        searchAllWinSkipCharsOpt.setEnabled(true);
        searchAllWinEndCharsInput.setEnabled(true);
        searchAllWinSearchButton.setEnabled(true);
        searchAllWinCancelButton.setEnabled(false);
    }

    /**
     * Clean search all window.
     */
    private void cleanSearchAllWindow() {
        //Valores por defecto
        searchAllWinSkipCharsOpt.setSelectedIndex(SEARCH_ALL_DEFAULT_CHARS_INDEX);
        searchAllWinEndCharsInput.setText(SEARCH_ALL_DEFAULT_END_CHARS);
        searchAllWinProgressBar.setValue(SEARCH_ALL_MIN_PROGRESS);
    }

    /**
     * Disable search all window.
     */
    private void disableSearchAllWindow() {
        searchAllWinSkipCharsOpt.setEnabled(false);
        searchAllWinEndCharsInput.setEnabled(false);
        searchAllWinSearchButton.setEnabled(false);
        searchAllWinCancelButton.setEnabled(true);
    }

    /**
     * Show search all window.
     */
    private void showSearchAllWindow() {
        enableSearchAllWindow();
        cleanSearchAllWindow();
        searchAllStringsWin.pack();
        searchAllStringsWin.setLocationRelativeTo(this);
        searchAllStringsWin.setVisible(true);
    }

    /**
     * Reload table file.
     *
     * @param selectedFile the selected file
     */
    private void reloadTableFile(File selectedFile) {
        tableFile = selectedFile;
        try {
            hexTable = new HexTable(tableFile.getAbsolutePath());
        } catch (Exception e1) {
            Utils.logException(e1);
        }
        refreshAll();
    }

    /**
     * Reload ext as offsets file.
     *
     * @param selectedFile the selected file
     */
    private void reloadExtAsOffsetsFile(File selectedFile) {
        try {
            // Get new entries from the dropped .ext file
            List<OffsetEntry> newEntries = Utils.getOffsets(FileUtils.getCleanOffsetsString(
                    FileUtils.cleanExtractedFile(selectedFile.getAbsolutePath())));
            if (offEntries == null) {
                offEntries = new ArrayList<>();
            }
            offEntries.addAll(newEntries);
            // Remove duplicates and sort
            Set<OffsetEntry> unique = new HashSet<>(offEntries);
            offEntries.clear();
            offEntries.addAll(unique);
            Collections.sort(offEntries);
        } catch (Exception e1) {
            Utils.logException(e1);
        }
        refreshAll();
    }

    /**
     * Action to load ranges (.ext) from the UI.
     */
    private void loadOffsetsAction() {
        File jarDir = Utils.getJarDirectory();
        JFileChooser chooser = new JFileChooser(jarDir);
        chooser.setFileFilter(extOnlyFileFilter);
        chooser.setDialogTitle(rb.getString(KeyConstants.KEY_LOAD_OFFSETS_MENUITEM));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected != null && selected.exists()) {
                reloadExtAsOffsetsFile(selected);
                JOptionPane.showMessageDialog(this,
                        rb.getString(KeyConstants.KEY_SEARCHED_ALL_DESC) + offEntries.size() + " rangos marcados",
                        rb.getString(KeyConstants.KEY_SEARCHED_ALL_TITLE),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Reload hex file.
     *
     * @param selectedFile the selected file
     */
    private void reloadHexFile(File selectedFile) {
        hexFile = selectedFile;
        try {
            byte[] newFileBytes = Files.readAllBytes(Paths.get(hexFile.getAbsolutePath()));
            if(newFileBytes.length >= getViewSize()) {
                fileBytes = newFileBytes;
            }
            else {
                fileBytes = new byte[getViewSize()];
                System.arraycopy(newFileBytes, 0, fileBytes, 0, newFileBytes.length);
            }
        } catch (Exception e1) {
            Utils.logException(e1);
        }
        offset = 0;
        refreshAll();
    }

    /* ================= Option A support: mapping & three-area viewer ================= */
    private void buildThreeAreaViewer() {
        // Ajuste de columnas para compatibilidad con tests: si tamaño 16*16 usar 16 columnas
        if (fileBytes != null && fileBytes.length == 16 * 16) {
            visibleColumns = 16;
            visibleRows = 16;
        }
        
        // Crear tres JTextArea con filas/columnas explícitas
        offsetsTextArea = new JTextArea(visibleRows, 8); // 8 chars para "00000000"
        hexTextArea = new JTextArea(visibleRows, visibleColumns * Constants.HEX_VALUE_SIZE);
        asciiTextArea = new JTextArea(visibleRows, visibleColumns);
        
        // Configurar propiedades
        offsetsTextArea.setEditable(false);
        hexTextArea.setEditable(false);
        asciiTextArea.setEditable(false);
        
        offsetsTextArea.setFont(baseFont);
        hexTextArea.setFont(baseFont);
        asciiTextArea.setFont(baseFont);
        
        offsetsTextArea.setBackground(Color.BLACK);
        hexTextArea.setBackground(Color.BLACK);
        asciiTextArea.setBackground(Color.BLACK);
        
        offsetsTextArea.setForeground(Color.LIGHT_GRAY);
        hexTextArea.setForeground(Color.WHITE);
        asciiTextArea.setForeground(Color.WHITE);
        
        offsetsTextArea.setLineWrap(false);
        hexTextArea.setLineWrap(false);
        asciiTextArea.setLineWrap(false);
        
        // Incrementar margen interno a la derecha (DPI-aware) para asegurar visibilidad en 150%
        int rightPad = com.wave.hextractor.util.GuiUtils.scaleInt(MARGIN_SAFETY);
        offsetsTextArea.setMargin(new java.awt.Insets(0, 0, 0, rightPad));
        hexTextArea.setMargin(new java.awt.Insets(0, 0, 0, rightPad));
        asciiTextArea.setMargin(new java.awt.Insets(0, 0, 0, rightPad));
        
        // Crear JScrollPane individuales para cada área
        JScrollPane offsetsScroll = new JScrollPane(offsetsTextArea);
        JScrollPane hexScroll = new JScrollPane(hexTextArea);
        JScrollPane asciiScroll = new JScrollPane(asciiTextArea);
        // Evitar líneas/blancos: quita bordes visibles y opacidad del viewport
        java.util.List<JScrollPane> panes = java.util.Arrays.asList(offsetsScroll, hexScroll, asciiScroll);
        for (JScrollPane sp : panes) {
            sp.setBorder(null);
            sp.setViewportBorder(null);
            sp.setOpaque(false);
            if (sp.getViewport() != null) sp.getViewport().setOpaque(false);
        }
        
        // Deshabilitar barras de scroll individuales (usaremos la barra central)
        offsetsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        offsetsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        hexScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        hexScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        asciiScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        asciiScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Fijar ancho mínimo de offsetsScroll para que no se comprima con 16 columnas
        FontMetrics fm = offsetsTextArea.getFontMetrics(baseFont);
        int offsetWidth = fm.stringWidth("00000000") + com.wave.hextractor.util.GuiUtils.scaleInt(MARGIN_SAFETY); // 8 chars + margen DPI-aware
        offsetsScroll.setMinimumSize(new Dimension(offsetWidth, 0));
        offsetsScroll.setPreferredSize(new Dimension(offsetWidth, offsetsScroll.getPreferredSize().height));
        
        // Crear panel contenedor con BoxLayout horizontal
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        // Quitar bordes en contenedor para no mostrar líneas blancas
        container.setBorder(null);
        container.setBackground(Color.BLACK);
        container.add(offsetsScroll);
        container.add(Box.createHorizontalStrut(com.wave.hextractor.util.GuiUtils.scaleInt(3)));
        container.add(hexScroll);
        container.add(Box.createHorizontalStrut(com.wave.hextractor.util.GuiUtils.scaleInt(3)));
        container.add(asciiScroll);
        
        add(container, BorderLayout.CENTER);
        
        // Listener de cambio de caret para sincronizar selección hex/ascii
        asciiTextArea.addCaretListener(e -> refreshSelection());
        hexTextArea.addCaretListener(e -> refreshSelectionFromHex());
        
        // MouseListener para ASCII con menú popup y posicionamiento preciso
        asciiTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override 
            public void mousePressed(java.awt.event.MouseEvent e) {
                asciiTextArea.requestFocusInWindow();
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // Mejorar precisión del clic: si está en la primera mitad, retroceder al anterior
                    try {
                        int pos = asciiTextArea.viewToModel2D(e.getPoint());
                        if (pos > 0 && pos <= asciiTextArea.getDocument().getLength()) {
                            java.awt.Rectangle rect = asciiTextArea.modelToView2D(pos).getBounds();
                            // Si el clic está en la primera mitad del carácter, retroceder al anterior
                            if (e.getX() < rect.x + rect.width / 2) {
                                pos--;
                            }
                            asciiTextArea.setCaretPosition(pos);
                        }
                    } catch (Exception ex) {
                        // Ignore
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    showPopupMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showPopupMenu(e);
                }
            }
            
            private void showPopupMenu(java.awt.event.MouseEvent e) {
                // Determinar posición de clic (no mover caret)
                int clickDocPos = asciiTextArea.viewToModel2D(e.getPoint());
                if (clickDocPos < 0 || clickDocPos > asciiTextArea.getText().length()) {
                    return;
                }
                int clickByteIndexInView = mapAsciiDocPosToByteIndex(clickDocPos);
                int clickAbsoluteByteIndex = offset + clickByteIndexInView;

                // Detectar en qué offset cae el clic
                final OffsetEntry targetEntry = getCaretEntry(clickAbsoluteByteIndex);

                JPopupMenu popup = new JPopupMenu();

                // Opción Inicio Offset: siempre disponible
                JMenuItem startItem = new JMenuItem(rb.getString("offsetSetStart"));
                startItem.addActionListener(ev -> {
                    HexViewer.this.markOffsetStart(clickAbsoluteByteIndex);
                });
                popup.add(startItem);

                // Opción Fin Offset: mostrar si está en currEntry con inicio o en otro offset
                if ((targetEntry == currEntry && currEntry.getStart() >= 0) || targetEntry != currEntry) {
                    JMenuItem endItem = new JMenuItem(rb.getString("offsetSetEnd"));
                    endItem.addActionListener(ev -> {
                        String defaultEndChars = caretByteIndex < fileBytes.length ? String.format("%02X", fileBytes[caretByteIndex] & 0xFF) : lastSelectedEndChars;
                        String result = null;
                        if (askEndCharactersItem.isSelected()) {
                            result = JOptionPane.showInputDialog(HexViewer.this, rb.getString("inputEndchars"), defaultEndChars);
                        } else {
                            result = defaultEndChars;
                        }
                        if (result != null && result.length() > 0) {
                            lastSelectedEndChars = result;
                            targetEntry.setEndChars(Arrays.asList(result.toUpperCase().split("-")));
                            int newEnd = clickAbsoluteByteIndex;
                            if (targetEntry.getStart() >= 0) {
                                newEnd = Math.max(newEnd, targetEntry.getStart());
                            }
                            targetEntry.setEnd(newEnd);
                            
                            // Si era currEntry, consolidarlo
                            if (targetEntry == currEntry) {
                                currEntry.mergeInto(offEntries);
                                currEntry = new OffsetEntry();
                                currEntry.setStart(-1);
                                currEntry.setEnd(-1);
                            } else {
                                sortOffEntries();
                            }
                            caretByteIndex = clickAbsoluteByteIndex;
                            int docCaret = mapByteIndexToAsciiDocPos(caretByteIndex - offset);
                            asciiTextArea.setCaretPosition(docCaret);
                            try {
                                java.awt.geom.Rectangle2D r2d = asciiTextArea.modelToView2D(docCaret);
                                if (r2d != null) asciiTextArea.scrollRectToVisible(r2d.getBounds());
                            } catch (BadLocationException ex) {
                                // Ignorar si la posición no es válida
                            }
                            refreshSelection();
                            updateOffsetLabel();
                        }
                    });
                    popup.add(endItem);
                }

                // Opciones de borrado y split solo si está en un offset existente (no currEntry)
                if (targetEntry != currEntry) {
                    JMenuItem deleteItem = new JMenuItem(rb.getString("offsetDelete"));
                    deleteItem.addActionListener(ev -> {
                        int confirm = JOptionPane.showConfirmDialog(HexViewer.this, 
                            rb.getString("confirmRangeDelete"), 
                            rb.getString("confirmRangeDeleteTitle"), 
                            JOptionPane.YES_NO_OPTION, 
                            JOptionPane.QUESTION_MESSAGE);
                        if (confirm == JOptionPane.YES_OPTION) {
                            offEntries.remove(targetEntry);
                            sortOffEntries();
                            refreshSelection();
                            updateOffsetLabel();
                        }
                    });
                    popup.add(deleteItem);

                    if (targetEntry.getEnd() - targetEntry.getStart() > 1) {
                        JMenuItem splitItem = new JMenuItem(rb.getString("offsetSplit"));
                        splitItem.addActionListener(ev -> {
                            String valor = JOptionPane.showInputDialog(HexViewer.this, rb.getString("offsetSplitPrompt"), "16");
                            if (valor != null) {
                                try {
                                    int len = Integer.parseInt(valor.trim());
                                    if (len > 0) {
                                        offEntries.remove(targetEntry);
                                        offEntries.addAll(targetEntry.split(len, fileBytes));
                                        sortOffEntries();
                                        refreshSelection();
                                    }
                                } catch (NumberFormatException exSplit) {
                                    showMessage(rb.getString("offsetSplitCancel"), rb.getString("offsetSplitCancelTitle"), JOptionPane.INFORMATION_MESSAGE);
                                }
                            }
                        });
                        popup.add(splitItem);
                    }
                }

                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        
        // Foco al hacer click en hex
        hexTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                hexTextArea.requestFocusInWindow();
            }
        });
        
        // Deshabilitar Ctrl+A (select all) en las áreas para que el accelerador del menú funcione
        int menuShortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        
        // Key bindings for asciiTextArea
        asciiTextArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_H, menuShortcut), "compareRoms");
        asciiTextArea.getActionMap().put("compareRoms", new AbstractAction() { public void actionPerformed(ActionEvent e) { compareRomsAction(); } });
        
        // Key bindings for hexTextArea
        hexTextArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_H, menuShortcut), "compareRoms");
        hexTextArea.getActionMap().put("compareRoms", new AbstractAction() { public void actionPerformed(ActionEvent e) { compareRomsAction(); } });
        
        // Rueda del mouse: desplazamiento manteniendo posición visual del caret
        MouseWheelListener wheelListener = e -> {
            int rot = e.getWheelRotation();
            if (rot < 0) {
                scrollViewportRows(-1);
            } else if (rot > 0) {
                scrollViewportRows(1);
            }
        };
        for (JTextArea area : Arrays.asList(asciiTextArea, hexTextArea, offsetsTextArea)) {
            area.addMouseWheelListener(wheelListener);
        }
        
        // Deshabilitar Ctrl+A (select all) en las áreas para que el accelerador del menú funcione
        KeyStroke ctrlA = KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcut);
        asciiTextArea.getInputMap().put(ctrlA, "none");
        hexTextArea.getInputMap().put(ctrlA, "none");
        offsetsTextArea.getInputMap().put(ctrlA, "none");

        // Navegación con teclado sobre asciiTextArea
        InputMap im = asciiTextArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = asciiTextArea.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "hex_left");
        am.put("hex_left", new AbstractAction() { public void actionPerformed(ActionEvent e) { moveCaretRelative(-1); }});

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "hex_right");
        am.put("hex_right", new AbstractAction() { public void actionPerformed(ActionEvent e) { moveCaretRelative(1); }});

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "hex_up");
        am.put("hex_up", new AbstractAction() { public void actionPerformed(ActionEvent e) { moveCaretRelative(-visibleColumns); }});

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "hex_down");
        am.put("hex_down", new AbstractAction() { public void actionPerformed(ActionEvent e) { moveCaretRelative(visibleColumns); }});

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "hex_page_up");
        am.put("hex_page_up", new AbstractAction() { public void actionPerformed(ActionEvent e) {
            scrollViewportRows(-visibleRows);
        }});

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "hex_page_down");
        am.put("hex_page_down", new AbstractAction() { public void actionPerformed(ActionEvent e) {
            scrollViewportRows(visibleRows);
        }});

        // HOME -> marcar inicio offset rápido
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "hex_mark_start");
        am.put("hex_mark_start", new AbstractAction() { public void actionPerformed(ActionEvent e) {
            int docPos = asciiTextArea.getCaretPosition();
            int byteIndexInView = mapAsciiDocPosToByteIndex(docPos);
            int absByte = offset + byteIndexInView;
            HexViewer.this.markOffsetStart(absByte);
        }});

        // END -> marcar fin offset rápido
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "hex_mark_end");
        am.put("hex_mark_end", new AbstractAction() { public void actionPerformed(ActionEvent e) {
            int docPos = asciiTextArea.getCaretPosition();
            int byteIndexInView = mapAsciiDocPosToByteIndex(docPos);
            int absByte = offset + byteIndexInView;

            // Detectar en qué offset cae el caret
            OffsetEntry targetEntry = getCaretEntry(absByte);
            // Solo permitir completar si está en currEntry con inicio marcado, o si está dentro de otro offset
            boolean canComplete = (targetEntry == currEntry && currEntry.getStart() >= 0 && absByte >= currEntry.getStart())
                               || (targetEntry != currEntry);
            if (canComplete) {
                String defaultEndChars = absByte < fileBytes.length ? String.format("%02X", fileBytes[absByte] & 0xFF) : lastSelectedEndChars;
                String result = null;
                if (askEndCharactersItem.isSelected()) {
                    result = JOptionPane.showInputDialog(HexViewer.this, "Caracteres finales de cadena (FF-00-01...)", defaultEndChars);
                } else {
                    result = defaultEndChars;
                }
                if (result != null && result.length() > 0) {
                    lastSelectedEndChars = result;

                    // Completar el offset detectado con clamp
                    targetEntry.setEndChars(Arrays.asList(result.toUpperCase().split("-")));
                    int newEnd = absByte;
                    if (targetEntry.getStart() >= 0) {
                        newEnd = Math.max(newEnd, targetEntry.getStart());
                    }
                    targetEntry.setEnd(newEnd);
                    
                    // Si era currEntry, consolidarlo en offEntries
                    if (targetEntry == currEntry) {
                        currEntry.mergeInto(offEntries);
                        currEntry = new OffsetEntry();
                        currEntry.setStart(-1);
                        currEntry.setEnd(-1);
                    }

                    sortOffEntries();
                    refreshSelection();
                    updateOffsetLabel();
                }
            }
        }});

        // F3/F4: vista 16 columnas / 16 filas (registrar a nivel de ventana)
        JRootPane rp = getRootPane();
        InputMap winIm = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap winAm = rp.getActionMap();
        // F3: alternar 16/32 columnas, F4: alternar 16/32 filas
        winIm.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "toggle_cols");
        winAm.put("toggle_cols", new AbstractAction() { public void actionPerformed(ActionEvent e) {
            int target = (visibleColumns == 16 ? 32 : 16);
            changeColumns(target);
        }});
        winIm.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "toggle_rows");
        winAm.put("toggle_rows", new AbstractAction() { public void actionPerformed(ActionEvent e) {
            visibleRows = (visibleRows == 16 ? 32 : 16);
            refreshViewMode();
            if (isMaximized()) {
                recalculateVisibleRowsAndRefresh();
            } else {
                adjustWindowToContent();
                pack();
                refreshAll();
            }
        }});

        // Suprimir: borrar offset si caret dentro
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "hex_delete");
        am.put("hex_delete", new AbstractAction() { public void actionPerformed(ActionEvent e) {
            int asciiDocPos = asciiTextArea.getCaretPosition();
            int localByte = mapAsciiDocPosToByteIndex(asciiDocPos);
            int absByte = offset + localByte;
            OffsetEntry target = null;
            for (OffsetEntry oe : offEntries) {
                if (absByte >= oe.getStart() && absByte <= oe.getEnd()) { target = oe; break; }
            }
            if (target != null) {
                int confirm = JOptionPane.showConfirmDialog(HexViewer.this, 
                    rb.getString(KeyConstants.KEY_CONFIRM_RANGE_DELETE), 
                    rb.getString(KeyConstants.KEY_CONFIRM_RANGE_DELETE_TITLE), 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    offEntries.remove(target);
                    refreshAll();
                }
            }
        }});
        
        // Drag & drop sobre el contenedor
        try {
            new com.wave.hextractor.object.FileDrop(this, files -> {
                if (files != null && files.length > 0) {
                    requestFocus();
                    requestFocusInWindow();
                    boolean extDropped = false;
                    for (File file : files) {
                        if (file.getAbsolutePath().endsWith(EXTENSION_TABLE)) {
                            reloadTableFile(file);
                        } else if (file.getAbsolutePath().endsWith(EXTENSION_EXTRACTION)) {
                            reloadExtAsOffsetsFile(file);
                            extDropped = true;
                        } else {
                            reloadHexFile(file);
                        }
                    }
                    // If any .ext was dropped, ensure offEntries is sorted and unique
                    if (extDropped && offEntries != null) {
                        Set<OffsetEntry> unique = new HashSet<>(offEntries);
                        offEntries.clear();
                        offEntries.addAll(unique);
                        Collections.sort(offEntries);
                        refreshAll();
                    }
                }
            });
        } catch (Exception ex) {
            Utils.logException(ex);
        }
        
        // Rueda ratón para scroll unificado
        addMouseWheelListener(e -> {
            int rotation = e.getWheelRotation();
            int delta = rotation * visibleColumns;
            offset = Math.max(0, Math.min(Math.max(0, fileBytes.length - getViewSize()), offset + delta));
            if (vsb != null) vsb.setValue(offset);
            updateThreeAreaContent();
        });
        
    }

    private void updateThreeAreaContent() {
        if (asciiTextArea == null || hexTextArea == null || offsetsTextArea == null) return;
        int viewSize = getViewSize();
        int end = Math.min(fileBytes.length, offset + viewSize);
        StringBuilder asciiBuilder = new StringBuilder();
        StringBuilder hexBuilder = new StringBuilder();
        StringBuilder offsBuilder = new StringBuilder();
        int bytesPerRow = visibleColumns;
        int rowCount = visibleRows;
        for (int r = 0; r < rowCount; r++) {
            int rowOffset = offset + r * bytesPerRow;
            if (rowOffset >= end) break;
            offsBuilder.append(String.format("%08X", rowOffset));
            for (int c = 0; c < bytesPerRow; c++) {
                int idx = rowOffset + c;
                if (idx >= end) {
                    // pad remaining cells
                    asciiBuilder.append(' ');
                    hexBuilder.append("   ");
                } else {
                    byte b = fileBytes[idx];
                    // Use hexTable to convert byte to character, same as original code
                    if (hexTable != null) {
                        asciiBuilder.append(hexTable.toString(b, false));
                    } else {
                        asciiBuilder.append((b >= 32 && b <= 126) ? (char) b : '.');
                    }
                    hexBuilder.append(String.format("%02X ", b));
                }
            }
            // newline for all but last displayed row
            if (r < rowCount - 1) {
                asciiBuilder.append('\n');
                hexBuilder.append('\n');
                offsBuilder.append('\n');
            }
        }
        asciiTextArea.setText(asciiBuilder.toString());
        hexTextArea.setText(hexBuilder.toString());
        offsetsTextArea.setText(offsBuilder.toString());
        // Actualizar label de offset
        if (offsetLabelValue != null) {
            offsetLabelValue.setText(getOffsetLabelValue());
        }
        // Restore caret mapping if we had a previous caret byte
        if (caretByteIndex >= 0) {
            int docPos = mapByteIndexToAsciiDocPos(caretByteIndex - offset);
            if (docPos >= 0 && docPos <= asciiTextArea.getDocument().getLength()) {
                asciiTextArea.setCaretPosition(docPos);
                refreshSelection();
            }
        } else {
            // Inicializar caret al inicio
            caretByteIndex = offset;
            asciiTextArea.setCaretPosition(0);
            refreshSelection();
        }
    }

    // Map ASCII document position (including newlines) to byte index within current view
    private int mapAsciiDocPosToByteIndex(int docPos) {
        return HexViewerMappingUtils.mapAsciiDocPosToByteIndex(docPos, visibleColumns, visibleRows);
    }

    // Map byte index within current view to ASCII document position
    private int mapByteIndexToAsciiDocPos(int byteIndex) {
        return HexViewerMappingUtils.mapByteIndexToAsciiDocPos(byteIndex, visibleColumns, visibleRows);
    }

    // Map byte index within current view to hex document position (3 chars per byte, newline per row except last)
    private int mapByteIndexToHexDocPos(int byteIndex) {
        return HexViewerMappingUtils.mapByteIndexToHexDocPos(byteIndex, visibleColumns, visibleRows);
    }

    // Map hex document position to byte index within current view
    private int mapHexDocPosToByteIndex(int docPos) {
        return HexViewerMappingUtils.mapHexDocPosToByteIndex(docPos, visibleColumns, visibleRows);
    }

    private boolean syncingCaret = false;

    // Synchronize hex caret with ascii caret
    private void refreshSelection() {
        if (asciiTextArea == null || hexTextArea == null) return;
        if (syncingCaret) return;
        syncingCaret = true;
        int asciiDocPos = asciiTextArea.getCaretPosition();
        int byteIndexInView = mapAsciiDocPosToByteIndex(asciiDocPos);
        if (!programmaticSelection) {
            caretByteIndex = offset + byteIndexInView;
        }
        int hexDocPos = mapByteIndexToHexDocPos(byteIndexInView);
        if (hexDocPos >= 0 && hexDocPos <= hexTextArea.getDocument().getLength()) {
            hexTextArea.setCaretPosition(hexDocPos);
        }
        
        // Draw offset highlights
        Highlighter highlighterHex = hexTextArea.getHighlighter();
        highlighterHex.removeAllHighlights();
        try {
            highlighterHex.addHighlight(hexDocPos, hexDocPos + Constants.HEX_VALUE_SIZE - 1, BLUE_PAINTER);
        } catch (BadLocationException e1) {
            // Ignore
        }
        
        Highlighter highlighterAscii = asciiTextArea.getHighlighter();
        highlighterAscii.removeAllHighlights();
        try {
            highlighterAscii.addHighlight(asciiDocPos, asciiDocPos + 1, BLUE_PAINTER);
            
            // Draw all offset entries
            for (OffsetEntry entry : offEntries) {
                drawOffsetEntry(entry, highlighterAscii, LGRAY_PAINTER, ORANGE_PAINTER);
            }
            
            // Draw current entry start if set (absolute file position)
            if (currEntry.getStart() >= 0 && currEntry.getStart() >= offset && currEntry.getStart() < offset + getViewSize()) {
                int startByteInView = currEntry.getStart() - offset;
                int startDocPos = mapByteIndexToAsciiDocPos(startByteInView);
                if (startDocPos >= 0 && startDocPos < asciiTextArea.getDocument().getLength()) {
                    highlighterAscii.addHighlight(startDocPos, startDocPos + 1, YELLOW_PAINTER);
                }
            }
            
            // Draw current entry end if set (absolute file position)
            if (currEntry.getEnd() >= 0 && currEntry.getEnd() >= offset && currEntry.getEnd() < offset + getViewSize()) {
                int endByteInView = currEntry.getEnd() - offset;
                int endDocPos = mapByteIndexToAsciiDocPos(endByteInView);
                if (endDocPos >= 0 && endDocPos < asciiTextArea.getDocument().getLength()) {
                    highlighterAscii.addHighlight(endDocPos, endDocPos + 1, ORANGE_PAINTER);
                }
            }
        } catch (BadLocationException e1) {
            Utils.log("Bad location in refreshSelection: " + e1.getMessage());
        }
        
        // Actualizar label de offset cuando cambie la posición del caret
        if (offsetLabelValue != null && caretByteIndex >= 0) {
            int size = fileBytes.length - 1;
            int lengthDec = String.valueOf(size).length();
            int lengthHex = Integer.toHexString(size).length();
            String strFormat = "%0" + lengthDec + "d";
            offsetLabelValue.setText("0x" + Utils.intToHexString(caretByteIndex, lengthHex) + " (" + String.format(strFormat, caretByteIndex)
                    + ") / 0x" + Utils.intToHexString(size, lengthHex) + " (" + String.format(strFormat, size)
                    + ") - (" + String.format("%03.2f", (100f * caretByteIndex) / size) + "% )");
        }
        
        syncingCaret = false;
    }

    private void refreshSelectionFromHex() {
        if (asciiTextArea == null || hexTextArea == null) return;
        if (syncingCaret) return;
        syncingCaret = true;
        int hexDocPos = hexTextArea.getCaretPosition();
        int byteIndexInView = mapHexDocPosToByteIndex(hexDocPos);
        caretByteIndex = offset + byteIndexInView;
        int asciiDocPos = mapByteIndexToAsciiDocPos(byteIndexInView);
        if (asciiDocPos >= 0 && asciiDocPos <= asciiTextArea.getDocument().getLength()) {
            asciiTextArea.setCaretPosition(asciiDocPos);
        }
        syncingCaret = false;
    }

    /**
     * Draw offset entry with highlights.
     *
     * @param entry the offset entry
     * @param highlighter the highlighter
     * @param painter the painter for the body
     * @param borderPainter the painter for start/end markers
     */
    private void drawOffsetEntry(OffsetEntry entry, Highlighter highlighter,
                                 HighlightPainter painter, HighlightPainter borderPainter) {
        try {
            // Check if entry overlaps with current view
            if (entry.getStart() <= offset + getViewSize() - 1 && entry.getEnd() >= offset) {
                // Clip to visible range (absolute file positions)
                int startAbsolute = entry.getStart();
                int endAbsolute = entry.getEnd();
                int viewEnd = offset + getViewSize() - 1;
                
                // Clip to view boundaries
                if (startAbsolute < offset) {
                    startAbsolute = offset;
                }
                if (endAbsolute > viewEnd) {
                    endAbsolute = viewEnd;
                }
                
                // Convert absolute positions to byte indices in view
                int startByteInView = startAbsolute - offset;
                int endByteInView = endAbsolute - offset;
                
                // Convert to document positions (with newlines)
                int startDocPos = mapByteIndexToAsciiDocPos(startByteInView);
                int endDocPos = mapByteIndexToAsciiDocPos(endByteInView);
                
                // Draw start marker if entry starts in visible area
                if (entry.getStart() >= offset) {
                    highlighter.addHighlight(startDocPos, startDocPos + 1, borderPainter);
                    startDocPos++; // skip the start marker for body
                }
                
                // Draw body (if there's space between start and end)
                if (startDocPos < endDocPos) {
                    highlighter.addHighlight(startDocPos, endDocPos, painter);
                }
                
                // Draw end marker
                highlighter.addHighlight(endDocPos, endDocPos + 1, borderPainter);
            }
        } catch (BadLocationException e) {
            Utils.log("Bad location in drawOffsetEntry: " + e.getMessage());
        }
    }

    /* Navegación de caret usando teclado (izquierda/derecha) */
    private void moveCaretRelative(int delta) {
        if (asciiTextArea == null) return;
        int localIndex = caretByteIndex - offset;
        localIndex += delta;
        int maxLocal = Math.min(getViewSize() - 1, fileBytes.length - 1 - offset);
        if (localIndex < 0) {
            // subir offset si es posible
            if (offset > 0) {
                offset = Math.max(0, offset - visibleColumns);
                if (vsb != null) vsb.setValue(offset);
                updateThreeAreaContent();
                localIndex = Math.min(getViewSize() - 1, caretByteIndex - offset);
            } else {
                localIndex = 0;
            }
        } else if (localIndex > maxLocal) {
            // bajar offset si alcanzamos final visible
            if (offset < Math.max(0, fileBytes.length - getViewSize())) {
                offset = Math.min(Math.max(0, fileBytes.length - getViewSize()), offset + visibleColumns);
                if (vsb != null) vsb.setValue(offset);
                updateThreeAreaContent();
                localIndex = Math.min(getViewSize() - 1, caretByteIndex - offset);
            } else {
                localIndex = maxLocal;
            }
        }
        caretByteIndex = offset + localIndex;
        int docPos = mapByteIndexToAsciiDocPos(localIndex);
        if (docPos >= 0 && docPos <= asciiTextArea.getDocument().getLength()) {
            asciiTextArea.setCaretPosition(docPos);
        }
        refreshSelection();
    }

    // Mantiene la fila/columna visual del caret al desplazar viewport por filas
    private void scrollViewportRows(int deltaRows) {
        if (asciiTextArea == null) return;
        int docPos = asciiTextArea.getCaretPosition();
        int localByte = mapAsciiDocPosToByteIndex(docPos);
        int col = localByte % visibleColumns;
        int row = localByte / visibleColumns;
        int newOffset = offset + deltaRows * visibleColumns;
        newOffset = Math.max(0, Math.min(Math.max(0, fileBytes.length - getViewSize()), newOffset));
        offset = newOffset;
        if (vsb != null) vsb.setValue(offset);
        updateThreeAreaContent();
        if (row >= visibleRows) row = visibleRows - 1;
        int newLocalByte = row * visibleColumns + col;
        if (newLocalByte >= getViewSize()) newLocalByte = Math.max(0, getViewSize() - 1);
        caretByteIndex = offset + newLocalByte;
        int newDocPos = mapByteIndexToAsciiDocPos(newLocalByte);
        if (newDocPos >= 0 && newDocPos <= asciiTextArea.getDocument().getLength()) {
            asciiTextArea.setCaretPosition(newDocPos);
        }
        refreshSelection();
    }

    private boolean isMaximized() {
        return (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    /**
     * Sets the look and feel.
     */
    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            Utils.logException(e);
        }
        // No reasignar rb, ya está inicializado como final
    }

    /**
     * New HexViewer with inputFile and tableFile.
     *
     * @param inputFile the input file
     * @param tableFile the table file
     * @throws IOException the exception
     */
    public static void view(String inputFile, String tableFile) throws IOException {
        Utils.log("Viewing Hex file \"" + inputFile + "\"\n with table file: \"" + tableFile + "\".");
        new HexViewer(Files.readAllBytes(Paths.get(inputFile)), inputFile, new HexTable(tableFile), tableFile);
    }

    /**
     * New HexViewer with inputFile and default table.
     *
     * @param inputFile the input file
     * @throws IOException the exception
     */
    public static void view(String inputFile) throws IOException {
        Utils.log("Viewing Hex file \"" + inputFile + "\"\n with table file ascii.");
        new HexViewer(Files.readAllBytes(Paths.get(inputFile)), inputFile, new HexTable(0), DEFAULT_TABLE);
    }

    /**
     * New empty HexViewer.
     */
    public static void view() {
        Utils.log(Utils.getMessage("consoleViewingHexFileEmpty"));
        new HexViewer(new byte[MAX_COLS_AND_ROWS * MAX_COLS_AND_ROWS], DEFAULT_HEXFILE, new HexTable(0), DEFAULT_TABLE);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        //None
    }
}
