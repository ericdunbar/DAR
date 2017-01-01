package ca.tdsb.dunbar.dailyattendancereport;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import ca.tdsb.dunbar.dailyattendancereport.DAR.DARType;
import ca.tdsb.dunbar.dailyattendancereport.DAR.TextDAR;

public class SejdaSupport {

	/**
	 * prefs is a pointer to a preferences object, printStream is where st.error
	 * should be directed.
	 * 
	 * @param prefs
	 * @param printStream
	 * @throws IOException File not found
	 */
	public SejdaSupport(DARProperties prefs, TextDAR msgFX) throws IOException {
		// Initialize variables
		this.preferences = prefs;
		this.messageFX = msgFX;
		if (!this.preferences.exists())
			throw new IOException("Please set preferences. Without valid preferences this program is useless.");

		String sedjaFS = prefs.getProperty(DAR.prefSejdaLocation);
		if (sedjaFS == null)
			throw new IOException(
					"Set the location of \"sejda-console\" using the \"Choose sedja-console...\" button. File location is likely whereever this is found: sejda-console-2.10.4-bin\\sejda-console-2.10.4\\bin\\sejda-console.");
		File sejdaF = new File(sedjaFS);
		if (!sejdaF.exists()) {
			throw new IOException(
					"\"sejda-console\" is unavailable. Move it back to its original location or set its new location using the \"Choose sedja-console...\" button. File location is likely whereever this is found: sejda-console-2.10.4-bin\\sejda-console-2.10.4\\bin\\sejda-console.");
		}

		String outPath = preferences.getProperty(DAR.prefOutputPath);
		if (outPath == null)
			throw new IOException(
					"Please set destination (output) directory using the \"Choose destination directory...\" button.");

		if (!(new File(outPath).exists())) {
			throw new IOException(
					"Destination directory unavailable or has been moved (this could indicate a network error in which case changing destination directory won't do anything). "
							+ "Please set destination (output) directory using the \"Choose destination directory...\" button.");
		}

	}

	TextDAR messageFX; // display status updates here
	File sejdaF; // sejda-console
	DARProperties preferences;

