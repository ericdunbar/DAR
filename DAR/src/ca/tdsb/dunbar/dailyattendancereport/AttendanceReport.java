package ca.tdsb.dunbar.dailyattendancereport;

import java.io.File;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import ca.tdsb.dunbar.dailyattendancereport.SejdaSupport;
import ca.tdsb.dunbar.dailyattendancereport.DL;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * This class manages the separation of a single PDF of the daily attendance
 * record into individual PDFs representing each teacher's DAR. Two types of
 * attendance report as produced by Trillium are supported. A DAR "useful" and a
 * TCAR "useless" one. This convention is arbitrarily (and cheekily) chosen. The
 * useful report contains phone numbers and class attendance while the useless
 * report contains only a comment about the reason for an absence.
 * 
 * @author ED
 * @date 2017-01-13
 *
 */
public class AttendanceReport extends Application {

	// ||||||||||||||||||||||||||||||
	// __________PREFERENCES
	// ||||||||||||||||||||||||||||||
	/** Instance of a preferences-management program that tracks preferences */
	static DARProperties preferences = new DARProperties("AttendanceReport.config");

	/** Destination for successfully split individual PDFs */
	public final static String prefOutputPath = "output_path";

	/**
	 * Full path and name for the original "useful" type of AR. Contains all
	 * teacher DARs in a single file.
	 */
	public final static String prefMasterDAR = "master_DAR_PowerBuilder";

	/**
	 * Should a no-date PDF be created. Adds a third copy procedure to the split
	 * procedure.
	 */
	public final static String prefCreateNoDatePDF = "no_date_pdf";

	/**
	 * Should files be archived in a folder with the teacher's name?
	 */
	public final static String prefArchiveByTeacher = "archive_by_teacher";

	/**
	 * Full path and name for the original "useless" type of AR. Contains all
	 * teacher DARs in a single file.
	 */
	public final static String prefMasterTCAR = "master_TCAR_Teacher_Class_Attendance_";

	/** Directory in which sejda-console will run. */
	public final static String prefSejdaDirectory = "sejda";

	/** Full path and name of the sejda-console application */
	public final static String prefSejdaLocation = "sejda_console";

	// ||||||||||||||||||||||||||||||
	// __JavaFX objects and fields
	// ||||||||||||||||||||||||||||||
	private TextDAR programUpdatesFX;
	private TextLbl destinationDirFX;
	private TextLbl masterDARFX;
	private TextLbl masterTCARFX;
	public static final String versionDAR = "20170113";

	private static final String formTitleFX = "Daily Attendance Report processor version " + versionDAR;

	// private TextLbl clockFX;
	private TextLbl sedjaDARFX;
	private Button btnChooseMasterReports;
	private Button btnChooseDestDir;
	private Button btnChooseSedjaConsole;
	private Button btnSplitReports;
	private Button btnExit;

	private ButtonBase[] buttons;
	private ButtonBase[] settingsButtons;

	private CheckBox chkNoDate;
	private CheckBox chkArchiveByTeacher;

	private ToggleButton toggleChangeSettings;

	// GENERAL
	/**
	 * The two types of daily attendance record PDF. DAR is the Daily Attendance
	 * Report. TCAR is the Teacher Class Attendance Report.
	 * 
	 * @author ED
	 *
	 */
	enum ReportType {
		DAR, TCAR
	}

	// CLASS fields
	protected static boolean firstRun = true; // prevents invisible text

	private static void announceProgram() {
		// https://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		DL.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
		DL.println(" AR Splitter launched: " + dateFormat.format(date));
		DL.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
	}

	// TODO
	// http://stackoverflow.com/questions/13786968/java-fx-thread-safe-pause-sleep

	public static void main(String[] args) {
		// change to ttf for compile
		DL.startLogging(true, true, false);
		DL.methodBegin();

		announceProgram();

		Application.launch(args);

		DL.methodEnd();
	}

