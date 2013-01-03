package cz.matejsimek.scup;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import org.imgscalr.Scalr;

/**
 * Scup - Simple screenshot & file uploader <p>Easily upload screenshot or files
 * to FTP server and copy its URL address to clipboard.
 *
 * @author Matej Simek | www.matejsimek.cz
 */
public class Scup {

  /**
   * Simple new version checking with incremental number
   */
  final static int VERSION = 3;
  //
  public static Clipboard clipboard;
  public static JXTrayIcon trayIcon;
  public static Point virtualOrigin;
  public static Dimension virtualSize;
  private static Preferences prefs;
  /**
   * 16x16 app icon
   */
  public static BufferedImage iconImage = null;
  /**
   * User configuration keys
   */
  public final static String KEY_FTP_SERVER = "FTP_SERVER";
  public final static String KEY_FTP_USERNAME = "FTP_USERNAME";
  public final static String KEY_FTP_PASSWORD = "FTP_PASSWORD";
  public final static String KEY_DIRECTORY = "FTP_DIRECTORY";
  public final static String KEY_URL = "URL";
  public final static String KEY_UPLOAD = "UPLOAD";
  public final static String KEY_MONITOR_ALL = "MONITOR_ALL";
  public final static String KEY_INITIAL_SETTINGS = "INITIAL_SETTINGS";
  /**
   * FTP configuration variables
   */
  private static String FTP_SERVER, FTP_USERNAME, FTP_PASSWORD, FTP_DIRECTORY, URL;
  /**
   * Flag which enable upload to FTP server
   */
  public static boolean UPLOAD;
  /**
   * Flag which enable capture images from all sources, not only printscreen
   */
  public static boolean MONITOR_ALL;
  /**
   * Flag indicates initial settings
   */
  private static boolean INITIAL_SETTINGS;
  /**
   * Tray Popup menu items
   */
  private static JCheckBoxMenuItem uploadEnabledCheckBox;
  private static JCheckBoxMenuItem monitorAllCheckBox;
  private static JMenu historySubmenu;
  private static ActionListener trayIconActionListener = null;

  /**
   * Startup initialization, then endless Thread sleep
   *
   * @param args not used yet
   * @throws InterruptedException
   */
  public static void main(String[] args) throws InterruptedException {
	// Set system windows theme and load default icon
	try {
	  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	  iconImage = ImageIO.read(Scup.class.getResource("/icon.png"));
	} catch (Exception ex) {
	  ex.printStackTrace();
	}
	// Read configuration
	prefs = Preferences.userNodeForPackage(cz.matejsimek.scup.Scup.class);
	readConfiguration();
	// Init tray icon
	initTray();
	// Get system clipboard and asign event handler to it
	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	ClipboardChangeListener cl = new ClipboardChangeListener(clipboard);
	cl.start();
	// Force users to download new version
	checkForUpdates();
	// Show configuration form on startup until first save
	if (INITIAL_SETTINGS) {
	  new SettingsForm().setVisible(true);
	}

	// Endless program run, events are handled in EDT thread
	while (true) {
	  Thread.sleep(Long.MAX_VALUE);
	}
  }

  /**
   * Simple new version checking with incremental number under on url
   */
  static private void checkForUpdates() {
	System.out.println("Checking for updates...");

	try {
	  final URI projectURL = new URI("https://github.com/enzy/Scup");
	  final URL url = new URL("https://raw.github.com/enzy/Scup/master/version.txt");
	  BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

	  String inputLine = in.readLine();
	  if (inputLine != null) {
		int remoteVersion = Integer.parseInt(inputLine);
		System.out.println("Found version " + remoteVersion + ", your is " + VERSION);

		if (remoteVersion > VERSION) {
		  System.out.println("New version available! Get it at: " + projectURL);
		  // Create option dialog
		  JOptionPane pane = new JOptionPane("New version is available. Download it now?", JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION);
		  JDialog dialog = pane.createDialog(new JFrame(), "Scup - Update check");
		  dialog.setIconImage(iconImage);
		  dialog.setAlwaysOnTop(true);
		  dialog.setVisible(true);
		  // Get user choice
		  Object obj = pane.getValue();
		  // Open browser on project page on yes and close program
		  if (obj != null && !obj.equals(JOptionPane.UNINITIALIZED_VALUE) && (Integer) obj == 0) {
			if (openBrowserOn(projectURL)) {
			  System.exit(0);
			}
		  }
		} else {
		  System.out.println("You have the latest version.");
		}
	  }

	  in.close();

	} catch (Exception ex) {
	  System.err.println("Check for updates failed.");
	  ex.printStackTrace();
	}
  }

