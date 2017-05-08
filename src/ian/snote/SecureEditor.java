package ian.snote;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class SecureEditor extends JFrame implements ActionListener, DocumentListener {
	private final class TextPaneTransferHander extends TransferHandler {
		private static final long serialVersionUID = 5332200935183708516L;
		private final TransferHandler defaultHandler;
		private TextPaneTransferHander(String property, TransferHandler defaultHandler) {
			super(property);
			this.defaultHandler = defaultHandler;
		}
		@Override
		public boolean canImport(TransferHandler.TransferSupport support) {
			if (defaultHandler.canImport(support)) {
				return true;
			}
			if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				final Transferable tran = support.getTransferable();
				try {
					@SuppressWarnings("unchecked")
					final List<File> list = (List<File>) tran.getTransferData(DataFlavor.javaFileListFlavor);
					final File file = list.get(0);
					return file.getName().toLowerCase().endsWith(".enc");
				} catch (final InvalidDnDOperationException ex) {
					return true;
				} catch (UnsupportedFlavorException | IOException e) {
					e.printStackTrace();
				}
				return false;
			}
			return false;
		}
		@Override
		public void exportAsDrag(JComponent comp, InputEvent e, int action) {
			defaultHandler.exportAsDrag(comp, e, action);
		}
		@Override
		public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
			defaultHandler.exportToClipboard(comp, clip, action);
		}
		@Override
		public boolean importData(TransferHandler.TransferSupport support) {
			if (defaultHandler.canImport(support)) {
				defaultHandler.importData(support);
				return true;
			}
			final Transferable tran = support.getTransferable();
			if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				try {
					@SuppressWarnings("unchecked")
					final List<File> list = (List<File>) tran.getTransferData(DataFlavor.javaFileListFlavor);
					final File file = list.get(0);
					if (changed) {
						checkSave();
					}
					textPane.setText(readFile(file));
					changed = false;
					updateTitle();
					return true;
				} catch (UnsupportedFlavorException | IOException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, e, "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			return false;
		}
	}
	private final class WindowClosingHandler extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent e) {
			if (changed) {
				checkSave();
			}
			final Point loc = SecureEditor.this.getLocation();
			pref.put("xpos", (int) loc.getX());
			pref.put("ypos", (int) loc.getY());
			final Dimension dim = SecureEditor.this.getSize();
			pref.put("width", (int) dim.getWidth());
			pref.put("height", (int) dim.getHeight());
			try {
				Files.write(prefFile, Json.toJSONString(pref).getBytes(UTF8));
			} catch (final IOException e1) {
				e1.printStackTrace();
			}
			System.exit(0);
		}
	}
	private static final long serialVersionUID = -2939827594066304200L;
	private static final Charset UTF8 = Charset.forName("utf-8");
	private static final Path prefFile = Paths.get("pref.json");
	private static byte[] getKey(String pass) {
		return CryptUtil.standardPasswordToSecretKey(pass, 256).getEncoded();
	}
	private static ByteBuffer loadFromFile(Path path, byte key[]) throws IOException {
		final ByteBuffer enc = IOUtil.readPath(path);
		return HC256Encryption.decrypt(enc, key);
	}
	public static void main(String[] args) {
		Map<String, Object> preferenceMap = null;
		try {
			if (!Files.exists(prefFile)) {
				try (InputStream in = SecureEditor.class.getClassLoader().getResourceAsStream("ian/snote/default.json")) {
					preferenceMap = Json.parseJSON(new String(ByteBufferUtil.toBytes(IOUtil.readInputStream(in)), UTF8));
				} catch (final IOException e) {
					e.printStackTrace();
				}
			} else {
				preferenceMap = Json.parseJSON(new String(Files.readAllBytes(prefFile), UTF8));
			}
		} catch (final IOException e) {
			JOptionPane.showMessageDialog(null, "Unable to load preference file " + e.getMessage(), "Start Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		final String theme = Json.getString(preferenceMap, "theme");
		try {
			UIManager.setLookAndFeel(theme);
		} catch (final Exception e) {
			System.out.println("No theme " + theme + " found. Using builtin Nimbus theme");
			try {
				UIManager.setLookAndFeel(NimbusLookAndFeel.class.getCanonicalName());
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
		}
		final SecureEditor se = new SecureEditor(preferenceMap);
		if (args.length > 0) {
			se.file = new File(args[0]);
			se.load();
		}
	}
	private static void writeToFile(Path path, ByteBuffer data, byte key[]) throws IOException {
		final ByteBuffer enc = HC256Encryption.encrypt(data, key);
		final Path file = Files.createTempFile(path.getParent(), "_", ".tmp");
		try (SeekableByteChannel ch = Files.newByteChannel(file, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {
			while (enc.hasRemaining()) {
				ch.write(enc);
			}
		}
		Files.move(file, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}
	protected JTextPane textPane;
	private final JMenuBar menu;
	private JMenuItem copy, paste, cut;
	private boolean changed = false;
	private File file;
	private final Set<String> fontSet = new TreeSet<>();
	private final Map<String, Object> pref;
	private byte[] key;
	public SecureEditor(Map<String, Object> preferenceMap) {
		super("SNote");
		try {
			final BufferedImage image = ImageIO
					.read(this.getClass().getClassLoader().getResourceAsStream("ian/snote/crow48.png"));
			setIconImage(image);
		} catch (final IOException e2) {
			e2.printStackTrace();
		}
		this.pref = preferenceMap;
		GraphicsEnvironment g = null;
		g = GraphicsEnvironment.getLocalGraphicsEnvironment();
		final String[] fonts = g.getAvailableFontFamilyNames();
		for (int i = 0; i < fonts.length; i++) {
			fontSet.add(fonts[i].toLowerCase());
		}
		textPane = new JTextPane();
		textPane.setFont(getPreferredFont());
		add(new JScrollPane(textPane), BorderLayout.CENTER);
		textPane.getDocument().addDocumentListener(this);
		menu = new JMenuBar();
		setJMenuBar(menu);
		buildMenu();
		setSize(Json.getInteger(pref, "width"), Json.getInteger(pref, "height"));
		if (Json.getInteger(pref, "xpos") < 0) {
			setLocationRelativeTo(null);
		} else {
			setLocation(Json.getInteger(pref, "xpos"), Json.getInteger(pref, "ypos"));
		}
		textPane.setDragEnabled(true);
		textPane.setDropMode(DropMode.INSERT);
		final TransferHandler defaultHandler = textPane.getTransferHandler();
		textPane.setTransferHandler(new TextPaneTransferHander("text", defaultHandler));
		setVisible(true);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowClosingHandler());
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		final String action = e.getActionCommand();
		if (action.equals("Quit")) {
			processEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		} else if (action.equals("Open")) {
			file = null;
			load();
		} else if (action.equals("Save")) {
			if (file == null) {
				key = null;
				save();
			} else {
				save();
			}
			changed = false;
		} else if (action.equals("New")) {
			newFile();
		} else if (action.equals("Save as...")) {
			file = null;
			key = null;
			save();
		} else if (action.equals("Select All")) {
			textPane.selectAll();
		} else if (action.equals("Copy")) {
			textPane.copy();
		} else if (action.equals("Cut")) {
			textPane.cut();
		} else if (action.equals("Paste")) {
			textPane.paste();
		} else if (action.equals("Find")) {
			final FindDialog find = new FindDialog(this, false);
			find.showDialog();
		}
	}
	private void buildEditMenu() {
		final JMenu edit = new JMenu("Edit");
		menu.add(edit);
		edit.setMnemonic('E');
		// cut
		cut = new JMenuItem("Cut");
		cut.addActionListener(this);
		cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
		cut.setMnemonic('T');
		edit.add(cut);
		// copy
		copy = new JMenuItem("Copy");
		copy.addActionListener(this);
		copy.setMnemonic('C');
		copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		edit.add(copy);
		// paste
		paste = new JMenuItem("Paste");
		paste.setMnemonic('P');
		paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
		edit.add(paste);
		paste.addActionListener(this);
		// find
		final JMenuItem find = new JMenuItem("Find");
		find.setMnemonic('F');
		find.addActionListener(this);
		edit.add(find);
		find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
		// select all
		final JMenuItem sall = new JMenuItem("Select All");
		sall.setMnemonic('A');
		sall.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
		sall.addActionListener(this);
		edit.add(sall);
	}
	private void buildFileMenu() {
		final JMenu file = new JMenu("File");
		file.setMnemonic('F');
		menu.add(file);
		final JMenuItem n = new JMenuItem("New");
		n.setMnemonic('N');
		n.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		n.addActionListener(this);
		file.add(n);
		final JMenuItem open = new JMenuItem("Open");
		file.add(open);
		open.addActionListener(this);
		open.setMnemonic('O');
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		final JMenuItem save = new JMenuItem("Save");
		file.add(save);
		save.setMnemonic('S');
		save.addActionListener(this);
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		final JMenuItem saveas = new JMenuItem("Save as...");
		saveas.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		file.add(saveas);
		saveas.addActionListener(this);
		final JMenuItem quit = new JMenuItem("Quit");
		file.add(quit);
		quit.addActionListener(this);
		quit.setMnemonic('Q');
		quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
	}
	private void buildMenu() {
		buildFileMenu();
		buildEditMenu();
	}
	@Override
	public void changedUpdate(DocumentEvent e) {
		if (!changed) {
			changed = true;
			updateTitle();
		}
		changed = true;
	}
	private void checkSave() {
		if (changed) {
			final int ans = JOptionPane.showConfirmDialog(this, "The file has changed. Do you want to save it?",
					"Save file", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (ans == JOptionPane.NO_OPTION) {
				return;
			}
			if (file == null) {
				key = null;
			}
			save();
		}
	}
	private Font getPreferredFont() {
		String fontName = Json.getString(pref, "font");
		if (fontSet.contains("consolas")) {
			fontName = "Consolas";
		} else {
			fontName = "Monospaced";
		}
		return new Font(fontName, Font.PLAIN, 18);
	}
	public String getText() {
		final Document doc = textPane.getDocument();
		try {
			return doc.getText(0, doc.getLength());
		} catch (final BadLocationException e) {
			e.printStackTrace();
		}
		return "";
	}
	public String getText(int off, int length) {
		final Document doc = textPane.getDocument();
		if (off + length > doc.getLength()) {
			length = doc.getLength() - off;
		}
		try {
			return doc.getText(off, length);
		} catch (final BadLocationException e) {
			e.printStackTrace();
		}
		return "";
	}
	@Override
	public void insertUpdate(DocumentEvent e) {
		if (!changed) {
			changed = true;
			updateTitle();
		}
		changed = true;
	}
	private void load() {
		try {
			if (file != null) {
				textPane.setText(readFile(file));
				changed = false;
				updateTitle();
				return;
			}
			final JFileChooser dialog = new JFileChooser(Json.getString(pref, "lastDir"));
			dialog.setMultiSelectionEnabled(false);
			dialog.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.getName().toLowerCase().endsWith(".enc");
				}
				@Override
				public String getDescription() {
					return "Encyrpted";
				}
			});
			final int result = dialog.showOpenDialog(this);
			if (result == JFileChooser.CANCEL_OPTION) {
				return;
			}
			if (result == JFileChooser.APPROVE_OPTION) {
				if (changed) {
					checkSave();
				}
				file = dialog.getSelectedFile();
				if (!Files.exists(file.toPath())) {
					JOptionPane.showMessageDialog(this, "File " + file.getName() + " not found", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				textPane.setText(readFile(file));
				changed = false;
				updateTitle();
			}
		} catch (final Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	private void newFile() {
		if (changed) {
			checkSave();
		}
		file = null;
		key = null;
		textPane.setText("");
		setTitle("Editor");
	}
	private String readFile(File file) {
		pref.put("lastDir", file.getParentFile().getAbsolutePath());
		final String pass = Dialogs.getReadPasswordDialog(this);
		key = getKey(pass);
		try {
			final ByteBuffer text = loadFromFile(file.toPath(), key);
			return ByteBufferUtil.toString(text);
		} catch (final IOException e) {
			e.printStackTrace();
			Dialogs.loadError(this, e.getMessage());
		}
		return "";
	}
	@Override
	public void removeUpdate(DocumentEvent e) {
		if (!changed) {
			changed = true;
			updateTitle();
		}
		changed = true;
	}
	private void save() {
		final String text = getText();
		final JFileChooser dialog = new JFileChooser(Json.getString(pref, "lastDir"));
		dialog.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.getName().toLowerCase().endsWith(".enc");
			}
			@Override
			public String getDescription() {
				return "Encyrpted";
			}
		});
		if (file == null) {
			dialog.setDialogTitle("Save As ...");
			final int result = dialog.showSaveDialog(this);
			if (result != JFileChooser.APPROVE_OPTION) {
				return;
			}
			file = dialog.getSelectedFile();
		} else {
			dialog.setDialogTitle("Save");
		}
		if (key == null) {
			final String pass = Dialogs.getCreatePasswordDialog(this);
			key = getKey(pass);
		}
		try {
			if (!file.getName().toLowerCase().endsWith(".enc")) {
				String name = file.getName();
				file = new File(file.getParentFile(), name + ".enc");
			}
			writeToFile(file.toPath(), ByteBuffer.wrap(text.getBytes(UTF8)), key);
			pref.put("lastDir", file.toPath().getParent().toAbsolutePath().toString());
			changed = false;
			updateTitle();
		} catch (final IOException e) {
			e.printStackTrace();
			Dialogs.saveError(this, e.getMessage());
		}
	}
	private void updateTitle() {
		if (changed) {
			if (file != null) {
				setTitle("Editor - " + file.getName() + "*");
			} else {
				setTitle("Editor *");
			}
		} else {
			if (file != null) {
				setTitle("Editor - " + file.getName());
			} else {
				setTitle("Editor");
			}
		}
	}
}