	@Override
	public void start(Stage primaryStage) {
		DL.methodBegin();
		DL.println("Preferences file: " + preferences.getPreferencesFileName());

		primaryStage.setTitle(formTitleFX);

		// Images

		// http://www.java2s.com/Code/Java/JavaFX/LoadajpgimagewithImageanduseImageViewtodisplay.htm
		// http://schoolweb.tdsb.on.ca/Portals/westway/images/eco%20icon.png
		ImageView ecoSchools = new ImageView();
		Image imgEcoschools = new Image(AttendanceReport.class.getResourceAsStream("eco_icon.png"));
		ecoSchools.setImage(imgEcoschools);
		ecoSchools.setFitHeight(80);
		ecoSchools.setPreserveRatio(true);
		HBox ecoHb = new HBox();
		ecoHb.setAlignment(Pos.CENTER_RIGHT);
		ecoHb.getChildren().addAll(ecoSchools);

		// Daily Attendance Report Title
		Label lblDAR = new Label("Daily Attendance Report");
		lblDAR.setTextFill(Color.DARKSLATEBLUE);
		lblDAR.setFont(Font.font("Calibri", FontWeight.BOLD, 36));

		// http://docs.oracle.com/javafx/2/text/jfxpub-text.htm
		DropShadow ds = new DropShadow();
		ds.setOffsetY(3.0f);
		ds.setColor(Color.color(0.4f, 0.4f, 0.4f));

		lblDAR.setEffect(ds);
		HBox labelHb = new HBox();
		labelHb.setAlignment(Pos.CENTER);
		labelHb.getChildren().add(lblDAR);

		// Actions
		HBox actionsHb = new HBox(10);
		actionsHb.setAlignment(Pos.CENTER_LEFT);

		// http://docs.oracle.com/javafx/2/layout/size_align.htm
		TilePane actionButtonsTP = new TilePane();
		actionButtonsTP.setAlignment(Pos.CENTER);

		Label lblActions = new Label("Perform actions:");
		lblActions.setTextFill(Color.DARKSLATEBLUE);
		lblActions.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		lblActions.setPrefWidth(100);
		lblActions.setMinWidth(100);
		actionsHb.getChildren().addAll(lblActions);

		btnSplitReports = new Button("_Split reports");
		btnSplitReports.setOnAction(new SplitDARFcButtonListener());
		btnSplitReports.setMnemonicParsing(true);
		actionButtonsTP.getChildren().addAll(btnSplitReports);

		btnExit = new Button("E_xit");
		btnExit.setOnAction(new ExitFcButtonListener());
		btnExit.setMnemonicParsing(true);
		actionButtonsTP.getChildren().addAll(btnExit);
		actionsHb.getChildren().addAll(actionButtonsTP);

		// Settings

		VBox settingsVb = new VBox(10);

		TilePane settingsButtonsTP = new TilePane();
		settingsButtonsTP.setAlignment(Pos.CENTER_LEFT);

		Label lblPrefs = new Label("Settings:");
		lblPrefs.setTextFill(Color.DARKSLATEBLUE);
		lblPrefs.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		lblPrefs.setPrefWidth(100);
		lblPrefs.setMinWidth(100);

		btnChooseMasterReports = new Button("Choose master report files...");
		btnChooseMasterReports.setOnAction(new SingleFcButtonListener());
		btnChooseMasterReports.setMnemonicParsing(true);
		settingsButtonsTP.getChildren().addAll(btnChooseMasterReports);

		btnChooseDestDir = new Button("Choose destination directory...");
		btnChooseDestDir.setOnAction(new DestinationDirFcButtonListener());
		btnChooseDestDir.setMnemonicParsing(true);
		settingsButtonsTP.getChildren().addAll(btnChooseDestDir);

		btnChooseSedjaConsole = new Button("Choose \"sedja-console\"...");
		btnChooseSedjaConsole.setOnAction(new SedjaDirFcButtonListener());
		btnChooseSedjaConsole.setMnemonicParsing(true);
		btnChooseSedjaConsole.setDisable(true);
		settingsButtonsTP.getChildren().addAll(btnChooseSedjaConsole);

		// Settings

		TilePane settingsOptionsTP = new TilePane();
		settingsOptionsTP.setAlignment(Pos.CENTER_LEFT);

		// http://docs.oracle.com/javafx/2/ui_controls/checkbox.htm
		chkNoDate = new CheckBox("Create a date-free PDF (extra copy)");
		chkNoDate.setOnAction(new ChkBoxNoDateListener());
		chkNoDate.setSelected(preferences.getProperty(AttendanceReport.prefCreateNoDatePDF).equals("true"));

		chkArchiveByTeacher = new CheckBox("Archive to folders with teacher name");
		chkArchiveByTeacher.setOnAction(new ChkBoxArchiveByTeacherListener());
		chkArchiveByTeacher.setSelected(preferences.getProperty(AttendanceReport.prefArchiveByTeacher).equals("true"));

		toggleChangeSettings = new ToggleButton("Change settings");
		toggleChangeSettings.setSelected(false);
		toggleChangeSettings.setOnAction(new ChkBoxSettingsListener());
		settingsButtons = new ButtonBase[] { btnChooseDestDir, btnChooseMasterReports, chkNoDate, chkArchiveByTeacher };

		settingsOptionsTP.getChildren().addAll(chkNoDate, chkArchiveByTeacher);

		settingsVb.getChildren().addAll(settingsButtonsTP, settingsOptionsTP);

		VBox settingsLblVb = new VBox(10);
		settingsLblVb.getChildren().addAll(lblPrefs, toggleChangeSettings);
		settingsLblVb.setMinWidth(150);
		settingsLblVb.setPrefWidth(150);

		HBox settingsHb = new HBox(10);
		settingsHb.getChildren().addAll(settingsLblVb, settingsVb);

		buttons = new ButtonBase[] { btnExit, btnSplitReports, toggleChangeSettings };

		// Status message text
		programUpdatesFX = new TextDAR();
		programUpdatesFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		programUpdatesFX.setText("");
		// http://stackoverflow.com/questions/19167750/control-keyboard-input-into-javafx-textfield
		programUpdatesFX.setEditable(false);
		programUpdatesFX.setWrapText(true);
		programUpdatesFX.setMaxHeight(150);

		// Status message text
		// Text statusFX = createTextFX(Font.font("Calibri", FontWeight.NORMAL,
		// 14), Color.DARKSLATEBLUE, "", "");
		// statusFX.setText("Status at");
		// statusFX.setWrappingWidth(0);
		//
		// clockFX = createTextFX(Font.font("Consolas", FontWeight.NORMAL, 12),
		// Color.DARKSLATEBLUE, "", "");
		// clockFX.setWrappingWidth(0);

		// had to disable to prevent problems with java.lang.Void, return null,
		// call() and hanging threads :(
		// ProgressIndicator pin = new ProgressIndicator();
		// pin.setProgress(-1);
		// pin.setVisible(false);
		//
		// HBox clockHb = new HBox(10);
		// clockHb.setAlignment(Pos.CENTER_LEFT);
		// clockHb.getChildren().addAll(statusFX, clockFX);

		// Source location
		masterDARFX = createTextFX(ReportType.DAR.toString() + " report file", prefMasterDAR);
		masterTCARFX = createTextFX(ReportType.TCAR.toString() + " report file", prefMasterTCAR);

		// Destination location
		destinationDirFX = createTextFX("Destination directory", prefOutputPath);

		// sejda-console location
		sedjaDARFX = createTextFX("sedja app", (prefSejdaLocation));

		// http://stackoverflow.com/questions/19968012/javafx-update-ui-label-asynchronously-with-messages-while-application-different

		// Separator
		int idx = 3;
		int counter = 0;
		Separator sep[] = new Separator[idx];

		for (int i = 0; i < idx; i++) {
			sep[i] = new Separator();
			sep[i].setOrientation(Orientation.HORIZONTAL);
		}

		// Vbox
		VBox vbox = new VBox(15);
		vbox.setPadding(new Insets(25, 25, 25, 25));

		vbox.getChildren().addAll(lblDAR, sep[counter++], actionsHb, sep[counter++], settingsHb, sep[counter++],
				programUpdatesFX, masterDARFX, masterTCARFX, destinationDirFX, sedjaDARFX, ecoHb);

		// vbox.getChildren().addAll(lblDAR, sep[counter++], actionsHb,
		// sep[counter++], preferencesHb, sep[counter++],
		// settingsHb, sep[counter++], programUpdatesFX, clockHb,
		// masterUsefulDARFX, masterUselessDARFX,
		// destinationDirFX, sedjaDARFX, ecoHb);

		// Create and display idle/working

		// Timeline statusUpdateEvt = new Timeline(new
		// KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {
		//
		// private int dots = 0;
		//
		// // TODO Eliminate the use of message boxes. They cause the
		// // odd failure when using this action event.
		// @Override
		// public void handle(ActionEvent event) {
		// // DL.methodBegin();
		// DateFormat dateFormat = new SimpleDateFormat("HH:mm");
		// Date date = new Date(System.currentTimeMillis());
		// String workingS = "idle";
		//
		// //TODO Remove and remove the block on delete master DAR
		//// try {
		//// Thread.sleep(2000);
		//// } catch (InterruptedException e) {
		//// // TODO Auto-generated catch block
		//// e.printStackTrace();
		//// }
		// if (DAR.working) {
		// char ca[] = new String("|/-\\").toCharArray();
		// int numDots = ca.length;
		//
		// workingS = "processing " + ca[dots % numDots];
		// // pin.setVisible(true);
		// // int numDots = 5;
		// // workingS = "processing [";
		// // for (int i = 0; i < dots % numDots; i++)
		// // workingS += "=";
		// // workingS += ">";
		// // for (int i = 0; i < numDots - dots % numDots; i++)
		// // workingS += " ";
		// // workingS += "]";
		// dots++;
		// }
		// // else
		// // pin.setVisible(false);
		//
		// clockFX.setText(dateFormat.format(date) + ": " + workingS);
		// // DL.methodEnd();
		// }
		// }));
		//
		// statusUpdateEvt.setCycleCount(Timeline.INDEFINITE);
		// statusUpdateEvt.play();

		// Scrolling
		// http://stackoverflow.com/questions/30971407/javafx-is-it-possible-to-have-a-scroll-bar-in-vbox
		// http://stackoverflow.com/questions/30390986/how-to-disable-horizontal-scrolling-in-scrollbar-javafx/30392217#30392217

		ScrollPane scrollTheVBox = new ScrollPane();
		scrollTheVBox.setFitToWidth(true);
		// scrollTheVBox.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		scrollTheVBox.setContent(vbox);

		// Scene
		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

		Scene scene = new Scene(scrollTheVBox, 800, 700); // w x h
		primaryStage.setScene(scene);

		// http://www.java2s.com/Code/Java/JavaFX/GetScreensize.htm
		if (primaryScreenBounds.getHeight() < 720) {
			primaryStage.setMaximized(true);
			;
		}
		primaryStage.show();

		// Trigger split button
		// ChkBoxSettingsListener c = new ChkBoxSettingsListener();
		// c.handle(new ActionEvent());
		// c = null;

		toggleChangeSettings.fireEvent(new ActionEvent());
		btnSplitReports.fire();

		DL.methodEnd();
	}

