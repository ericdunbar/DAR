package ca.tdsb.dunbar.dailyattendancereport;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import ca.tdsb.dunbar.dailyattendancereport.AttendanceReport.ReportType;
import ca.tdsb.dunbar.dailyattendancereport.AttendanceReport.TextDateAndTime;

public class SejdaSupport {

	/**
	 * Creates an instance. Preferences are checked to ensure validity. Throws
	 * IOExceptions if preferences are missing or target files are missing. Null
	 * is returned if any IOExceptions occur.
	 * 
	 * @param prefs An object of type DARProperties responsible for managing
	 *            file locations preferences
	 * @param msgFX A TextArea (TextDAR) JavaFX node for status updates.
	 * @throws IOException File not found
	 */
	public SejdaSupport(DARProperties prefs, TextDateAndTime msgFX) throws IOException {
		// Initialize variables
		this.preferences = prefs;
		this.messageFX = msgFX;

		// Check for the existence of the preferences file
		if (!this.preferences.exists())
			throw new IOException(
					"Please set preferences. Without valid preferences this program is not useful.");

		// TODO To copy sejda to the correct location

		// TODO Logic of copying sejda
		// 1. If file is null try copying the sejda directory to //LOCALAPPDATA
		// and set preference
		// 2. If copy fails or file not found tell user to manually set the
		// sejda location

		// Check for the preference sejda-console
		String sejdaFS = prefs.getProperty(AttendanceReport.prefSejdaLocation);
		String sejdaInfo = "The file will be located at the end of this path: sejda-console-2.10.4\\bin\\sejda-console.\n\nThis program was developed using sejda-console v. 2.10.4. A newer version may work. For downloads, visit http://www.sejda.org/download/";
		if (sejdaFS == null) {
			sejdaFS = copySejdaToLocalAppData();
		}

		// Check for the existence of sejda-console or whether the chosen file
		// is sejda-console.
		File sejdaF = new File(sejdaFS);

		if (!sejdaF.exists() || !sejdaF.getName().equals("sejda-console")) {
			sejdaFS = copySejdaToLocalAppData();

			sejdaF = new File(sejdaFS);

			// The following is likely dead code since the above line will take
			// care of setting the preference and throw an exception
			// if the copy fails
			if (!sejdaF.exists() || !sejdaF.getName().equals("sejda-console")) {
				throw new IOException(
						"\"sejda-console\" is unavailable. Specify its location using the \"Choose sedja-console...\" button. \n\n"
								+ sejdaInfo);
			}
		}

		String outPath = preferences.getProperty(AttendanceReport.prefOutputPath);

		// Set the default hard-coded destination directory
		if (outPath == null) {
			outPath = "\\\\Tdsbshares\\schadmin$\\1392\\1392-TCH\\Admin\\Daily Attendance";
			preferences.setProperty(AttendanceReport.prefOutputPath, outPath);
		}

		// Check to see if the destination directory exists and warn user if it
		// doesn't
		if (!(new File(outPath).exists())) {
			throw new IOException(
					"Destination directory unavailable.\n\nIf the directory is correct (confirm the dir) you will want to wait until TeacherShare becomes available. "
							+ "\n\nIf the network is functioning properly or you do not wish to use a network directory, please set the destination (output) directory using the \"Choose destination directory...\" button.");
		}

		// Check to see if the daily attendance report settings have been set

	}

	private String copySejdaToLocalAppData() throws IOException {
		// 1. file location is null so try copying \sejda-console to
		// \\LOCALAPPDATA
		
		messageFX.prependTextWithDate("Copying support files. This only happens the first time the program is run.");

		File masterSejdaDir = new File("sejda-console-2.10.4");

		// allow for flexibility in future versions of sejda provided the
		// version number is removed
		if (!masterSejdaDir.exists())
			masterSejdaDir = new File("sejda-console");

		// throw exception if sejda-console missing
		if (!masterSejdaDir.exists())
			throw new IOException(
					"sejda-console or sejda-console-2.10.4. Please download sejda-console and make sure the uncompressed sejda-console or sejda-console-2.10.4 directory is in the same directory as this application");

		File destDir = new File(System.getenv("LOCALAPPDATA") + "\\" + masterSejdaDir.getName());

		// 1. a. Recurse through the directory and copy each file
		FileUtils.copyDirectory(masterSejdaDir, destDir);

		String sejdaFS = destDir.getAbsolutePath() + "\\bin\\sejda-console";
		preferences.setProperty(AttendanceReport.prefSejdaLocation, sejdaFS);

		return sejdaFS;
	}

