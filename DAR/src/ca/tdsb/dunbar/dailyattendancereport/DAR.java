package ca.tdsb.dunbar.dailyattendancereport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import ca.tdsb.dunbar.dailyattendancereport.SejdaSupport;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DAR extends Application {

	// TODO: describe TextDAR and MOVE
	public class TextDAR extends TextArea {
		public final void setTextWithDate(String s) {
			// https://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date(System.currentTimeMillis());
			setText(dateFormat.format(date) + ": " + s + "\n" + this.getText());
		}
	}

	public static class TextLbl extends Text {
		private String descriptor;
		private String pref;

		public TextLbl(String descriptor, String pref) {
			this.descriptor = descriptor;
			this.pref = pref;
			initialize();
		}

		private void setTextValue(boolean update) {
			String s = "";
			String updateS = "";

			if (update)
				updateS = " (update)";
			s = descriptor + updateS + ": " + preferences.getProperty(pref);
			this.setText(s);
		}

		public void initialize() {
			setTextValue(false);
		}

		public void update() {
			setTextValue(true);
		}

		private String generateMsgMasterDARLocation(DARType d, boolean update) {
			String s = "";
			String key = "AN ERROR OCCURRED";
			String u = "";

			if (d == DARType.Useless)
				key = preferences.getProperty(prefMasterUselessDAR);
			else if (d == DARType.Useful)
				key = preferences.getProperty(prefMasterUsefulDAR);
			if (update)
				u = " (update)";

			s = "Master " + d.toString() + " DAR" + u + ": " + key;
			return s;
		}
	}

	// JavaFX variables
	private TextDAR programUpdatesFX;
	private TextLbl destinationDirFX;
	private TextLbl masterUsefulDARFX;
	private TextLbl masterUselessDARFX;

	private static final String formTitleFX = "Daily Attendance Report processor version 2016.12.24";

	// PREFERENCES variables
	static DARProperties preferences = new DARProperties("DAR.config");

	public final static String prefInputPath = "input_path";
	public final static String prefOutputPath = "output_path";
	public final static String prefMasterUsefulDAR = "master_useful_DAR_PowerBuilder";
	public final static String prefMasterUselessDAR = "master_useless_DAR_Teacher_Class_Attendance_";
	public final static String prefSejdaDirectory = "sejda";
	public final static String prefSejdaLocation = "sejda_console";
	private TextLbl clockFX;
	private TextLbl sedjaDARFX;
	private Button btnMasterDAR;
	private Button btnDestDir;
	private Button btnSedjaConsole;
	private Button btnSplitDAR;
	private Button btnExit;

	private Button[] buttons;

	// CLASS or INSTANCE variables
	/**
	 * The two types of daily attendance record PDF.
	 * 
	 * @author ED
	 *
	 */
	enum DARType {
		Useless, Useful
	}

	// TODO: Organize logging code
	static PrintStream ps = null; // log file for System.out and StackTrace

	// TODO: Get rid of working. Such an ugly solution.
	static boolean working = false;

	public static void main(String[] args) {

		// http://stackoverflow.com/questions/8043356/file-write-printstream-append
		// http://stackoverflow.com/questions/12053075/how-do-i-write-the-exception-from-printstacktrace-into-a-text-file-in-java
		try {
			ps = new PrintStream(new FileOutputStream("DAR20161228_log.txt", true));
			// new FileOutputStream(System.getProperty("java.io.tmpdir") + "\\"
			// + "DAR_log.txt", true));
			// System.setOut(ps);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// https://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		System.out.print("DAR Splitter launched: ");
		System.out.println(dateFormat.format(date));

		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		System.out.println(preferences.getFileName());

		primaryStage.setTitle(formTitleFX);

		// Window label
		Label label = new Label("Daily Attendance Report");
		label.setTextFill(Color.DARKBLUE);
		label.setFont(Font.font("Calibri", FontWeight.BOLD, 36));
		HBox labelHb = new HBox();
		labelHb.setAlignment(Pos.CENTER);
		labelHb.getChildren().add(label);

		// Buttons
		btnMasterDAR = new Button("Choose master DAR files...");
		btnMasterDAR.setOnAction(new SingleFcButtonListener());
		btnMasterDAR.setMnemonicParsing(true);
		HBox buttonHb1 = new HBox(10);
		buttonHb1.setAlignment(Pos.CENTER);
		buttonHb1.getChildren().addAll(btnMasterDAR);

		btnDestDir = new Button("Choose destination directory...");
		btnDestDir.setOnAction(new DestinationDirFcButtonListener());
		btnDestDir.setMnemonicParsing(true);
		buttonHb1.getChildren().addAll(btnDestDir);

		btnSedjaConsole = new Button("Choose \"sedja-console\"...");
		btnSedjaConsole.setOnAction(new SedjaDirFcButtonListener());
		btnSedjaConsole.setMnemonicParsing(true);
		buttonHb1.getChildren().addAll(btnSedjaConsole);

		btnSplitDAR = new Button("_Split master DAR");
		btnSplitDAR.setOnAction(new SplitDARFcButtonListener());
		btnSplitDAR.setMnemonicParsing(true);
		HBox buttonHb4 = new HBox(10);
		buttonHb4.setAlignment(Pos.CENTER);
		buttonHb4.getChildren().addAll(btnSplitDAR);

		btnExit = new Button("E_xit");
		btnExit.setOnAction(new ExitFcButtonListener());
		btnExit.setMnemonicParsing(true);
		HBox buttonHb5 = new HBox(10);
		buttonHb5.setAlignment(Pos.CENTER);
		buttonHb5.getChildren().addAll(btnExit);

		buttons = new Button[] { btnDestDir, btnMasterDAR, btnSedjaConsole, btnExit, btnSplitDAR };

		// Status message text
		programUpdatesFX = new TextDAR();
		programUpdatesFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		// TODO Should this be a TextArea?
		// errorStatusFX.setFill(Color.FIREBRICK);
		programUpdatesFX.setText("");
		programUpdatesFX.setEditable(false);
		programUpdatesFX.setWrapText(true);

		// Status message text
		clockFX = createTextFX(Font.font("Calibri", FontWeight.NORMAL, 14), Color.DARKSLATEBLUE, "", "");
		clockFX.setWrappingWidth(0);

		// Source location
		masterUsefulDARFX = createTextFX("Master " + DARType.Useful.toString() + " DAR", prefMasterUsefulDAR);
		masterUselessDARFX = createTextFX("Master " + DARType.Useless.toString() + " DAR", prefMasterUselessDAR);

		// Destination location
		destinationDirFX = createTextFX("Destination directory", prefOutputPath);

		// sejda-console location
		sedjaDARFX = createTextFX("sedja app", (prefSejdaLocation));

		// TODO Status updates and labels
		// http://stackoverflow.com/questions/19968012/javafx-update-ui-label-asynchronously-with-messages-while-application-different

		// Separator
		Separator separator2 = new Separator();
		separator2.setOrientation(Orientation.HORIZONTAL);

		Separator separator3 = new Separator();
		separator3.setOrientation(Orientation.HORIZONTAL);

		// Vbox
		VBox vbox = new VBox(15);
		vbox.setPadding(new Insets(25, 25, 25, 25));
		;

		vbox.getChildren().addAll(labelHb, buttonHb4, separator2, buttonHb1, separator3, buttonHb5, programUpdatesFX,
				clockFX, masterUsefulDARFX, masterUselessDARFX, destinationDirFX, sedjaDARFX);

		// Create clock
		Timeline fiveSecondsWonder = new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {

			private int dots = 0;

			@Override
			public void handle(ActionEvent event) {
				DateFormat dateFormat = new SimpleDateFormat("HH:mm");
				Date date = new Date(System.currentTimeMillis());
				String workingS = "idle";
				if (DAR.working) {
					workingS = "processing";
					for (int i = 0; i < dots % 6; i++) {
						workingS += ".";
					}
					dots++;
				}

				clockFX.setText("Status at " + dateFormat.format(date) + ": " + workingS);

			}
		}));

		fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
		fiveSecondsWonder.play();

		// Scene
		Scene scene = new Scene(vbox, 800, 600); // w x h
		primaryStage.setScene(scene);
		primaryStage.show();

		// Trigger split button
		btnSplitDAR.fire();

		// TODO Automate the process if valid DAR is available
		boolean success = false;

		if (success)
			Platform.exit();
	}

	private static TextLbl createTextFX(String descriptor, String prefString) {
		return createTextFX(Font.font("Calibri", FontWeight.NORMAL, 14), Color.DARKGREEN, descriptor, prefString);
	}

	private static TextLbl createTextFX(Font f, Color c, String descriptor, String prefString) {
		TextLbl newTextFX = new TextLbl(descriptor, prefString);
		newTextFX.setFont(f);
		newTextFX.setFill(c);
		newTextFX.setWrappingWidth(700);
		return newTextFX;
	}

	// TODO Describe resetErrorStatus
	private void resetErrorStatus() {
		// TODO What does this do?
		// errorStatusFX.setTextWithDate("");
	}

	// TODO Describe SplitDARFcB
	private class SplitDARFcButtonListener implements EventHandler<ActionEvent> {
		//// http://stackoverflow.com/questions/26554814/javafx-updating-gui

		@Override
		public void handle(ActionEvent e) {
			Task<Void> task = new Task<Void>() {
				@Override
				public Void call() {

					for (Button button : buttons) {
						button.setDisable(true);
					}

					SejdaSupport r = null;
					try {
						r = new SejdaSupport(preferences, ps, programUpdatesFX);
						r.runMe(programUpdatesFX);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						if (e.getMessage() == null) {
							programUpdatesFX.setTextWithDate("Possible problem with preferences. Please set them.");
						} else
							programUpdatesFX.setTextWithDate(e.getMessage());
						e.printStackTrace(ps);
					}

					System.out.println("iiii");
					for (Button button : buttons) {
						System.out.print("round,");
						button.setDisable(false);
					}

					return null;
				}
			};
			task.messageProperty()
					.addListener((obs, oldMessage, newMessage) -> programUpdatesFX.setTextWithDate(newMessage));
			new Thread(task).start();
		}
	}

	// TODO Name properly
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
			showDestinationDirChooser();
		}
	}

	private class SedjaDirFcButtonListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			resetErrorStatus();
			showSedjaChooser();
		}
	}

	private class ExitFcButtonListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			resetErrorStatus();
			System.exit(0);
		}
	}

	private void showSedjaChooser() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(System.getenv("userprofile")));
		fileChooser.setTitle(
				"Select and open \"sedja-console\" from within \"sejda-console-2.10.4-bin\\sejda-console-2.10.4\\bin\"");
		// fileChooser.setSelectedExtensionFilter(filter);
		File selectedFile = fileChooser.showOpenDialog(null);

		if (selectedFile != null) {
			preferences.setProperty(prefSejdaDirectory, selectedFile.getParent());
			preferences.setProperty(prefSejdaLocation, selectedFile.getAbsolutePath());
			// sedjaDARFX.setText("sejda application" + " (updated):\n " +
			// preferences.getProperty(prefSejdaDirectory)
			// + "\n " + preferences.getProperty(prefSejdaLocation));
			sedjaDARFX.update();
		}
	}

	private void showDARFileChooser() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(System.getenv("userprofile")));
		fileChooser.setTitle("Open useful DAR, aka \"PowerBuilder.pdf\"");
		File selectedFile = fileChooser.showOpenDialog(null);

		if (selectedFile != null) {
			preferences.setProperty(prefMasterUsefulDAR, selectedFile.getAbsolutePath());
			// TODO update object
			// masterUsefulDARFX.setText("Master " + DARType.Useful.toString() +
			// " DAR (updated):\n "
			// + preferences.getProperty(prefMasterUsefulDAR));
			masterUsefulDARFX.update();
		}

		fileChooser.setTitle("Open " + DARType.Useless.toString() + " DAR, aka \"Teacher Class Attendance .pdf\"");
		File selectedFile2 = fileChooser.showOpenDialog(null);

		if (selectedFile2 != null) {
			preferences.setProperty(prefMasterUselessDAR, selectedFile2.getAbsolutePath());
			masterUselessDARFX.update();
			// masterUselessDARFX
			// .setText("Master Useless DAR (updated):\n " +
			// preferences.getProperty(prefMasterUselessDAR));
		}
	}

	/**
	 * Obtains and sets the destination directory.
	 * 
	 */
	private void showDestinationDirChooser() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Pick destination directory for split DARs. Hint: use folder on TeacherShare.");
		File defaultDirectory = new File(System.getenv("userprofile") + "\\Desktop");
		chooser.setInitialDirectory(defaultDirectory);
		File selectedDirectory = chooser.showDialog(null);

		if (selectedDirectory != null) {
			String messageS = null;
			messageS = selectedDirectory.getAbsolutePath();
			preferences.setProperty(prefOutputPath, messageS);
			destinationDirFX.update();
			// destinationDirFX.setText("Destination directory:\n " + messageS);
		}
	}
}