	private TextLbl createTextFX(String descriptor, String prefString) {
		return createTextFX(Font.font("Calibri", FontWeight.NORMAL, 14), Color.DARKGREEN, descriptor, prefString);
	}

	private TextLbl createTextFX(Font f, Color c, String descriptor, String prefString) {
		TextLbl newTextFX = new TextLbl(descriptor, prefString);
		newTextFX.setFont(f);
		newTextFX.setFill(c);
		newTextFX.setWrappingWidth(699); // 699 avoid a horizontal scroll bar
		return newTextFX;
	}

	// TODO Describe SplitDARFcB
	private class SplitDARFcButtonListener implements EventHandler<ActionEvent> {
		// http://stackoverflow.com/questions/26554814/javafx-updating-gui
		// http://stackoverflow.com/questions/19968012/javafx-update-ui-label-asynchronously-with-messages-while-application-different

		// TODO Correct for the false missing in runMe
		@Override
		public void handle(ActionEvent e) {
			Task<Void> task = new Task<Void>() {
				@Override
				public Void call() {
					// http://stackoverflow.com/questions/10839042/what-is-the-difference-between-java-lang-void-and-void
					DL.methodBegin();
					for (ButtonBase button : buttons) {
						button.setDisable(true);
					}
					toggleChangeSettings.fireEvent(new ActionEvent());

					DL.println("Buttons disabled");

					// TODO: Determine why TextArea (TextDAR) is not updating if
					// prefs file failure occurs too early in program launch

					// Solves problem with invisible but selectable text
					if (AttendanceReport.firstRun) {
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
						 * C:\Users\ED\AppData\Roaming\DAR.config iiii
						 * round,round,round,round,round,
						 */

					}
					SejdaSupport r = null;
					try {
						r = new SejdaSupport(preferences, programUpdatesFX);
					} catch (Exception e) {
						btnChooseSedjaConsole.setDisable(false);
						if (e.getMessage() == null) {
							String msg = "Possible problem with preferences. Please set them.";
							programUpdatesFX.prependTextWithDate(msg);
							msgBoxError("Preferences Problem Detected", "Possible problem with preferences.", msg);
							e.printStackTrace();
						} else {
							programUpdatesFX.prependTextWithDate(e.getMessage());
							msgBoxError("Error encountered",
									"An error was encountered at the start of the split process.", e.getMessage());
						}
						toggleChangeSettings.setSelected(true);
						e.printStackTrace();
					}
					if (r != null)
						r.runMe(programUpdatesFX);

					for (ButtonBase button : buttons) {
						button.setDisable(false);
					}

					toggleChangeSettings.fireEvent(new ActionEvent());
					DL.println("Buttons enabled");

					DL.methodEnd();
					// could this be the cause of status update headaches?
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
			showDARFileChooser();
		}
	}

	private class DestinationDirFcButtonListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			showDestinationDirChooser();
		}
	}

