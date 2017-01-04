package ca.tdsb.dunbar.dailyattendancereport;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import ca.tdsb.dunbar.dailyattendancereport.DL;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;


public class Prob extends Application {

	// ||||||||||||||||||||||||||||||
	// __JavaFX objects and fields
	// ||||||||||||||||||||||||||||||
	private TextDAR programUpdatesFX;

	private TextLbl clockFX;

	private Button btnSplitDAR;

	private ButtonBase[] buttons;


	// CLASS fields
	static boolean working = false; // disable buttons while processing
	protected static boolean firstRun = true; // prevents invisible text


	public static void main(String[] args) {
		DL.methodBegin();

		Application.launch(args);

		DL.methodEnd();
	}

	@Override
	public void start(Stage primaryStage) {
		DL.methodBegin();

		primaryStage.setTitle("Title");

		// http://docs.oracle.com/javafx/2/layout/size_align.htm
		btnSplitDAR = new Button("_Split master DAR");
		btnSplitDAR.setOnAction(new SplitDARFcButtonListener());
		btnSplitDAR.setMnemonicParsing(true);

		buttons = new ButtonBase[] {    btnSplitDAR  };

		// Status message text
		programUpdatesFX = new TextDAR();
		programUpdatesFX.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
		programUpdatesFX.setText("");
		// http://stackoverflow.com/questions/19167750/control-keyboard-input-into-javafx-textfield
		programUpdatesFX.setEditable(false);
		programUpdatesFX.setWrapText(true);
		programUpdatesFX.setMaxHeight(150);

		// Status message text
		clockFX = createTextFX(Font.font("Consolas", FontWeight.NORMAL, 12), Color.DARKSLATEBLUE, "", "");
		clockFX.setWrappingWidth(0);

		// had to disable to prevent problems with java.lang.Void, return null,
		// call() and hanging threads :(
		// ProgressIndicator pin = new ProgressIndicator();
		// pin.setProgress(-1);
		// pin.setVisible(false);
		//]
		// http://stackoverflow.com/questions/19968012/javafx-update-ui-label-asynchronously-with-messages-while-application-different

		// Separator

		// Vbox
		VBox vbox = new VBox(15);
		vbox.setPadding(new Insets(25, 25, 25, 25));

		vbox.getChildren().addAll( btnSplitDAR, programUpdatesFX, clockFX);

		// Create and display idle/working
		Timeline statusUpdateEvt = new Timeline(new KeyFrame(Duration.millis(2000), new EventHandler<ActionEvent>() {

			private int dots = 0;

			// TODO Eliminate the use of message boxes. They cause the
			// odd failure when using this action event.
			@Override
			public void handle(ActionEvent event) {
				// DL.methodBegin();
				DateFormat dateFormat = new SimpleDateFormat("HH:mm");
				Date date = new Date(System.currentTimeMillis());
				String workingS = "idle";
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (DAR.working) {
					char ca[] = new String("|/-\\").toCharArray();
					int numDots = ca.length;

					workingS = "processing " + ca[dots % numDots];
					// pin.setVisible(true);
					// int numDots = 5;
					// workingS = "processing [";
					// for (int i = 0; i < dots % numDots; i++)
					// workingS += "=";
					// workingS += ">";
					// for (int i = 0; i < numDots - dots % numDots; i++)
					// workingS += " ";
					// workingS += "]";
					dots++;
				}
				// else
				// pin.setVisible(false);

				clockFX.setText(dateFormat.format(date) + ": " + workingS);
				// DL.methodEnd();
			}
		}));

		statusUpdateEvt.setCycleCount(Timeline.INDEFINITE);
		statusUpdateEvt.play();

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
		btnSplitDAR.fire();

		DL.methodEnd();
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

		// TODO
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

					// Solves problem with invisible but selectable text
					if (DAR.firstRun) {
						firstRun = false;
						try {
							Thread.sleep(2000);
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}


					}
					try {
						throw new Exception();
					} catch (Exception e) {
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
						e.printStackTrace();
					}

					for (ButtonBase button : buttons) {
						button.setDisable(false);
					}

					DL.methodEnd();
					return null;
				}
			};
			task.messageProperty()
					.addListener((obs, oldMessage, newMessage) -> programUpdatesFX.prependTextWithDate(newMessage));
			new Thread(task).start();
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
			s = prefDescriptor + updateS + ": ";
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
				alert.showAndWait();

				DL.methodEnd();
			}
		});
	}
}