  /**
   * Open default system browser on given URI
   *
   * @param uri
   * @return true if operation was successful
   */
  static private boolean openBrowserOn(URI uri) {
	if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
	  try {
		Desktop.getDesktop().browse(uri);
		return true;
	  } catch (Exception ex) {
		System.err.println("Error while opening browser");
		ex.printStackTrace();
	  }
	}
	return false;
  }

  /**
   * Open given file with default associated program
   *
   * @param filepath
   * @return
   */
  static private boolean openOnFile(String filepath) {
	if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
	  try {
		Desktop.getDesktop().open(new File(filepath));
		return true;
	  } catch (Exception ex) {
		System.err.println("Error while opening file with associated program");
		ex.printStackTrace();
	  }
	}
	return false;
  }

    /**
   * Place app icon into system tray, build popupmenu and attach event handlers
   * to items
   */
  static private void initTray() {
	if (SystemTray.isSupported()) {
	  final SystemTray tray = SystemTray.getSystemTray();
	  // Different trayicon sizes, prefer downscalling
	  String icoVersion;
	  int icoWidth = tray.getTrayIconSize().width;
	  if (icoWidth <= 16) {
		icoVersion = "";
	  } else if (icoWidth <= 24) {
		icoVersion = "24";
	  } else if (icoWidth <= 32) {
		icoVersion = "32";
	  } else if (icoWidth <= 48) {
		icoVersion = "48";
	  } else if (icoWidth <= 64) {
		icoVersion = "64";
	  } else if (icoWidth <= 96) {
		icoVersion = "96";
	  } else if (icoWidth <= 128) {
		icoVersion = "128";
	  } else if (icoWidth <= 256) {
		icoVersion = "256";
	  } else {
		icoVersion = "512";
	  }
	  // Load tray icon
	  try {
		trayIcon = new JXTrayIcon(ImageIO.read(Scup.class.getResource("/icon" + icoVersion + ".png")));
		trayIcon.setToolTip("Scup v" + VERSION);
		trayIcon.setImageAutoSize(true);
	  } catch (IOException ex) {
		System.err.println("IOException: TrayIcon could not be added.");
		System.exit(1);
	  }
	  // Add tray icon to system tray
	  try {
		tray.add(trayIcon);
	  } catch (AWTException e) {
		System.err.println("AWTException: TrayIcon could not be added.");
		System.exit(1);
	  }
	  // Build popup menu showed on trayicon right click (on Windows)
	  final JPopupMenu jpopup = new JPopupMenu();

	  JMenuItem settingsItem = new JMenuItem("Settings...");
	  uploadEnabledCheckBox = new JCheckBoxMenuItem("Upload to FTP");
	  monitorAllCheckBox = new JCheckBoxMenuItem("Monitor all");
	  historySubmenu = new JMenu("History");
	  JMenuItem exitItem = new JMenuItem("Exit");

	  jpopup.add(settingsItem);
	  jpopup.add(uploadEnabledCheckBox);
	  jpopup.add(monitorAllCheckBox);
	  jpopup.addSeparator();
	  jpopup.add(historySubmenu);
	  jpopup.addSeparator();
	  jpopup.add(exitItem);
	  // Add popup to tray
	  trayIcon.setJPopupMenu(jpopup);
	  // Set default flags
	  uploadEnabledCheckBox.setState(UPLOAD);
	  monitorAllCheckBox.setState(MONITOR_ALL);
	  historySubmenu.setEnabled(false);

	  // Add listener to settingsItem.
	  settingsItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  new SettingsForm().setVisible(true);
		}
	  });

	  // Add listener to uploadEnabledCheckBox
	  uploadEnabledCheckBox.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		  int chxbx = e.getStateChange();
		  if (chxbx == ItemEvent.SELECTED) {
			UPLOAD = true;
		  } else {
			UPLOAD = false;
		  }
		  prefs.putBoolean(KEY_UPLOAD, UPLOAD);
		}
	  });

	  // Add listener to monitorAllCheckBox
	  monitorAllCheckBox.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		  int chxbx = e.getStateChange();
		  if (chxbx == ItemEvent.SELECTED) {
			MONITOR_ALL = true;
		  } else {
			MONITOR_ALL = false;
		  }
		  prefs.putBoolean(KEY_MONITOR_ALL, MONITOR_ALL);
		}
	  });

	  // Add listener to exitItem.
	  exitItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  tray.remove(trayIcon);
		  System.exit(0);
		}
	  });

	  trayIcon.displayMessage("Scup", "I am here to serve", TrayIcon.MessageType.NONE);
	} else {
	  System.err.println("SystemTray is not supported");
	}
  }

  /**
   * Fills class varibles:
   * <code>FTP_SERVER, FTP_USERNAME, FTP_PASSWORD,
   * FTP_DIRECTORY, URL, UPLOAD, MONITOR_ALL</code>
   *
   * @param filename to read configuration from
   */
  static private void readConfiguration(/*String filename*/) {
	// Load config
	FTP_SERVER = prefs.get(KEY_FTP_SERVER, "localhost");
	FTP_USERNAME = prefs.get(KEY_FTP_USERNAME, "anonymous");
	FTP_PASSWORD = prefs.get(KEY_FTP_PASSWORD, "");
	FTP_DIRECTORY = prefs.get(KEY_DIRECTORY, "");
	URL = prefs.get(KEY_URL, "http://localhost");
	UPLOAD = prefs.getBoolean(KEY_UPLOAD, false);
	MONITOR_ALL = prefs.getBoolean(KEY_MONITOR_ALL, true);
	INITIAL_SETTINGS = prefs.getBoolean(KEY_INITIAL_SETTINGS, true);
  }

  public static void reloadConfiguration() {
	readConfiguration();
	uploadEnabledCheckBox.setState(UPLOAD);
	monitorAllCheckBox.setState(MONITOR_ALL);
  }

  /**
   * Whole image handling process - display, crop, save on disk, transfer to
   * FTP, copy its URL to clipboard
   *
   * @param image to process
   * @param cropImage should be image cropped?
   * @param device to display image on
   */
  static void processImage(BufferedImage image, boolean cropImage, GraphicsDevice device) {
	System.out.println("Processing image...");
	System.out.println("Image: " + image.getWidth() + "x" + image.getHeight());

	if (cropImage) {
	  image = cropImage(image, device);
	}
	if (image == null) {
	  System.out.println("Image is empty, canceling");
	  return;
	}

	File imageFile = saveImageToFile(image);

	if (UPLOAD) {
	  // Calculate image hash
	  String hash = generateHashForFile(imageFile);
	  String newFilename = hash.substring(0, 10) + ".png";
	  // Rename file after its hash
	  File renamedFile = new File(newFilename);
	  if(imageFile.renameTo(renamedFile)){
		imageFile = renamedFile;
	  }
	  // Transer image to FTP
	  System.out.println("Uploading image to FTP server...");
	  // FTP file upload service
	  FileUpload fileupload = new FileUpload(FTP_SERVER, FTP_USERNAME, FTP_PASSWORD, FTP_DIRECTORY);
	  if (fileupload.uploadFile(imageFile, imageFile.getName())) {
		System.out.println("Upload done");
		// Generate URL
		final String url = (URL.endsWith("/") ? URL : URL + "/") + imageFile.getName();
		System.out.println(url);
		// Copy URL to clipboard
		setClipboard(url);
		// Notify me about it
		trayIcon.displayMessage("Image uploaded", url, TrayIcon.MessageType.INFO);

		// Display last uploaded image
		switchTrayIconActionListenerTo(new ActionListener() {
		  public void actionPerformed(ActionEvent e) {
			System.out.println("Last image URL " + url);
			trayIcon.displayMessage("Last image URL", url, TrayIcon.MessageType.INFO);
			setClipboard(url);
		  }
		});
		// Save it to history
		addImageToHistory(image, url, false);
	  } else {
		// Upload failed, it happens
		System.err.println("Upload failed");
		trayIcon.displayMessage("Upload failed", "I can not serve, sorry", TrayIcon.MessageType.ERROR);
	  }
	  // Don't keep copy of already uploaded image
	  imageFile.delete();
	} else {
	  // Copy URL to clipboard
	  final String imageAbsolutePath = imageFile.getAbsolutePath();
	  setClipboard(imageAbsolutePath);
	  // Notify user about it
	  System.out.println("Image saved " + imageAbsolutePath);
	  trayIcon.displayMessage("Image saved", imageAbsolutePath, TrayIcon.MessageType.INFO);

	  // Display last uploaded image
	  switchTrayIconActionListenerTo(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  System.out.println("Last image path " + imageAbsolutePath);
		  trayIcon.displayMessage("Last image path", imageAbsolutePath, TrayIcon.MessageType.INFO);
		  setClipboard(imageAbsolutePath);
		}
	  });

	  // Save it to history
	  addImageToHistory(image, imageAbsolutePath, true);
	}
  }

  /**
   * Adds image into history submenu
   *
   * @param image
   * @param path
   */
  static private void addImageToHistory(BufferedImage image, final String path, final boolean isLocalFile) {
	BufferedImage scaled;
	// Resize image to usable dimensions
	if (image.getWidth() > 140 || image.getHeight() > 80) {
	  scaled = Scalr.resize(image, 140, 80);
	} else {
	  scaled = image;
	}

	// Rape JMenuItem with big image
	JMenuItem item = new JMenuItem(path, new ImageIcon(scaled));
	// Copy path on click
	item.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
		setClipboard(path);
	  }
	});

	// Open browser/program on CTRL+click
	item.addMouseListener(new MouseAdapter() {
	  @Override
	  public void mouseReleased(MouseEvent e) {
		if (e.isControlDown()) {
		  if (isLocalFile) {
			openOnFile(path);
		  } else {
			openBrowserOn(URI.create(path));
		  }
		}
	  }
	});
	// Finally add item to submenu
	historySubmenu.add(item);
	historySubmenu.setEnabled(true);
	// Clean old items
	if (historySubmenu.getItemCount() > 5) {
	  historySubmenu.remove(0);
	}
	scaled.flush();
  }

  /**
   * Copy given String to system clipboard (could fail)
   *
   * @param str string to copy
   */
  static void setClipboard(String str) {
	try {
	  clipboard.setContents(new StringSelection(str), null);
	} catch (IllegalStateException ex) {
	  System.err.println("Can't set clipboard, trying again!");
	  ex.printStackTrace();
	  try {
		Thread.sleep(250);
	  } catch (InterruptedException ex1) {
		ex1.printStackTrace();
	  }
	  setClipboard(str);
	}
  }

  /**
   * Assure there is only one action listener on tratIcon
   *
   * @param newListener to switch
   */
  static void switchTrayIconActionListenerTo(ActionListener newListener) {
	if (trayIconActionListener != null) {
	  trayIcon.removeActionListener(trayIconActionListener);
	}
	trayIconActionListener = newListener;
	trayIcon.addActionListener(trayIconActionListener);
  }

  /**
   * Display image full screen and crop it by user celection
   *
   * @param image to crop
   * @param device to display image on
   * @return cropped image or null in case of crop cancel
   */
  static BufferedImage cropImage(BufferedImage image, GraphicsDevice device) {
	CountDownLatch framerun = new CountDownLatch(1);
	FullscreenFrame fullscreenFrame = new FullscreenFrame(framerun, image);
	fullscreenFrame.setVisible(true);

	if (device.isFullScreenSupported()) {
	  device.setFullScreenWindow(fullscreenFrame);
	} else {
	  System.err.println("FullScreen is not supported");
	}

	try {
	  framerun.await();
	} catch (InterruptedException ex) {
	  ex.printStackTrace();
	}

	// When its closed, get cropped image
	if (fullscreenFrame.isImageCropped()) {
	  image.flush();
	  image = fullscreenFrame.getCroppedImage();
	} else {
	  image.flush();
	  image = null;
	}
	fullscreenFrame.setVisible(false);
	fullscreenFrame.dispose();
	fullscreenFrame = null;

	return image;
  }

  /**
   * File handling process - upload single file in original format or multiple files in one zip archive
   * <pre>
   * There is 4 situations, which can occur:
   * - single file, no upload - only absolute path to file is copied to clipboard
   * - single file, upload - file is uploaded to FTP server and named after its SHA hash, URL copied to clipboard
   * - multiple files, no upload - files are compressed into single archive named by actual date and located in running directory, absolute path copied to clipboard
   * - multiple files, upload - files are compressed into single archive and then uploaded to FTP server, file named by its SHA hash, URL copied to clipboard
   * </pre>
   *
   * @param files List of files to process
   */
  static void processFiles(List<File> files) {
	System.out.println("Processing files...");

	File fileToProcess;
	String extension;
	boolean isZip = false;

	if (files.size() > 1) {
	  fileToProcess = zipFiles(files);
	  extension = ".zip";
	  isZip = true;
	} else {
	  fileToProcess = files.get(0);
	  String filename = fileToProcess.getName();
	  System.out.println("Processing file: " + filename);

	  int pos = filename.lastIndexOf(".");
	  extension = filename.substring(pos);
	}

	final String fileUrl;

	if (UPLOAD) {
	  String hash = generateHashForFile(fileToProcess);
	  String newFilename = hash.substring(0, 10) + extension;

	  // Transer file to FTP
	  System.out.println("Uploading file to FTP server...");
	  // FTP file upload service
	  FileUpload fileupload = new FileUpload(FTP_SERVER, FTP_USERNAME, FTP_PASSWORD, FTP_DIRECTORY);
	  if (fileupload.uploadFile(fileToProcess, newFilename)) {
		System.out.println("Upload done");
		// Generate URL
		fileUrl = (URL.endsWith("/") ? URL : URL + "/") + newFilename;
		System.out.println(fileUrl);
		// Clean after myself
		if (isZip) {
		  fileToProcess.delete();
		}
	  } else {
		// Upload failed, it happens
		System.err.println("Upload failed");
		trayIcon.displayMessage("Upload failed", "I can not serve, sorry", TrayIcon.MessageType.ERROR);
		return;
	  }

	} else {
	  fileUrl = fileToProcess.getAbsolutePath();
	}

	// Copy URL to clipboard
	setClipboard(fileUrl);

	// Notify user about it
	System.out.println("File " + (UPLOAD ? "uploaded " : "located ") + fileUrl);
	trayIcon.displayMessage("File " + (UPLOAD ? "uploaded" : "located"), fileUrl, TrayIcon.MessageType.INFO);

	// Display last processed file
	switchTrayIconActionListenerTo(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
		System.out.println("Last file path " + fileUrl);
		trayIcon.displayMessage("Last file path", fileUrl, TrayIcon.MessageType.INFO);
		setClipboard(fileUrl);
	  }
	});

	// Save it to history
	BufferedImage ico = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
	try {
	  ico = ImageIO.read(Scup.class.getResource("/" + (isZip ? "package" : "page") + ".png"));
	} catch (Exception ex) {
	  ex.printStackTrace();
	}
	addImageToHistory(ico, fileUrl, true);

  }

  /**
   * Zip given files into one archive
   *
   * @param files Input files to compress
   * @return Zip file named in format yyyyMMdd-HHmmss.zip
   */
  static File zipFiles(List<File> files) {
	DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
	File outputFile = new File(dateFormat.format(Calendar.getInstance().getTime()) + ".zip");

	try {
	  FileOutputStream fos = new FileOutputStream(outputFile);
	  ZipOutputStream zos = new ZipOutputStream(fos);
	  byte[] buf = new byte[1024];

	  for (File file : files) {
		System.out.println("Compressing file: " + file.getName());
		FileInputStream fis = null;

		try {
		  fis = new FileInputStream(file);
		  zos.putNextEntry(new ZipEntry(file.getName()));
		  int len;
		  while ((len = fis.read(buf)) > 0) {
			zos.write(buf, 0, len);
		  }
		  zos.closeEntry();
		} catch (IOException ex) {
		  ex.printStackTrace();
		}

		fis.close();
	  }

	  zos.close();
	  fos.close();

	} catch (IOException ex) {
	  ex.printStackTrace();
	}

	return outputFile;
  }

  /**
   * Save image to PNG file named by its content hash into current directory
   *
   * @param img
   * @return
   */
  static File saveImageToFile(BufferedImage img) {
	try {
	  // Generate default image name
	  DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
	  File outputFile = new File(dateFormat.format(Calendar.getInstance().getTime()) + ".png");
	  // Write image data
	  ImageIO.write(img, "png", outputFile);

	  return outputFile;

	} catch (IOException ex) {
	  System.err.println("Can't write image to file!");
	}
	return null;
  }

  /**
   * Generate SHA has from given data
   *
   * @param data Array of bytes to calculate hash from
   * @return SHA hash from data or currentTimeMillis in case of error
   */
  static String generateHash(byte[] data) {
	try {
	  MessageDigest md = MessageDigest.getInstance("SHA");
	  md.update(data);

	  Formatter formatter = new Formatter();
	  for (byte b : md.digest()) {
		formatter.format("%02x", b);
	  }

	  return formatter.toString();

	} catch (NoSuchAlgorithmException ex) {
	  System.err.println("Can't load digest algorithm!");
	}

	return Long.toString(System.currentTimeMillis());
  }

  static String generateHashForFile(File file) {
	FileInputStream fis = null;

	try {
	  byte[] buf = new byte[1024];

	  MessageDigest md = MessageDigest.getInstance("SHA");

	  fis = new FileInputStream(file);
	  int len;
	  while ((len = fis.read(buf)) > 0) {
		md.update(buf, 0, len);
	  }

	  Formatter formatter = new Formatter();
	  for (byte b : md.digest()) {
		formatter.format("%02x", b);
	  }

	  fis.close();

	  return formatter.toString();

	} catch (Exception ex) {
	  ex.printStackTrace();
	} finally {
	  try {
		fis.close();
	  } catch (IOException ex) {
		ex.printStackTrace();
	  }
	}

	return null;
  }
}