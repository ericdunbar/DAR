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
	 * @throws IOException
	 *             File not found
	 */
	public SejdaSupport(DARProperties prefs, PrintStream printStream, TextDAR msgFX) throws IOException {
		// Initialize variables
		this.preferences = prefs;
		this.ps = printStream;
		this.messageFX = msgFX;
		File sejdaF = new File(prefs.getProperty(DAR.prefSejdaLocation));
		if (!sejdaF.exists()) {
			throw new IOException("sejda not found. Ensure correct application is available.");
		}
	}

	TextDAR messageFX;
	File sejdaF;
	PrintStream ps;
	DARProperties preferences;

	/**
	 * Splits the DAR.
	 * 
	 * @throws IOException
	 */
	public void splitDAR(DARType whichDAR) throws IOException {

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

		String localPrefDARLocation;

		String teacherNameCoordinates[];

		String describeDAR = whichDAR.toString();

		if (whichDAR == DARType.Useful) {
			localPrefDARLocation = DAR.prefMasterUsefulDAR;

			// USEFUL split by TEACHERNAME
			teacherNameCoordinates = new String[] { "86", "84", "200", "22" };
		} else {
			localPrefDARLocation = DAR.prefMasterUselessDAR;

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
		List<String> cmdAndArgs = Arrays.asList("cmd", "/c", "call", quotesWrap(sejdaS), "splitbytext", "-f",
				quotesWrap(sourceFile), "--top", teacherNameCoordinates[0], "--left", teacherNameCoordinates[1],
				"--width", teacherNameCoordinates[2], "--height", teacherNameCoordinates[3], "-o",
				quotesWrap(destinationDirectory), "-p", "[TEXT]", "--existingOutput", "overwrite");

		sejdaSplitDAR(cmdAndArgs, tempDir);

		// DELETE FILES
		// http://stackoverflow.com/questions/5751335/using-file-listfiles-with-filenameextensionfilter
		File ftbd = new File(preferences.getProperty(DAR.prefOutputPath));

		// delete existing PDFs
		deleteFiles(ftbd, whichDAR, ".pdf");

		// TODO?

		String newFileList[] = tempDir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				return name.toLowerCase().endsWith(".pdf");
			}
		});

		// String newFileList[] = tempDir.list(new SuffixFileFilter(".pdf"));

		// MOVE and COPY PDFs

		System.out.println("TROUBLESHOOTING: tempDir.getAbsolutePath() = " + tempDir.getAbsolutePath());

		for (String string : newFileList) {
			System.out.println("    " + tempDir.getAbsolutePath() + "\\" + string);
		}

		// Attempted fix for constantly refreshing archive directory
		File archiveDir = new File(preferences.getProperty(DAR.prefOutputPath) + "\\Archive\\");
		archiveDir.mkdir();
		archiveDir = new File(preferences.getProperty(DAR.prefOutputPath) + "\\Archive\\" + dateForDAR);
		archiveDir.mkdir();

		System.out.println();
		System.out.println("Start: MOVE and ARCHIVE FILES");
		for (String s : newFileList) {
			String oldFile = tempDir.getAbsolutePath() + "\\" + s;

			String newName = FilenameUtils.getBaseName(s) + " " + describeDAR + ".pdf";
			String newArchivalName = FilenameUtils.getBaseName(s) + " " + describeDAR + " " + dateForDAR + ".pdf";

			String newFile = preferences.getProperty(DAR.prefOutputPath) + "\\" + newName;
			String archiveFile = archiveDir.getAbsolutePath() + "\\" + newArchivalName;

			darMoveFile(oldFile, newFile, archiveFile);
		}
		System.out.println("End: MOVE and ARCHIVE FILES");

		// MOVE master DAR to ARCHIVE
		String FROM = preferences.getProperty(localPrefDARLocation);
		String TO = preferences.getProperty(DAR.prefOutputPath) + "\\Archive\\Masters\\" + "Master_DAR_" + describeDAR
				+ "_" + dateForDAR + ".pdf";
		darMoveFile(FROM, TO);
		System.out.println("End: MOVE and ARCHIVE MASTER for " + describeDAR);
		messageFX.prependTextWithDate("MOVE and ARCHIVE MASTER for " + describeDAR);
	}

	/**
	 * Splits the PDF given the String array of DOS command-line arguments, the
	 * base directory (as type File) in which to run sejda. It throws an
	 * IOException if a problem occurs when the console (cmd.exe) is run or the
	 * given directory does not exist.
	 * 
	 * @param cmdAndArgs
	 *            array of command line arguments
	 * @param baseDir
	 *            directory in which sejda is run
	 * @throws IOException
	 *             thrown if directory missing or console failed
	 */
	private void sejdaSplitDAR(List<String> cmdAndArgs, File baseDir) throws IOException {
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
	}

	/**
	 * Creates or empties a temporary directory with the given name in the
	 * system's temp directory structure.
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
	private String quotesWrap(String s) {
		return "\"" + s + "\"";
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

	// TODO Implement filtering
	private void deleteFiles(File ftbd, DARType whichDAR, String extension) {

		String filesToBeDeletedList[] = ftbd.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				return name.contains(whichDAR.toString().toLowerCase()) && name.toLowerCase().endsWith(extension);
			}
		});

		for (String string : filesToBeDeletedList) {
			new File(preferences.getProperty(DAR.prefOutputPath) + "\\" + string).delete();
		}

	}

	public String getPDFDate(String sourceFile, DARType darT) throws IOException {
		File tempDir = createTempDir("sejda_date_extraction");
		if (tempDir == null)
			throw new IOException(
					"Temp date directory could not be created. Try again. \nIf error persists logout and login or restart.");

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
			// do not delete dir for troublshooting purposes
			return "date_error";
		}

		deleteDir(tempDir);
		return FilenameUtils.removeExtension(fL[0].getName());
	}

	/**
	 * Recursively deletes the contents of a directory.
	 * http://stackoverflow.com/questions/20281835/how-to-delete-a-folder-with-files-using-java
	 * 
	 * @param file
	 *            directory to empty
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
		DAR.working = true;

		try {
			// TODO Have errorStatusFX update before processing

			if (!(new File(preferences.getProperty(DAR.prefOutputPath)).exists())) {
				throw new IOException("Destination directory unavailable");
			}

			// TODO Eliminate Apache
			// TODO RENAME variable

			File masterUselessDAR = new File("\\\\NowhereInParticularJunkDir");
			File masterUsefulDAR = new File("\\\\NowhereInParticularJunkDir");
			
			String pfmul = preferences.getProperty(DAR.prefMasterUselessDAR);
			String pfmuf = preferences.getProperty(DAR.prefMasterUsefulDAR);

			DAR.minorln(pfmul);
			DAR.minorln(pfmuf);

			boolean missing[] = { false, false };

			try {
				masterUselessDAR = new File(pfmul);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				missing[0] = true;
			}

			try {
				masterUsefulDAR = new File(pfmuf);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				missing[1] = true;
			}


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
				throw new IOException(
						"Both master DAR files missing. Use CutePDF to print new ones and/or choose the master DAR PDFs.");
			else if (!missing[0] || !missing[1]) {
				Desktop desktop = Desktop.getDesktop();
				desktop.open(new File(preferences.getProperty(DAR.prefOutputPath)));
				System.out.println("I opened the file explorer");
				if (missing[0])
					throw new IOException("Only one DAR processed. " + masterUselessDAR.getName() + " ("
							+ DARType.Useless.toString() + ")" + " is missing. Use CutePDF to print a new "
							+ DARType.Useless.toString() + " DAR and/or choose a new master "
							+ DARType.Useless.toString()
							+ " DAR. Note: if you press Cancel to one of the \"Choose master DAR...\" dialog boxes the relevant preference will not be changed.");
				else if (missing[1])
					throw new IOException("Only one DAR processed. " + masterUsefulDAR.getName() + " ("
							+ DARType.Useful.toString() + ")" + " is missing. Use CutePDF to print a new "
							+ DARType.Useful.toString() + " DAR and/or choose a new master " + DARType.Useful.toString()
							+ " DAR. Note: if you press Cancel to one of the \"Choose master DAR...\" dialog boxes the relevant preference will not be changed.");
			}

			errorStatusFX.prependTextWithDate("Both DARs successfully processed.");

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();

			errorStatusFX.prependTextWithDate(e1.getMessage());
			DAR.working = false;
		}

		DAR.working = false;
	}

}