	private class SedjaDirFcButtonListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			showSejdaChooser();
		}
	}

	private class ExitFcButtonListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			System.exit(0);
		}
	}

	private class ChkBoxSettingsListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			DL.methodBegin();
			DL.println(e.toString());
			DL.println("" + e.getSource());
			for (ButtonBase button : settingsButtons) {
				boolean setDisabled = toggleChangeSettings.isDisabled() || !toggleChangeSettings.isSelected();
				button.setDisable(setDisabled);
			}
			DL.methodEnd();
		}
	}

	private class ChkBoxArchiveByTeacherListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			preferences.setProperty(prefArchiveByTeacher, "" + chkArchiveByTeacher.isSelected());
		}
	}

	private class ChkBoxNoDateListener implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			preferences.setProperty(prefCreateNoDatePDF, "" + chkNoDate.isSelected());
		}
	}

	/**
	 * Class that extends TextArea to include a method to prepend date & time to
	 * the text.
	 * 
	 * @author ED
	 *
	 */
	public class TextDAR extends TextArea {
		public final void prependTextWithDate(String s) {
			// https://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date(System.currentTimeMillis());
			String setT = dateFormat.format(date) + ": " + s + "\n" + this.getText();
			setText(setT);
			DL.println(setT);
		}
	}

	/**
	 * Class that displays preference values in a pre-determined format in a
	 * JavaFX Text field.
	 * 
	 * @author ED
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
		 * property using the key (pref) and the value of the property.
		 * 
		 * @param update whether to include the notice (update)
		 */
		private void setTextValue(boolean update) {
			String s = "";
			String updateS = "";

			if (update)
				updateS = " (update)";
			s = prefDescriptor + updateS + ": " + AttendanceReport.preferences.getProperty(pref);
			this.setText(s);
		}

		/**
		 * Set the initial Text contents to value as stored in the preferences
		 * file.
		 */
		public void initialize() {
			setTextValue(false);
		}

		/**
		 * Force an update to the value of the Text contents and indicate that
		 * an update was forced.
		 */
		public void update() {
			setTextValue(true);
		}
	}

	/**
	 * Choose the sejda-console application.
	 */
	private void showSejdaChooser() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(System.getenv("userprofile")));
		fileChooser.setTitle(
				"Select and open \"sedja-console\" from within \"sejda-console-2.10.4-bin\\sejda-console-2.10.4\\bin\"");
		// fileChooser.setSelectedExtensionFilter(filter);
		File selectedFile = fileChooser.showOpenDialog(null);

		if (selectedFile != null) {
			preferences.setProperty(prefSejdaDirectory, selectedFile.getParent());
			preferences.setProperty(prefSejdaLocation, selectedFile.getAbsolutePath());
			sedjaDARFX.update();
		}
	}

	public static void msgBoxError(String title, String header, String content) {
		msgBox(title, header, content, Alert.AlertType.ERROR);
	}

	public static void msgBoxInfo(String title, String header, String content) {
		msgBox(title, header, content, Alert.AlertType.INFORMATION);
	}

	// http://stackoverflow.com/questions/11662857/javafx-2-1-messagebox

	public static void msgBox(String title, String header, String content, Alert.AlertType typeOfBox) {
		Platform.runLater(new Runnable() {

			public void run() {
				DL.methodBegin();

				Alert alert = new Alert(typeOfBox);
				alert.setTitle(title);
				alert.setHeaderText(header);
				alert.setContentText(content);

				// TODO Testing .show vs .showAndWait
				alert.show();

				DL.methodEnd();
			}
		});
	}

	static class DARFileChooserClass {

		public void msgBox2(String title, String header, String content, Alert.AlertType typeOfBox) {
			DL.methodBegin();
			DL.println("content = " + content);

			Platform.runLater(new Runnable() {

				public void run() {
					Alert alert = new Alert(typeOfBox);
					alert.setTitle(title);
					alert.setHeaderText(header);
					alert.setContentText(content);

					// TODO Testing .show vs. .showAndWait for thread hanging
					// purposes.
					alert.show();
				}
			});
			DL.methodEnd();
		}

		public File DARFileChooser(String DARFileName, ReportType dT) {
			DL.methodBegin();
			FileChooser fileChooser = new FileChooser();
			fileChooser.setInitialDirectory(new File(System.getenv("userprofile")));
			File selectedFile = null;
			fileChooser.setTitle("Open " + dT.toString() + " attendance report, likely called \"" + DARFileName + "\"");

			selectedFile = fileChooser.showOpenDialog(null);

			DL.methodEnd();
			return selectedFile;
		}
	}

	private void showDARFileChooser() {
		File selectedFile, selectedFile2;
		DARFileChooserClass dfc = new DARFileChooserClass();
		selectedFile = dfc.DARFileChooser("PowerBuilder.pdf", ReportType.DAR);
		if (selectedFile != null) {
			preferences.setProperty(prefMasterDAR, selectedFile.getAbsolutePath());
			masterDARFX.update();
		}

		dfc = new DARFileChooserClass();
		selectedFile2 = dfc.DARFileChooser("Teacher Class Attendance .pdf", ReportType.TCAR);
		if (selectedFile2 != null) {
			preferences.setProperty(prefMasterTCAR, selectedFile2.getAbsolutePath());
			masterTCARFX.update();
		}

		// TODO: MESSY. AM TOO TIRED TO FIX

		String errorMsg = "";
		if (!selectedFile.getName().equals("PowerBuilder.pdf")) {
			errorMsg = "\"" + "PowerBuilder.pdf" + "\"" + " was expected. \"" + selectedFile.getName() + "\""
					+ " was chosen. ";
		}
		if (!selectedFile2.getName().equals("Teacher Class Attendance .pdf")) {
			errorMsg = errorMsg + "\"" + "Teacher Class Attendance .pdf" + "\"" + " was expected. \""
					+ selectedFile2.getName() + "\"" + " was chosen. ";
		}

		if (!errorMsg.equals("")) {
			dfc.msgBox2("Confirm selected PDF", "Unexpected file(s) chosen",
					errorMsg + " \n\nConfirm that the correct files were chosen. If they weren't, please use the \"Choose master report files...\" to choose the correct files.",
					Alert.AlertType.WARNING);
		}
	}

	/**
	 * Obtains and sets the destination directory.
	 * 
	 */
	private void showDestinationDirChooser() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Pick destination directory for split DARs. Hint: use a folder on TeacherShare.");
		File defaultDirectory = new File(System.getenv("userprofile") + "\\Desktop");
		chooser.setInitialDirectory(defaultDirectory);
		File selectedDirectory = chooser.showDialog(null);

		if (selectedDirectory != null) {
			String messageS = null;
			messageS = selectedDirectory.getAbsolutePath();
			preferences.setProperty(prefOutputPath, messageS);
			destinationDirFX.update();
		}
	}
}