	TextDateAndTime messageFX; // display status updates here
	File sejdaF; // sejda-console
	DARProperties preferences;

	/**
	 * Splits the report.
	 * 
	 * @throws IOException
	 */
	public void splitReport(ReportType whichReport) throws IOException {
		DL.methodBegin();
		DL.println("whichReport = " + whichReport.toString());

		/*
		 * List of tasks to do: 1. prepare temporary directory by emptying it 2.
		 * prepare arguments to pass to sejda for DATE getDateFromPDF(File f) 3.
		 * run sejda for DATE 4. read DATE into a variable dateVar 5. write
		 * dateVar to file 6. delete PDF
		 */

		String sejdaS = preferences.getProperty(AttendanceReport.prefSejdaLocation);

		// 0. PREPARE SEDJA

		// 1. TEXT COORDINATES & type of preferences file
		// String c[] = {"--top", "--left", "--width", "--height"};

		String localPrefMasterReport;

		String teacherNameCoordinates[];

		String describeReport = whichReport.toString();

		if (whichReport == ReportType.DAR) {
			localPrefMasterReport = AttendanceReport.prefMasterDAR;

			// DAR split by TEACHERNAME
			teacherNameCoordinates = new String[] { "86", "84", "200", "22" };
		} else {
			localPrefMasterReport = AttendanceReport.prefMasterTCAR;

			// TCAR split by TEACHERNAME
			teacherNameCoordinates = new String[] { "105", "71", "200", "22" };
		}

		// SOURCE FILE
		String sourceFile = preferences.getProperty(localPrefMasterReport);

		// DETERMINE DATE
		// Throws IOException
		String dateForReport = getPDFDate(sourceFile, whichReport);

		File tFile = new File(dateForReport);
		if (tFile.getName().equalsIgnoreCase("date_error")) {
			throw new IOException("The report file \"" + new File(sourceFile).getName()
					+ "\" is not the correct file for the " + whichReport.toString() + " report.");
		}
		DL.println("THE DATE: " + dateForReport);

		// DESTINATION DIRECTORY
		// Prepare temporary directory
		File tempDir = createTempDir();
		if (tempDir == null)
			throw new IOException("TEMP directory could not be created. Try again.");

		String destinationDirectory = tempDir.getAbsolutePath();

		// PERFORM THE SPLIT

		// http://stackoverflow.com/questions/357315/get-list-of-passed-arguments-in-windows-batch-script-bat
		// http://stackoverflow.com/questions/19103570/run-batch-file-from-java-code

		List<String> cmdAndArgs = Arrays.asList("cmd", "/c", "call", quotesWrap(sejdaS),
				"splitbytext", "-f", quotesWrap(sourceFile), "--top", teacherNameCoordinates[0],
				"--left", teacherNameCoordinates[1], "--width", teacherNameCoordinates[2],
				"--height", teacherNameCoordinates[3], "-o", quotesWrap(destinationDirectory), "-p",
				"[TEXT]", "--existingOutput", "overwrite");

		sejdaSplitReport(cmdAndArgs, tempDir);

		// boolean simpleCopy =
		// preferences.getProperty(AttendanceReport.prefCreateNoDatePDF).equals("true");

		// DL.println("simpleCOPY: " + simpleCopy);

		// CREATE DIRECTORIES, if missing
		// Attempted fix for constantly refreshing archive directory
		File archiveDir = new File(
				preferences.getProperty(AttendanceReport.prefOutputPath) + "\\Archive\\");
		archiveDir.mkdir();
		archiveDir = new File(preferences.getProperty(AttendanceReport.prefOutputPath)
				+ "\\Archive\\" + dateForReport);
		archiveDir.mkdir();

		File currentDir = new File(
				preferences.getProperty(AttendanceReport.prefOutputPath) + "\\Current\\");
		currentDir.mkdir();

		// DELETE EXISTING FILES
		// http://stackoverflow.com/questions/5751335/using-file-listfiles-with-filenameextensionfilter
		// File ftbd = new
		// File(preferences.getProperty(AttendanceReport.prefOutputPath));
		//
		int delF = deleteFilesOfReportType(currentDir, whichReport, ".pdf");

		messageFX.prependTextWithDate("Cleaned up " + currentDir.getAbsolutePath() + " by removing "
				+ delF + " " + whichReport.toString() + " files.");

		// ftbd = new
		// File(preferences.getProperty(AttendanceReport.prefOutputPath) + "\\No
		// Date\\");
		//
		// if (!simpleCopy && ftbd.exists()) {
		// deleteDir(ftbd);
		// }

		// if (ftbd.exists()) {
		// int delG = deleteFilesOfReportType(ftbd, whichReport, ".pdf");
		// messageFX.prependTextWithDate(
		// "Cleaned up destination NO DATE by removing " + delG + " " +
		// whichReport.toString() + " files.");
		// }

		// MOVE and COPY PDFs

		String newFileList[] = tempDir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				return name.toLowerCase().endsWith(".pdf");
			}
		});

		DL.println("FILE LIST FOR: " + tempDir.getAbsolutePath());

		for (String string : newFileList) {
			DL.println("    " + tempDir.getAbsolutePath() + "\\" + string);
		}

		DL.println("Start: MOVE and ARCHIVE NEW FILES");
		messageFX.prependTextWithDate("Will move and create archives for " + newFileList.length
				+ " " + whichReport.toString() + " files in "
				+ preferences.getProperty(AttendanceReport.prefOutputPath));
		for (String s : newFileList) {
			String oldFile = tempDir.getAbsolutePath() + "\\" + s;

			String simpleFileName = FilenameUtils.getBaseName(s) + " " + describeReport + ".pdf";
			String fileNameDateAndType = FilenameUtils.getBaseName(s) + " " + describeReport + " "
					+ dateForReport + ".pdf";

			// if (simpleCopy) {
			// String simpleFile = simpleDir.getAbsolutePath() + "\\" +
			// simpleName;
			// reportCopyFile(oldFile, simpleFile);
			// }

			// Move into a folder labelled by teacher's name
			if (preferences.getProperty(AttendanceReport.prefArchiveByTeacher).equals("true")) {
				String newBaseName = FilenameUtils.getBaseName(s).toUpperCase();

				String byTeacherDirName = preferences.getProperty(AttendanceReport.prefOutputPath)
						+ "\\By Teacher\\";

				String newArchivalFile = byTeacherDirName + newBaseName + "\\"
						+ fileNameDateAndType;

				try {
					reportCopyFile(oldFile, newArchivalFile);
				} catch (Exception e) {

					File byTeacherDir = new File(byTeacherDirName);
					byTeacherDir.mkdir();
					byTeacherDir = new File(preferences.getProperty(AttendanceReport.prefOutputPath)
							+ "\\By Teacher\\" + newBaseName);
					byTeacherDir.mkdir();

					reportCopyFile(oldFile, newArchivalFile);
				}

				String simpleFile = byTeacherDirName + newBaseName + "\\" + simpleFileName;
				reportCopyFile(oldFile, simpleFile);

				// reportCopyFile(oldFile, newFile);
			}

			// move into one folder where all teachers are listed
			// String newFile =
			// preferences.getProperty(AttendanceReport.prefOutputPath) + "\\" +
			// newArchivalName;
			String archivalFile = archiveDir.getAbsolutePath() + "\\" + fileNameDateAndType;
			String currentFile = currentDir.getAbsolutePath() + "\\" + fileNameDateAndType;

			reportMoveFile(oldFile, currentFile, archivalFile);

		}

		messageFX.prependTextWithDate("Moved " + newFileList.length + " new "
				+ whichReport.toString() + " teacher report PDF files " + "for " + dateForReport
				+ " to: " + currentDir.getAbsolutePath()
				+ ", and to individual teacher directories.");

		// MOVE master REPORT to ARCHIVE
		String FROM = preferences.getProperty(localPrefMasterReport);
		String TO = preferences.getProperty(AttendanceReport.prefOutputPath)
				+ "\\Archive\\Masters\\" + "Master_Report_" + describeReport + "_" + dateForReport
				+ ".pdf";
		reportMoveFile(FROM, TO);
		messageFX.prependTextWithDate("Archived the master report PDF for " + describeReport
				+ " for " + dateForReport + ".");
		DL.methodEnd();
	}

	/**
	 * Splits the PDF given the String array of DOS command-line arguments, the
	 * base directory (as type File) in which to run sejda. It throws an
	 * IOException if a problem occurs when the console (cmd.exe) is run or the
	 * given directory does not exist.
	 * 
	 * @param cmdAndArgs array of command line arguments
	 * @param baseDir directory in which sejda is run
	 * @throws IOException thrown if directory missing or console failed
	 */
	private void sejdaSplitReport(List<String> cmdAndArgs, File baseDir) throws IOException {
		DL.methodBegin();
		// troubleshooting

		DL.println("Parameters for sejda console launch:");
		for (String string : cmdAndArgs) {
			DL.println("    Param: \'" + string + "\'");
		}

		// call the sejda processor
		ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
		pb.directory(new File(FilenameUtils
				.getFullPath(preferences.getProperty(AttendanceReport.prefSejdaLocation))));
		Process p = pb.start();

		// process the console output to prevent application from hanging
		// http://stackoverflow.com/questions/3774432/starting-a-process-in-java
		try {
			String line;
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = input.readLine()) != null) {
				DL.println(line);
			}
			input.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
		DL.methodEnd();
	}

	/**
	 * Creates or empties a temporary directory with the given name in the
	 * system's temp directory structure.
	 * 
	 * @param name Name of temporary directory to create or empty
	 * @return File object pointing to the directory
	 */
	private File createTempDir(String name) {
		// http://stackoverflow.com/questions/31154727/saving-files-to-temp-folder
		String tempDirS = System.getProperty("java.io.tmpdir") + name;
		File tempDir = new File(tempDirS);
		DL.println("Successful temporary directory created: " + tempDirS + "? " + tempDir.mkdir());
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
		return createTempDir("DARv20170101");
	}

	/**
	 * Wrap given String in quotations. Helpful for DOS command line arguments.
	 * 
	 * @param s String to be wrapped in quotations
	 * @return String wrapped in quotations
	 */
	private String quotesWrap(String s) {
		return "\"" + s + "\"";
	}

	/**
	 * Move the old file to the new location.
	 * 
	 * @param oldFile Full pathname of file to be moved
	 * @param newFile Full pathname of new location
	 * @throws IOException
	 */
	private void reportMoveFile(String oldFile, String newFile) throws IOException {
		reportMoveFile(oldFile, newFile, null);
	}

	/**
	 * Copy the old file to one or two new locations and delete the old file.
	 * Effectively this is a move but with two destinations. Ensure that the
	 * second new filename is null if only one new location is required.
	 * 
	 * @param oldFile Full pathname of file to be moved
	 * @param firstNewFile Full pathname of first new location
	 * @param secondNewFile Full pathname of second new location
	 * @throws IOException
	 */
	private void reportMoveFile(String oldFile, String firstNewFile, String secondNewFile)
			throws IOException {
		DL.methodBegin();
		new File(firstNewFile).delete();

		if (secondNewFile != null) {
			new File(secondNewFile).delete();
			reportCopyFile(oldFile, secondNewFile);
		}

		// TODO Correct for network timeout error
		FileUtils.moveFile(FileUtils.getFile(oldFile), FileUtils.getFile(firstNewFile));
		DL.methodEnd();
	}

	/**
	 * Copies a file successfully across the network, I hope.
	 * 
	 * @param oldFile
	 * @param newFile
	 * @throws IOException
	 */
	private void reportCopyFile(String oldFile, String newFile) throws IOException {
		DL.methodBegin();
		new File(newFile).delete();

		DL.println("Copy file:");
		DL.println("  oldFile: \"" + oldFile + "\"");
		DL.println("  newFile: \"" + newFile + "\"");
		try {
			FileUtils.copyFile(FileUtils.getFile(oldFile), FileUtils.getFile(newFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IOException(
					e.getMessage() + " Try again. The network may be working against you.");
		}
		DL.methodEnd();
	}

	private int deleteFilesOfReportType(File dirToBeCleaned, ReportType whichReport,
			String extension) {
		DL.methodBegin();

		String filesToBeDeletedList[] = dirToBeCleaned.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				return name.contains(whichReport.toString().toLowerCase())
						&& name.toLowerCase().endsWith(extension);
			}
		});

		messageFX.prependTextWithDate("Will now delete " + filesToBeDeletedList.length + " "
				+ whichReport.toString() + " files from " + dirToBeCleaned.getAbsolutePath());
		for (String string : filesToBeDeletedList) {
			new File(dirToBeCleaned.getAbsolutePath() + "\\" + string).delete();
		}
		DL.methodEnd();
		return filesToBeDeletedList.length;
	}

	public String getPDFDate(String sourceFile, ReportType reportT) throws IOException {
		DL.methodBegin();
		File tempDir = createTempDir("sejda_date_extraction");
		if (tempDir == null)
			throw new IOException(
					"Temp date directory could not be created. Try again. If error persists logout and login or restart.");

		String sejdaS = preferences.getProperty(AttendanceReport.prefSejdaLocation);

		// PREPARE SEDJA
		// TEXT COORDINATES:
		// String c[] = {"--top", "--left", "--width", "--height"};
		String c[];

		if (reportT == ReportType.DAR)
			c = new String[] { "514", "61", "75", "22" }; // coords for DAR
															// split by DATE
		else
			c = new String[] { "340", "55", "75", "22" }; // coords for TCAR
		// split by DATE

		// DESTINATION DIRECTORY
		String destinationDirectory = tempDir.getAbsolutePath();

		// http://stackoverflow.com/questions/357315/get-list-of-passed-arguments-in-windows-batch-script-bat
		// http://stackoverflow.com/questions/19103570/run-batch-file-from-java-code

		List<String> cmdAndArgs = Arrays.asList("cmd", "/c", "call", quotesWrap(sejdaS),
				"splitbytext", "-f", quotesWrap(sourceFile), "--top", c[0], "--left", c[1],
				"--width", c[2], "--height", c[3], "-o", quotesWrap(destinationDirectory), "-p",
				"[TEXT]", "--existingOutput", "overwrite");

		sejdaSplitReport(cmdAndArgs, tempDir);

		File fL[] = tempDir.listFiles();

		if (fL.length != 1) {
			// do not delete dir for troubleshooting purposes
			return "date_error";
		}

		deleteDir(tempDir);
		DL.methodEnd();
		return FilenameUtils.removeExtension(fL[0].getName());
	}

	/**
	 * Recursively deletes the contents of a directory.
	 * http://stackoverflow.com/questions/20281835/how-to-delete-a-folder-with-files-using-java
	 * 
	 * @param file directory to empty
	 * @return
	 */
	private boolean deleteDir(File file) {
		File[] contents = file.listFiles();
		if (contents != null) {
			for (File f : contents) {
				deleteDir(f);
			}
		}
		return file.delete();
	}

	//// http://stackoverflow.com/questions/26554814/javafx-updating-gui

	void runMe(TextDateAndTime errorStatusFX) {
		DL.methodBegin();

		errorStatusFX.prependTextWithDate("Try splitting the report(s).");

		boolean success[] = { false, false };
		final int tcar = 0;
		final int dar = 1;
		String commonCancelMsg = "\n\nNote: if you press Cancel to one of the \"Choose master report files...\" dialog boxes the relevant preference will not be changed but you can still set the other preference.";
		String msg = "";

		// Split the TCAR
		try {
			success[tcar] = attemptSplit(ReportType.TCAR);
		} catch (IOException e2) {
			e2.printStackTrace();
			errorStatusFX.prependTextWithDate(e2.getMessage());
			msg += e2.getMessage() + " ";
		}

		// Split the DAR
		try {
			success[dar] = attemptSplit(ReportType.DAR);
		} catch (IOException e2) {
			e2.printStackTrace();
			errorStatusFX.prependTextWithDate(e2.getMessage());
			msg += e2.getMessage() + " ";
		}

		DL.println(
				"Check to see if no, one or two missing report files and whether to THROW exception");

		// no master reports processed
		if (!success[tcar] && !success[dar]) {

			String details = "";
			details = ("No report files processed. " + msg + "\n\nDetails:\n\n"
					+ generateMissingMsg(ReportType.TCAR) + " \n\n"
					+ generateMissingMsg(ReportType.DAR) + " " + commonCancelMsg);
			AttendanceReport.msgBoxError("No reports processed", msg, details);
			errorStatusFX.prependTextWithDate(details);

		} else if (success[tcar] ^ success[dar]) {
			openDesktopFolder(errorStatusFX);

			ReportType dM = !success[tcar] ? ReportType.TCAR : ReportType.DAR;

			String details = "";
			details = ("Only one report processed. \n\n" + msg + generateMissingMsg(dM)
					+ commonCancelMsg);
			AttendanceReport.msgBoxError("One report file processed", "Only one report processed",
					details);
			errorStatusFX.prependTextWithDate(details);
		} else {
			DL.println("Both report files processed.");

			openDesktopFolder(errorStatusFX);

		}

		DL.methodEnd();
	}

	private void openDesktopFolder(TextDateAndTime errorStatusFX) {
		Desktop desktop = Desktop.getDesktop();
		DL.println(preferences.getProperty(AttendanceReport.prefOutputPath) + "\\Current");
		try {
			desktop.open(new File(
					preferences.getProperty(AttendanceReport.prefOutputPath) + "\\Current"));
			errorStatusFX.prependTextWithDate(
					"Windows File Explorer opened. Please confirm that files were generated.");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			DL.println(preferences.getProperty(AttendanceReport.prefOutputPath) + "\\Current");
			errorStatusFX.prependTextWithDate("Failed to open the destination folder: "
					+ preferences.getProperty(AttendanceReport.prefOutputPath) + "\\Current");
		}
	}

	void backupToRunMe(TextDateAndTime errorStatusFX) {
		DL.methodBegin();

		errorStatusFX.prependTextWithDate("Try splitting the report...");

		boolean missing[] = { false, false };
		final int tcar = 0;
		final int dar = 1;
		String commonMsg = "\n\nNote: if you press Cancel to one of the \"Choose master report files...\" dialog boxes the relevant preference will not be changed but you can still set the other preference.";

		try {

			missing[tcar] = attemptSplit(ReportType.TCAR);
			errorStatusFX.prependTextWithDate(
					"Trouble shooting: " + ReportType.TCAR.toString() + " " + missing[tcar]);

			errorStatusFX.prependTextWithDate(
					"Trouble shooting: " + ReportType.DAR.toString() + " " + missing[dar]);
			missing[dar] = attemptSplit(ReportType.DAR);
			errorStatusFX.prependTextWithDate(
					"Trouble shooting: " + ReportType.DAR.toString() + " " + missing[dar]);

			DL.println(
					"Check to see if no, one or two missing report files and whether to THROW exception");
			// THROW EXCEPTIONS

			if (missing[tcar] && missing[dar]) {
				String msg = ("No master report PDF files available. \n\n"
						+ generateMissingMsg(ReportType.TCAR) + " \n\n"
						+ generateMissingMsg(ReportType.DAR) + " " + commonMsg);
				AttendanceReport.msgBoxError("Two missing reports",
						"A problem was encountered with both report files", msg);
				throw new IOException(msg);
			} else if (!missing[tcar] ^ !missing[dar]) {
				Desktop desktop = Desktop.getDesktop();
				desktop.open(new File(preferences.getProperty(AttendanceReport.prefOutputPath)));
				errorStatusFX.prependTextWithDate(
						"Windows File Explorer opened because files should have been generated.");

				ReportType dM = missing[tcar] ? ReportType.TCAR : ReportType.DAR;

				String msg = ("Only one report was processed. \n\n" + generateMissingMsg(dM)
						+ commonMsg);
				AttendanceReport.msgBoxError("Problem with a report",
						"A problem was encountered with one report file", msg);
				throw new IOException(msg);
			} else {
				DL.println(
						"Completed check to see if no, one or two missing report files. THROW IOException not used.");

				Desktop desktop = Desktop.getDesktop();
				desktop.open(new File(preferences.getProperty(AttendanceReport.prefOutputPath)));

				// AttendanceReport.msgBoxInfo("Success", "Both reports
				// successfully processed",
				// "Please check the destination directory that opened to
				// confirm that the files were successfully processed.");

				errorStatusFX.prependTextWithDate(
						"Both reports successfully processed. Please check the destination directory that opened to confirm that the files were successfully processed.");
			}
		} catch (Exception e1) {
			DL.methodBegin();
			DL.println("TRY..CATCH (RM1): one or two missing report exception: e1.getMessage() = "
					+ e1.getMessage());

			e1.printStackTrace();

			DL.println("I'm here");
			if (!missing[tcar] ^ !missing[dar]) {
				Desktop desktop = Desktop.getDesktop();
				try {
					desktop.open(
							new File(preferences.getProperty(AttendanceReport.prefOutputPath)));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				errorStatusFX.prependTextWithDate(
						"Windows File Explorer opened because files should have been generated.");

				String msg = ("Only one report was processed.");
				errorStatusFX.prependTextWithDate(msg);
			}

			errorStatusFX.prependTextWithDate(e1.getMessage());
			DL.methodEnd();
		}

		DL.methodEnd();
	}

	private String generateMissingMsg(ReportType d) {
		DL.methodBegin(d.toString());
		String prefKey = null;

		if (d == ReportType.DAR)
			prefKey = AttendanceReport.prefMasterDAR;
		else if (d == ReportType.TCAR)
			prefKey = AttendanceReport.prefMasterTCAR;

		String pathToMasterReport = preferences.getProperty(prefKey);

		String s = "";
		if (pathToMasterReport == null) {
			s += "The preference for " + d.toString() + " report file has not been set. "
					+ "Use the PDF printer to print that report, and click \"Choose master report files...\" to set the file location. ";
		} else {
			File masterReport = new File(pathToMasterReport);
			if (!masterReport.exists())
				s = "The report file (" + masterReport.getName() + ") for the " + d.toString()
						+ " report is missing. "
						+ "Use the PDF printer to print that report. Ensure the location for the file is set correctly. If the location is incorrect, click \"Choose master report files...\" to set the file location. ";
			else
				s = "A " + d.toString()
						+ " report file was available but was not processed. Confirm that it is the correct file for this report type. Click \"Split reports\" to try to complete the process if the report is of the correct type.";
		}
		DL.methodEnd();
		return s;
	}

	/**
	 * Tries to split the given report type. Throws IOExceptions if an error is
	 * encountered or if the master file for the report type is missing.
	 * 
	 * @param d the type of report as a ReportType
	 * @return true if successful
	 * @throws IOException if any number of failures are encountered
	 */
	private boolean attemptSplit(ReportType d) throws IOException {
		DL.methodBegin();
		DL.println("ReportType." + d.toString());

		File masterReport = new File(
				"[FYI No_" + d.toString() + " file of type PDF has been selected]");

		String prefKey = null;

		if (d == ReportType.DAR)
			prefKey = AttendanceReport.prefMasterDAR;
		else if (d == ReportType.TCAR)
			prefKey = AttendanceReport.prefMasterTCAR;

		String pathToMasterReport = preferences.getProperty(prefKey);

		DL.println("pathToMasterReport = " + pathToMasterReport);

		// TODO Is this try..catch block redundant?
		try {
			masterReport = new File(pathToMasterReport);
		} catch (Exception e) {
			DL.println(
					"TRY..CATCH (AS1): Failure. \"masterReport = new File(pathToMasterReport);\". e.getMessage() = "
							+ e.getMessage());
		}

		// PERFORM SPLIT
		if (masterReport.exists()) {
			// Process the report, throws an IOException that needs to be
			// reported

			messageFX.prependTextWithDate("Begin processing: " + pathToMasterReport);

			splitReport(d);
		} else
			throw new IOException("The file for the " + d.toString() + " report is missing.");

		DL.methodEnd();
		return true;
	}

}
