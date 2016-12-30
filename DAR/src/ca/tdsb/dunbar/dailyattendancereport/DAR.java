package ca.tdsb.dunbar.dailyattendancereport;

import java.io.File;
import java.io.FileOutputStream;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

	// PREFERENCES fields
	static DARProperties preferences = new DARProperties("DAR.config");

	public final static String prefInputPath = "input_path";
	public final static String prefOutputPath = "output_path";
	public final static String prefMasterUsefulDAR = "master_useful_DAR_PowerBuilder";
	public final static String prefMasterUselessDAR = "master_useless_DAR_Teacher_Class_Attendance_";
	public final static String prefSejdaDirectory = "sejda";
	public final static String prefSejdaLocation = "sejda_console";

	// JavaFX objects and fields
	private TextDAR programUpdatesFX;
	private TextLbl destinationDirFX;
	private TextLbl masterUsefulDARFX;
	private TextLbl masterUselessDARFX;

	private static final String formTitleFX = "Daily Attendance Report processor version 2016.12.24";

	private TextLbl clockFX;
	private TextLbl sedjaDARFX;
	private Button btnMasterDAR;
	private Button btnDestDir;
	private Button btnSedjaConsole;
	private Button btnSplitDAR;
	private Button btnExit;

	private Button[] buttons;

	// GENERAL INSTANCE fields
	/**
	 * The two types of daily attendance record PDF.
	 * 
	 * @author ED
	 *
	 */
	enum DARType {
		Useless, Useful
	}

	// CLASS fields
	static PrintStream ps = null; // log file for System.out and StackTrace

	// TODO: Get rid of working. Such an ugly solution.
	static boolean working = false; // used to determine status updates
	protected static boolean firstRun = true; // prevents invisible text

	public static void main(String[] args) {

		// CONFIGURE LOGGING
		// http://stackoverflow.com/questions/8043356/file-write-printstream-append
		// http://stackoverflow.com/questions/12053075/how-do-i-write-the-exception-from-printstacktrace-into-a-text-file-in-java
		try {
			// ps = new PrintStream(new FileOutputStream("DAR20161230_log.txt",
			// true));
			ps = new PrintStream(
					new FileOutputStream(System.getProperty("java.io.tmpdir") + "\\" + "DAR20161230_1_log.txt", true));
//			System.setOut(ps);
		} catch (Exception e) {
			e.printStackTrace(ps);
		}

		// https://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
		System.out.print("DAR Splitter launched: ");
		System.out.println(dateFormat.format(date));
		System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		minorln("Preferences file: " + preferences.getFileName());

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
		Timeline statusUpdateEvt = new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {

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

		statusUpdateEvt.setCycleCount(Timeline.INDEFINITE);
		statusUpdateEvt.play();

		// Scene
		Scene scene = new Scene(vbox, 800, 600); // w x h
		primaryStage.setScene(scene);
		primaryStage.show();

		// Trigger split button
		btnSplitDAR.fire();

		// TODO Automate the process if valid DAR is available
	}

	private TextLbl createTextFX(String descriptor, String prefString) {
		return createTextFX(Font.font("Calibri", FontWeight.NORMAL, 14), Color.DARKGREEN, descriptor, prefString);
	}

	private TextLbl createTextFX(Font f, Color c, String descriptor, String prefString) {
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
		// TODO
		@Override
		public void handle(ActionEvent e) {
			Task<Void> task = new Task<Void>() {
				@Override
				public Void call() {

					for (Button button : buttons) {
						button.setDisable(true);
					}
					minorln("Buttons disabled");

					// TODO: Determine why TextArea (TextDAR) is not updating if
					// prefs file failure occurs too early in program launch

					// Solves problem with invisible but selectable text
					if (DAR.firstRun) {
						firstRun = false;
						try {
							Thread.sleep(2000);
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}

						// Stack Trace for invisible but selectable text

						/*
						 * java.lang.NullPointerException at
						 * java.io.File.<init>(Unknown Source) at
						 * ca.tdsb.dunbar.dailyattendancereport.SejdaSupport.
						 * runMe(SejdaSupport.java:362) at
						 * ca.tdsb.dunbar.dailyattendancereport.
						 * DAR$SplitDARFcButtonListener$1.call(DAR.java:339) at
						 * ca.tdsb.dunbar.dailyattendancereport.
						 * DAR$SplitDARFcButtonListener$1.call(DAR.java:1) at
						 * javafx.concurrent.Task$TaskCallable.call(Task.java:
						 * 1423) at java.util.concurrent.FutureTask.run(Unknown
						 * Source) at java.lang.Thread.run(Unknown Source)
						 * java.lang.Exception: Program failure. Have all
						 * preferences been set? at
						 * ca.tdsb.dunbar.dailyattendancereport.SejdaSupport.
						 * runMe(SejdaSupport.java:367) at
						 * ca.tdsb.dunbar.dailyattendancereport.
						 * DAR$SplitDARFcButtonListener$1.call(DAR.java:339) at
						 * ca.tdsb.dunbar.dailyattendancereport.
						 * DAR$SplitDARFcButtonListener$1.call(DAR.java:1) at
						 * javafx.concurrent.Task$TaskCallable.call(Task.java:
						 * 1423) at java.util.concurrent.FutureTask.run(Unknown
						 * Source) at java.lang.Thread.run(Unknown Source) DAR
						 * Splitter launched: 2016/12/29 05:44:08
						 * C:\Users\094360\AppData\Roaming\DAR.config iiii
						 * round,round,round,round,round,
						 */

					}
					SejdaSupport r = null;
					try {
						r = new SejdaSupport(preferences, ps, programUpdatesFX);
						r.runMe(programUpdatesFX);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						if (e.getMessage() == null) {
							programUpdatesFX.prependTextWithDate("Possible problem with preferences. Please set them.");
						} else
							programUpdatesFX.prependTextWithDate(e.getMessage());
						e.printStackTrace(ps);
					}

					for (Button button : buttons) {
						button.setDisable(false);
					}
					DAR.minorln("Buttons enabled");

					return null;
				}
			};
			task.messageProperty()
					.addListener((obs, oldMessage, newMessage) -> programUpdatesFX.prependTextWithDate(newMessage));
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

	/**
	 * Class that extends TextArea to include a method to prepend date & time to
	 * the text.
	 * 
	 * @author 094360
	 *
	 */
	public class TextDAR extends TextArea {
		public final void prependTextWithDate(String s) {
			// https://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date(System.currentTimeMillis());
			String setT = dateFormat.format(date) + ": " + s + "\n" + this.getText();
			setText(setT);
			minorln(setT);
		}
	}

	/**
	 * Class that displays preference values in a pre-determined format in a
	 * JavaFX Text field.
	 * 
	 * @author 094360
	 *
	 */
	public class TextLbl extends Text {
		private String prefDescriptor;
		private String pref;

		public TextLbl(String descriptor, String pref) {
			this.prefDescriptor = descriptor;
			this.pref = pref;
			initialize();
		}

		/**
		 * Sets the Text contents to the description (similar to the key) of the
		 * property and the value of the property.
		 * 
		 * @param update
		 *            whether to include the notice (update)
		 */
		private void setTextValue(boolean update) {
			String s = "";
			String updateS = "";

			if (update)
				updateS = " (update)";
			s = prefDescriptor + updateS + ": " + DAR.preferences.getProperty(pref);
			this.setText(s);
		}

		/**
		 * Set the initial Text contents to forced value
		 */
		public void initialize() {
			setTextValue(false);
		}

		/**
		 * Force an update to the forced value of the Text contents
		 */
		public void update() {
			setTextValue(true);
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

	/**
	 * Provide a minor program update.
	 * 
	 * @param string
	 */
	public static void minor(String string) {
		System.out.print(string);
	}

	/**
	 * Provide a minor program update.
	 * 
	 * @param string
	 */
	public static void majorln(String string) {
		System.out.println(string);
	}

	public static void minorln(String string) {
		majorln("__" + string);
	}
	

	public static void msgBoxError(String title, String header, String content) { 
		msgBox(title, header, content, Alert.AlertType.ERROR);
	}
	
	public static void msgBoxInfo(String title, String header, String content) { 
		msgBox(title, header, content, Alert.AlertType.INFORMATION);
	}

	//http://stackoverflow.com/questions/11662857/javafx-2-1-messagebox
	public static void msgBox(String title, String header, String content, Alert.AlertType typeOfBox) { 
	    Platform.runLater(new Runnable() {
	        public void run() {
	    	    Alert alert = new Alert(typeOfBox);
	    	    alert.setTitle(title);
	    	    alert.setHeaderText(header);
	    	    alert.setContentText(content);
	    	    alert.showAndWait();
	        }
	      });
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