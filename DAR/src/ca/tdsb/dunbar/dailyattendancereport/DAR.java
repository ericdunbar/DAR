package ca.tdsb.dunbar.dailyattendancereport;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class DAR extends Application {

	// JavaFX variables
	private Text errorStatusFX;
	private Text destinationDirFX;
	private Text masterUsefulDARFX;
	private Text masterUselessDARFX;

	private static final String titleTxtFX = "Daily Attendance Report processor version 2016.12.23";

	// PREFERENCES variables
	DARProperties preferences = new DARProperties("DAR.config");

	public final String prefInputPath = "input_path";
	public final String prefOutputPath = "output_path";
	public final String prefMasterUsefulDAR = "master_useful_DAR_PowerBuilder";
	public final String prefMasterUselessDAR = "master_useless_DAR_Teacher_Class_Attendance_";
	public final String prefSejdaLocation = "sejda";

	// CLASS or INSTANCE variables
	/**
	 * The two types of daily attendance record PDF.
	 * 
	 * @author 094360
	 *
	 */
	enum DARType {
		Useless, Useful
	}

	public static void main(String[] args) {
		Application.launch(args);
	}

	/**
	 * Recursively deletes the contents of a directory. //
	 * http://stackoverflow.com/questions/20281835/how-to-delete-a-folder-with-files-using-java
	 * 
	 * @param file
	 *            directory to empty
	 * @return
	 */
	boolean deleteDir(File file) {
		File[] contents = file.listFiles();
		if (contents != null) {
			for (File f : contents) {
				deleteDir(f);
			}
		}
		return file.delete();
	}

	/**
	 * Creates or empties a temporary directory with name in the system's temp
	 * directory structure.
	 * 
	 * @param name
	 *            Name of temporary directory to create or empty
	 * @return File object pointing to the directory
	 */
	private File createTempDir(String name) {
		// http://stackoverflow.com/questions/31154727/saving-files-to-temp-folder
		String tempDirS = System.getProperty("java.io.tmpdir") + name;
		System.out.println("temp directory: " + tempDirS);
		File tempDir = new File(tempDirS);
		System.out.println("Info. Successful temp dir creation?: " + tempDir.mkdir() + " " + tempDirS);
		if (!tempDir.exists())
			tempDir = null;
		else {
			for (File f : tempDir.listFiles()) {
				f.delete();
			}
		}
		return tempDir;
	}

	/**
	 * Default temporary directory.
	 * 
	 * @return File object pointing to the directory. Note that calling method
	 *         needs to check for its existence.
	 */
	private File createTempDir() {
		return createTempDir("DARv20161223");
	}

	/**
	 * Wrap given String in quotations. Helpful for DOS command line arguments.
	 * 
	 * @param s
	 *            String to be wrapped in quotations
	 * @return String wrapped in quotations
	 */
	private String quoteWrap(String s) {
		return "\"" + s + "\"";
	}

	private void sejdaSplitDAR(List<String> cmdAndArgs, File baseDir) throws IOException {
		// troubleshooting
		for (String string : cmdAndArgs) {
			// System.out.print(string + " ");
			System.out.println("         Param: _" + string + "_");
		}

		// call the sejda processor
		ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
		pb.directory(new File(FilenameUtils.getFullPath(preferences.getProperty(prefSejdaLocation))));
		Process p = pb.start();

		// process the console output to prevent application from hanging
		// http://stackoverflow.com/questions/3774432/starting-a-process-in-java
		try {
			String line;
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = input.readLine()) != null) {
				System.out.println(line);
			}
			input.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

	public String getPDFDate(String sourceFile, DARType darT) throws IOException {
		File tempDir = createTempDir("sejda_date_extraction");
		if (tempDir == null)
			throw new IOException(
					"Temp date directory could not be created. Try again. \nIf error persists logout and login or restart.");

		String sejdaS = preferences.getProperty(prefSejdaLocation);

		// PREPARE SEDJA
		// TEXT COORDINATES:
		// String c[] = {"--top", "--left", "--width", "--height"};
		String c[];

		if (darT == DARType.Useful)
			c = new String[] { "514", "61", "75", "22" }; // coords for USEFUL
															// split by DATE
		else
			c = new String[] { "340", "55", "75", "22" }; // coords for USELESS
		// split by DATE

		// DESTINATION DIRECTORY
		String destinationDirectory = tempDir.getAbsolutePath();

		// http://stackoverflow.com/questions/357315/get-list-of-passed-arguments-in-windows-batch-script-bat
		// http://stackoverflow.com/questions/19103570/run-batch-file-from-java-code

		List<String> cmdAndArgs = Arrays.asList("cmd", "/c", "call", quoteWrap(sejdaS), "splitbytext", "-f",
				quoteWrap(sourceFile), "--top", c[0], "--left", c[1], "--width", c[2], "--height", c[3], "-o",
				quoteWrap(destinationDirectory), "-p", "[TEXT]", "--existingOutput", "overwrite");

		sejdaSplitDAR(cmdAndArgs, tempDir);

		File fL[] = tempDir.listFiles();

		if (fL.length != 1) {
			// do not delete dir for troublshooting purposes
			return "date_error";
		}

		deleteDir(tempDir);
		return FilenameUtils.removeExtension(fL[0].getName());
	}

	/**
	 * Splits the DAR.
	 * 
	 * @throws IOException
	 */
	public void splitDAR(DARType whichDAR) throws IOException {

		// SET PREFS
		// TODO: MOVE ELSEWHERE
		preferences.setProperty(prefSejdaLocation,
				"C:\\Users\\094360\\Dropbox\\Riverdale at TDSB\\ICS General\\DAR\\sejda-console-2.10.4-bin\\sejda-console-2.10.4\\bin\\sejda-console");
		preferences.setProperty("test",
				"C:\\Users\\094360\\Dropbox\\Riverdale at TDSB\\ICS General\\DAR\\sejda-console-2.10.4-bin\\sejda-console-2.10.4\\bin\\a.bat");

		/*
		 * List of tasks to do: 1. prepare temporary directory by emptying it 2.
		 * prepare arguments to pass to sejda for DATE getDateFromPDF(File f) 3.
		 * run sejda for DATE 4. read DATE into a variable dateVar 5. write
		 * dateVar to file 6. delete PDF
		 */

		String sejdaS = preferences.getProperty(prefSejdaLocation);

		// 0. PREPARE SEDJA

		// 1. TEXT COORDINATES & type of preferences file
		// String c[] = {"--top", "--left", "--width", "--height"};

		String localPrefDARLocation;

		String teacherNameCoordinates[];

		String describeDAR = whichDAR.name();

		if (whichDAR == DARType.Useful) {
			localPrefDARLocation = prefMasterUsefulDAR;

			// USEFUL split by TEACHERNAME
			teacherNameCoordinates = new String[] { "86", "84", "200", "22" };
		} else {
			localPrefDARLocation = prefMasterUselessDAR;

			// USELESS split by TEACHERNAME
			teacherNameCoordinates = new String[] { "105", "71", "200", "22" };
		}

		System.out.println("ENUM NAME: " + describeDAR);

		// SOURCE FILE
		String sourceFile = preferences.getProperty(localPrefDARLocation);

		// DATE
		String dateForDAR = getPDFDate(sourceFile, whichDAR);
		System.out.println("THE DATE: " + dateForDAR);

		// DESTINATION DIRECTORY
		// Prepare temporary directory
		File tempDir = createTempDir();
		if (tempDir == null)
			throw new IOException("Temp process directory could not be created. Try again.");

		String destinationDirectory = tempDir.getAbsolutePath();

		// PERFORM THE SPLIT

		// http://stackoverflow.com/questions/357315/get-list-of-passed-arguments-in-windows-batch-script-bat
		// http://stackoverflow.com/questions/19103570/run-batch-file-from-java-code
		List<String> cmdAndArgs = Arrays.asList("cmd", "/c", "call", quoteWrap(sejdaS), "splitbytext", "-f",
				quoteWrap(sourceFile), "--top", teacherNameCoordinates[0], "--left", teacherNameCoordinates[1],
				"--width", teacherNameCoordinates[2], "--height", teacherNameCoordinates[3], "-o",
				quoteWrap(destinationDirectory), "-p", "[TEXT]", "--existingOutput", "overwrite");

		sejdaSplitDAR(cmdAndArgs, tempDir);

		String newFileList[] = tempDir.list(new SuffixFileFilter(".pdf"));

		// MOVE and COPY PDFs

		System.out.println("TROUBLESHOOTING: tempDir.getAbsolutePath() = " + tempDir.getAbsolutePath());

		for (String string : newFileList) {
			System.out.println("    " + tempDir.getAbsolutePath() + "\\" + string);
		}

		//Attempted fix for constantly refreshing archive directory
		File archiveDir = new File(preferences.getProperty(prefOutputPath) + "\\Archive\\");
		archiveDir.mkdir();
		archiveDir = new File(preferences.getProperty(prefOutputPath) + "\\Archive\\" + dateForDAR);
		archiveDir.mkdir();

		System.out.println();
		System.out.println("Start: MOVE and ARCHIVE FILES");
		for (String s : newFileList) {
			String oldFile = tempDir.getAbsolutePath() + "\\" + s;

			String newName = FilenameUtils.getBaseName(s) + " " + describeDAR + ".pdf";
			String newArchivalName = FilenameUtils.getBaseName(s) + " " + describeDAR + " " + dateForDAR + ".pdf";

			String newFile = preferences.getProperty(prefOutputPath) + "\\" + newName;
			String archiveFile = archiveDir.getAbsolutePath() + "\\" + newArchivalName;

			darMoveFile(oldFile, newFile, archiveFile);
		}
		System.out.println("End: MOVE and ARCHIVE FILES");

		// MOVE master DAR to ARCHIVE
		String FROM = preferences.getProperty(localPrefDARLocation);
		String TO = preferences.getProperty(prefOutputPath) + "\\Archive\\Masters\\" + "Master_DAR_" + describeDAR + "_"
				+ dateForDAR + ".pdf";
		darMoveFile(FROM, TO);
		System.out.println("End: MOVE and ARCHIVE MASTER for " + describeDAR);
	}

	private void darMoveFile(String oldFile, String newFile) throws IOException {
		darMoveFile(oldFile, newFile, null);
	}

	private void darMoveFile(String oldFile, String newFile, String secondNewFile) throws IOException {
		new File(newFile).delete();

		if (secondNewFile != null) {
			new File(secondNewFile).delete();
			FileUtils.copyFile(FileUtils.getFile(oldFile), FileUtils.getFile(secondNewFile));
		}
		FileUtils.moveFile(FileUtils.getFile(oldFile), FileUtils.getFile(newFile));
	}

	@Override
	public void start(Stage primaryStage) {
		System.out.println(preferences.getFileName());

		primaryStage.setTitle(titleTxtFX);

		System.out.println("I'm here");

		// Window label
		Label label = new Label("Daily Attendance Report");
		label.setTextFill(Color.DARKBLUE);
		label.setFont(Font.font("Calibri", FontWeight.BOLD, 36));
		HBox labelHb = new HBox();
		labelHb.setAlignment(Pos.CENTER);
		labelHb.getChildren().add(label);

		// Buttons
		Button btn1MasterDAR = new Button("Choose _master DAR files...");
		btn1MasterDAR.setOnAction(new SingleFcButtonListener());
		btn1MasterDAR.setMnemonicParsing(true);
		HBox buttonHb1 = new HBox(10);
		buttonHb1.setAlignment(Pos.CENTER);
		buttonHb1.getChildren().addAll(btn1MasterDAR);

		Button btn3 = new Button("Choose destination _directory...");
		btn3.setOnAction(new DestinationDirFcButtonListener());
		btn3.setMnemonicParsing(true);
		HBox buttonHb3 = new HBox(10);
		buttonHb3.setAlignment(Pos.CENTER);
		buttonHb3.getChildren().addAll(btn3);

		Button btn4 = new Button("_Split master DAR");
		btn4.setOnAction(new SplitDARFcButtonListener());
		btn4.setMnemonicParsing(true);
		HBox buttonHb4 = new HBox(10);
		buttonHb4.setAlignment(Pos.CENTER);
		buttonHb4.getChildren().addAll(btn4);

		Button btn5 = new Button("E_xit");
		btn5.setOnAction(new ExitFcButtonListener());
		btn5.setMnemonicParsing(true);
		HBox buttonHb5 = new HBox(10);
		buttonHb5.setAlignment(Pos.CENTER);
		buttonHb5.getChildren().addAll(btn5);

		// Status message text
		errorStatusFX = new Text();
		errorStatusFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		errorStatusFX.setFill(Color.FIREBRICK);
		errorStatusFX.setText("");

		// Destination message text
		masterUsefulDARFX = new Text();
		masterUsefulDARFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		masterUsefulDARFX.setFill(Color.DARKGREEN);
		masterUsefulDARFX.setText("Master Useful DAR:\n    " + preferences.getProperty(prefMasterUsefulDAR));

		// Destination message text
		masterUselessDARFX = new Text();
		masterUselessDARFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		masterUselessDARFX.setFill(Color.DARKGREEN);
		masterUselessDARFX.setText("Master Useless DAR:\n    " + preferences.getProperty(prefMasterUselessDAR));

		// Destination message text
		destinationDirFX = new Text();
		destinationDirFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		destinationDirFX.setFill(Color.DARKGREEN);
		destinationDirFX.setText("Destination directory:\n    " + preferences.getProperty(prefOutputPath));

		// Separator
		Separator separator2 = new Separator();
		separator2.setOrientation(Orientation.HORIZONTAL);

		Separator separator3 = new Separator();
		separator3.setOrientation(Orientation.HORIZONTAL);

		// Vbox
		VBox vbox = new VBox(15);
		vbox.setPadding(new Insets(25, 25, 25, 25));
		;

		vbox.getChildren().addAll(labelHb, buttonHb4, separator2, buttonHb1, buttonHb3, separator3, buttonHb5,
				errorStatusFX, masterUsefulDARFX, masterUselessDARFX, destinationDirFX);

		// Scene
		Scene scene = new Scene(vbox, 600, 700); // w x h
		primaryStage.setScene(scene);
		primaryStage.show();

		boolean success = false;

		if (success)
			Platform.exit();
	}

	private void resetErrorStatus() {
		errorStatusFX.setText("");
	}

	private class SplitDARFcButtonListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {

			resetErrorStatus();

			try {
				errorStatusFX.setText("Start DAR processing...");

				String filesToBeDeletedList[] = new File(preferences.getProperty(prefOutputPath))
						.list(new SuffixFileFilter(".pdf"));

				// delete existing PDFs

				for (String string : filesToBeDeletedList) {
					new File(preferences.getProperty(prefOutputPath) + "\\" + string).delete();
				}

				File masterUselessDAR = new File(preferences.getProperty(prefMasterUselessDAR));
				File masterUsefulDAR = new File(preferences.getProperty(prefMasterUsefulDAR));

				boolean missing[] = { false, false };

				// PERFORM SPLIT
				if (masterUselessDAR.exists()) {
					// Process the useless DAR
					splitDAR(DARType.Useless);
				} else
					missing[0] = true;

				if (masterUsefulDAR.exists()) {
					// Process the useful DAR
					splitDAR(DARType.Useful);
				} else
					missing[1] = true;

				// THROW EXCEPTIONS
				if (missing[0] && missing[1])
					throw new IOException("Both master DAR files missing.");
				else if (!missing[0] || !missing[1]) {
					Desktop desktop = Desktop.getDesktop();
					desktop.open(new File(preferences.getProperty(prefOutputPath)));
					System.out.println("I opened the file explorer");
					if (missing[0])
						throw new IOException(masterUselessDAR.getAbsolutePath() + " missing.");
					else if (missing[1])
						throw new IOException(masterUsefulDAR.getAbsolutePath() + " missing.");
				}
				
				errorStatusFX.setText("Both DARs successfully processed.");

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				errorStatusFX.setText(e1.getMessage());
			}
		}
	}

	private class SingleFcButtonListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			resetErrorStatus();

			showDARFileChooser();
		}
	}

	private class DestinationDirFcButtonListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			resetErrorStatus();
			showDirPicker();
		}
	}

	private class ExitFcButtonListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			resetErrorStatus();
			System.exit(0);
		}
	}

	private void showDARFileChooser() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(System.getenv("userprofile")));
		fileChooser.setTitle("Open useful DAR, aka PowerBuilder.pdf");
		File selectedFile = fileChooser.showOpenDialog(null);

		if (selectedFile != null) {
			preferences.setProperty(prefMasterUsefulDAR, selectedFile.getAbsolutePath());
			masterUsefulDARFX
					.setText("Master Useful DAR (updated):\n    " + preferences.getProperty(prefMasterUsefulDAR));
		}

		fileChooser.setTitle("Open useless DAR, aka Teacher Class Attendance .pdf");
		File selectedFile2 = fileChooser.showOpenDialog(null);

		if (selectedFile2 != null) {
			preferences.setProperty(prefMasterUselessDAR, selectedFile2.getAbsolutePath());
			masterUselessDARFX
					.setText("Master Useless DAR (updated):\n    " + preferences.getProperty(prefMasterUselessDAR));
		}
	}

	/**
	 * Obtains and sets the destination directory.
	 * 
	 */
	private void showDirPicker() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Pick destination directory for split DARs");
		File defaultDirectory = new File(System.getenv("userprofile") + "\\Desktop");
		chooser.setInitialDirectory(defaultDirectory);
		File selectedDirectory = chooser.showDialog(null);

		if (selectedDirectory != null) {
			String messageS = null;
			messageS = selectedDirectory.getAbsolutePath();
			preferences.setProperty(prefOutputPath, messageS);
			destinationDirFX.setText("Destination directory:\n    " + messageS);
		}
	}
}