	/**
	 * Splits the DAR.
	 * 
	 * @throws IOException
	 */
	public void splitDAR(DARType whichDAR) throws IOException {
		DL.methodBegin();
		DL.println("whichDAR = " + whichDAR.toString());

		/*
		 * List of tasks to do: 1. prepare temporary directory by emptying it 2.
		 * prepare arguments to pass to sejda for DATE getDateFromPDF(File f) 3.
		 * run sejda for DATE 4. read DATE into a variable dateVar 5. write
		 * dateVar to file 6. delete PDF
		 */

		String sejdaS = preferences.getProperty(DAR.prefSejdaLocation);

		// 0. PREPARE SEDJA

		// 1. TEXT COORDINATES & type of preferences file
		// String c[] = {"--top", "--left", "--width", "--height"};

		String localPrefMasteDAR;

		String teacherNameCoordinates[];

		String describeDAR = whichDAR.toString();

		if (whichDAR == DARType.Useful) {
			localPrefMasteDAR = DAR.prefMasterUsefulDAR;

			// USEFUL split by TEACHERNAME
			teacherNameCoordinates = new String[] { "86", "84", "200", "22" };
		} else {
			localPrefMasteDAR = DAR.prefMasterUselessDAR;

			// USELESS split by TEACHERNAME
			teacherNameCoordinates = new String[] { "105", "71", "200", "22" };
		}

		// SOURCE FILE
		String sourceFile = preferences.getProperty(localPrefMasteDAR);

		// DATE
		String dateForDAR = getPDFDate(sourceFile, whichDAR);

		File tFile = new File(dateForDAR);
		if (tFile.getName().equalsIgnoreCase("date_error")) {
			throw new IOException(
					"The source PDF(s) are reversed or incorrect PDF(s) were chosen for one or both DAR types. Please use the \"Choose Master DAR Files...\" button to choose the correct file(s).");
		}
		DL.println("THE DATE: " + dateForDAR);

		// DESTINATION DIRECTORY
		// Prepare temporary directory
		File tempDir = createTempDir();
		if (tempDir == null)
			throw new IOException("Temp directory could not be created. Try again.");

		String destinationDirectory = tempDir.getAbsolutePath();

		// PERFORM THE SPLIT

		// http://stackoverflow.com/questions/357315/get-list-of-passed-arguments-in-windows-batch-script-bat
		// http://stackoverflow.com/questions/19103570/run-batch-file-from-java-code
		List<String> cmdAndArgs = Arrays.asList("cmd", "/c", "call", quotesWrap(sejdaS), "splitbytext", "-f",
				quotesWrap(sourceFile), "--top", teacherNameCoordinates[0], "--left", teacherNameCoordinates[1],
				"--width", teacherNameCoordinates[2], "--height", teacherNameCoordinates[3], "-o",
				quotesWrap(destinationDirectory), "-p", "[TEXT]", "--existingOutput", "overwrite");

		sejdaSplitDAR(cmdAndArgs, tempDir);

		// DELETE EXISTING FILES
		// http://stackoverflow.com/questions/5751335/using-file-listfiles-with-filenameextensionfilter
		File ftbd = new File(preferences.getProperty(DAR.prefOutputPath));

		int delF = deleteFiles(ftbd, whichDAR, ".pdf");

		messageFX.prependTextWithDate(
				"Cleaned up destination by removing " + delF + " " + whichDAR.toString() + " files.");

		String newFileList[] = tempDir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				return name.toLowerCase().endsWith(".pdf");
			}
		});

		// MOVE and COPY PDFs

		DL.println("FILE LIST FOR: " + tempDir.getAbsolutePath());

		for (String string : newFileList) {
			DL.println("    " + tempDir.getAbsolutePath() + "\\" + string);
		}

		// Attempted fix for constantly refreshing archive directory
		File archiveDir = new File(preferences.getProperty(DAR.prefOutputPath) + "\\Archive\\");
		archiveDir.mkdir();
		archiveDir = new File(preferences.getProperty(DAR.prefOutputPath) + "\\Archive\\" + dateForDAR);
		archiveDir.mkdir();

		DL.println("Start: MOVE and ARCHIVE NEW FILES");
		for (String s : newFileList) {
			String oldFile = tempDir.getAbsolutePath() + "\\" + s;

			String newName = FilenameUtils.getBaseName(s) + " " + describeDAR + ".pdf";
			String newArchivalName = FilenameUtils.getBaseName(s) + " " + describeDAR + " " + dateForDAR + ".pdf";
			newName = newArchivalName; // yes, got lazy!

			String newFile = preferences.getProperty(DAR.prefOutputPath) + "\\" + newName;
			String newArchivalFile = archiveDir.getAbsolutePath() + "\\" + newArchivalName;

			darMoveFile(oldFile, newFile, newArchivalFile);
		}
		messageFX.prependTextWithDate(
				"Moved " + newFileList.length + " new " + whichDAR.toString() + " teacher DAR PDF files to: "
						+ preferences.getProperty(DAR.prefOutputPath) + " for " + dateForDAR + ".");

		// MOVE master DAR to ARCHIVE
		String FROM = preferences.getProperty(localPrefMasteDAR);
		String TO = preferences.getProperty(DAR.prefOutputPath) + "\\Archive\\Masters\\" + "Master_DAR_" + describeDAR
				+ "_" + dateForDAR + ".pdf";
		darMoveFile(FROM, TO);
		messageFX.prependTextWithDate("Archived the master DAR PDF for " + describeDAR + " for " + dateForDAR + ".");
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
	private void sejdaSplitDAR(List<String> cmdAndArgs, File baseDir) throws IOException {
		DL.methodBegin();
		// troubleshooting

		System.out.println("Parameters for sejda console launch:");
		for (String string : cmdAndArgs) {
			System.out.println("         Param: _" + string + "_");
		}

		// call the sejda processor
		ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
		pb.directory(new File(FilenameUtils.getFullPath(preferences.getProperty(DAR.prefSejdaLocation))));
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
	 * @param s String to be wrapped in quotations
	 * @return String wrapped in quotations
	 */
	private String quotesWrap(String s) {
		return "\"" + s + "\"";
	}

	private void darMoveFile(String oldFile, String newFile) throws IOException {
		darMoveFile(oldFile, newFile, null);
	}

	private void darMoveFile(String oldFile, String firstNewFile, String secondNewFile) throws IOException {
		new File(firstNewFile).delete();

		if (secondNewFile != null) {
			new File(secondNewFile).delete();
			FileUtils.copyFile(FileUtils.getFile(oldFile), FileUtils.getFile(secondNewFile));
		}
		FileUtils.moveFile(FileUtils.getFile(oldFile), FileUtils.getFile(firstNewFile));
	}

	private int deleteFiles(File ftbd, DARType whichDAR, String extension) {
		DL.methodBegin();

		String filesToBeDeletedList[] = ftbd.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				return name.contains(whichDAR.toString().toLowerCase()) && name.toLowerCase().endsWith(extension);
			}
		});

		for (String string : filesToBeDeletedList) {
			new File(preferences.getProperty(DAR.prefOutputPath) + "\\" + string).delete();
		}
		DL.methodEnd();
		return filesToBeDeletedList.length;
	}

	public String getPDFDate(String sourceFile, DARType darT) throws IOException {
		DL.methodBegin();
		File tempDir = createTempDir("sejda_date_extraction");
		if (tempDir == null)
			throw new IOException(
					"Temp date directory could not be created. Try again. If error persists logout and login or restart.");

		String sejdaS = preferences.getProperty(DAR.prefSejdaLocation);

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

		List<String> cmdAndArgs = Arrays.asList("cmd", "/c", "call", quotesWrap(sejdaS), "splitbytext", "-f",
				quotesWrap(sourceFile), "--top", c[0], "--left", c[1], "--width", c[2], "--height", c[3], "-o",
				quotesWrap(destinationDirectory), "-p", "[TEXT]", "--existingOutput", "overwrite");

		sejdaSplitDAR(cmdAndArgs, tempDir);

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

	void runMe(TextDAR errorStatusFX) {
		DL.methodBegin();
		DAR.working = true;

		try {
			boolean missing[] = { false, false };
			final int useless = 0;
			final int useful = 1;

			missing[useless] = attemptSplit(DARType.Useless);

			missing[useful] = attemptSplit(DARType.Useful);

			DL.println("Check to see if no, one or two missing DAR files and whether to THROW exception");
			// THROW EXCEPTIONS
			String commonMsg = "Note: if you press Cancel to one of the \"Choose master DAR...\" dialog boxes the relevant preference will not be changed but you can still set the other preference.";

			if (missing[useless] && missing[useful]) {
				String msg = ("Both master DAR PDF files unavailable. " + generateMissingMsg(DARType.Useful) + " "
						+ generateMissingMsg(DARType.Useless) + " " + commonMsg);
				DAR.msgBoxError("Two missing DAR", "A problem was encountered with both DAR files", msg);
				throw new IOException(msg);
			} else if (!missing[useless] ^ !missing[useful]) {
				Desktop desktop = Desktop.getDesktop();
				desktop.open(new File(preferences.getProperty(DAR.prefOutputPath)));
				errorStatusFX
						.prependTextWithDate("Windows File Explorer opened because files should have been generated.");

				DARType dM = missing[useless] ? DARType.Useless : DARType.Useful;

				String msg = ("Only one DAR processed. " + generateMissingMsg(dM) + commonMsg);
				DAR.msgBoxError("Problem with a DAR", "A problem was encountered with one DAR file", msg);
				throw new IOException(msg);
			} else {
				DL.println("Completed check to see if no, one or two missing DAR files. THROW IOException not used.");

				Desktop desktop = Desktop.getDesktop();
				desktop.open(new File(preferences.getProperty(DAR.prefOutputPath)));

				DAR.msgBoxInfo("Success", "Both DARs successfully processed",
						"Please check the destination directory that opened to confirm that the files were successfully processed.");

				errorStatusFX.prependTextWithDate("Both DARs successfully processed.");
			}
		} catch (Exception e1) {
			DL.println("TRY..CATCH (RM1): one or two missing DAR Exception: e1.getMessage() = " + e1.getMessage());

			e1.printStackTrace();

			errorStatusFX.prependTextWithDate(e1.getMessage());
		}

		DAR.working = false;
		DL.methodEnd();
	}

	private String generateMissingMsg(DARType d) {
		String prefKey = null;

		if (d == DARType.Useful)
			prefKey = DAR.prefMasterUsefulDAR;
		else if (d == DARType.Useless)
			prefKey = DAR.prefMasterUselessDAR;

		String pathToMasterDAR = preferences.getProperty(prefKey);

		String s = "";
		if (pathToMasterDAR == null) {
			s = "The preference for " + d.toString() + " DAR masters has not been set. "
					+ "Use CutePDF to print a new DAR of that type, if necessary and Choose that master DAR PDF. ";
		} else {
			File masterDAR = new File(pathToMasterDAR);
			if (!masterDAR.exists())
				s = "The master DAR file (" + masterDAR.getName() + ") for " + d.toString()
						+ " DAR masters is missing. "
						+ "Use CutePDF to print a new DAR of that type. If necessary, choose the location for the master DAR PDF. ";
		}
		return s;
	}

	private boolean attemptSplit(DARType d) throws IOException {
		DL.methodBegin();
		DL.println("d = " + d.toString());
		File masterDAR = new File("[FYI No_" + d.toString() + " file of type PDF has been selected]");

		String prefKey = null;

		if (d == DARType.Useful)
			prefKey = DAR.prefMasterUsefulDAR;
		else if (d == DARType.Useless)
			prefKey = DAR.prefMasterUselessDAR;

		String pathToMasterDAR = preferences.getProperty(prefKey);

		boolean missing = false;

		DL.println("pathToMasterDAR in attemptSplit(): " + pathToMasterDAR);

		try {
			masterDAR = new File(pathToMasterDAR);
		} catch (Exception e) {
			DL.println("TRY..CATCH (AS1): Failure. \"masterDAR = new File(pathToMasterDAR);\". e.getMessage() = "
					+ e.getMessage());
			missing = true;
		}

		// PERFORM SPLIT
		DL.println("Try processing: " + pathToMasterDAR);

		if (masterDAR.exists()) {
			// Process the DAR
			splitDAR(d);
		} else
			missing = true;

		DL.methodEnd();
		return missing;
	}

}
