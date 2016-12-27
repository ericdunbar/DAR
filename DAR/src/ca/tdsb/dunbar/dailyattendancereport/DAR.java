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

	// TODO: describe TextDAR and MOVE
	public class TextDAR extends TextArea {
		public final void setTextWithDate(String s) {
			// https://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date(System.currentTimeMillis());
			setText( dateFormat.format(date) + ": " + s + "\n" + this.getText());
		}
	}

	// JavaFX variables
	private TextDAR errorStatusFX;
	private Text destinationDirFX;
	private Text masterUsefulDARFX;
	private Text masterUselessDARFX;

	private static final String formTitleFX = "Daily Attendance Report processor version 2016.12.24";

	// PREFERENCES variables
	DARProperties preferences = new DARProperties("DAR.config");

	public final static String prefInputPath = "input_path";
	public final static String prefOutputPath = "output_path";
	public final static String prefMasterUsefulDAR = "master_useful_DAR_PowerBuilder";
	public final static String prefMasterUselessDAR = "master_useless_DAR_Teacher_Class_Attendance_";
	public final static String prefSejdaLocation = "sejda";
	private Text clockFX;

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
	static boolean working = false;

	public static void main(String[] args) {

		// http://stackoverflow.com/questions/8043356/file-write-printstream-append
		// http://stackoverflow.com/questions/12053075/how-do-i-write-the-exception-from-printstacktrace-into-a-text-file-in-java
		try {
			ps = new PrintStream(
					new FileOutputStream(System.getProperty("java.io.tmpdir") + "\\" + "DAR_log.txt", true));
			System.setOut(ps);
		} catch (Exception e) {
			e.printStackTrace(ps);
		}

		// https://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		System.out.print("Launched at: ");
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
		errorStatusFX = new TextDAR();
		errorStatusFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		//TODO Should this be a TextArea?
		//errorStatusFX.setFill(Color.FIREBRICK);
		errorStatusFX.setTextWithDate("No actions taken yet.");

		// Status message text
		clockFX = new Text();
		clockFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		clockFX.setFill(Color.DARKSLATEBLUE);
		clockFX.setText("");

		// Destination message text
		masterUsefulDARFX = new Text();
		masterUsefulDARFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		masterUsefulDARFX.setFill(Color.DARKGREEN);
		masterUsefulDARFX.setText(
				"Master " + DARType.Useful.toString() + " DAR:\n    " + preferences.getProperty(prefMasterUsefulDAR));

		// Destination message text
		masterUselessDARFX = new Text();
		masterUselessDARFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		masterUselessDARFX.setFill(Color.DARKGREEN);
		masterUselessDARFX.setText(
				"Master " + DARType.Useful.toString() + " DAR:\n    " + preferences.getProperty(prefMasterUselessDAR));

		// Destination message text
		destinationDirFX = new Text();
		destinationDirFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		destinationDirFX.setFill(Color.DARKGREEN);
		destinationDirFX.setText("Destination directory:\n    " + preferences.getProperty(prefOutputPath));

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

		vbox.getChildren().addAll(labelHb, buttonHb4, separator2, buttonHb1, buttonHb3, separator3, buttonHb5,
				errorStatusFX, clockFX, masterUsefulDARFX, masterUselessDARFX, destinationDirFX);

		// Create clock
		Timeline fiveSecondsWonder = new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {

			private int dots = 0;

			@Override
			public void handle(ActionEvent event) {
				DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
				Date date = new Date(System.currentTimeMillis());
				String workingS = "idle";
				if (DAR.working) {
					workingS = "processing";
					for (int i = 0; i < dots % 6; i++) {
						workingS += ".";
					}
					dots++;
				}

				clockFX.setText("Current time:\n  " + dateFormat.format(date) + "\nStatus:\n  " + workingS);

			}
		}));

		fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
		fiveSecondsWonder.play();

		// Scene
		Scene scene = new Scene(vbox, 600, 700); // w x h
		primaryStage.setScene(scene);
		primaryStage.show();

		// TODO Automate the process if valid DAR is available
		boolean success = false;

		if (success)
			Platform.exit();
	}

	// TODO Describe resetErrorStatus
	private void resetErrorStatus() {
		//TODO What does this do?
//		errorStatusFX.setTextWithDate("");
	}

	// TODO Describe SplitDARFcB
	private class SplitDARFcButtonListener implements EventHandler<ActionEvent> {
		//// http://stackoverflow.com/questions/26554814/javafx-updating-gui

		@Override
		public void handle(ActionEvent e) {
			Task<Void> task = new Task<Void>() {
				@Override
				public Void call() {
					SejdaSupport r = new SejdaSupport(preferences, ps);
					r.runMe(errorStatusFX);
					return null;
				}
			};
			task.messageProperty()
					.addListener((obs, oldMessage, newMessage) -> errorStatusFX.setTextWithDate(newMessage));
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
			masterUsefulDARFX.setText("Master " + DARType.Useful.toString() + " DAR (updated):\n    "
					+ preferences.getProperty(prefMasterUsefulDAR));
		}

		fileChooser.setTitle("Open " + DARType.Useless.toString() + " DAR, aka \"Teacher Class Attendance .pdf\"");
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
			destinationDirFX.setText("Destination directory:\n    " + messageS);
		}
	}
}