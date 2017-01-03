package ca.tdsb.dunbar.dailyattendancereport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.commons.io.FileUtils;

public class DL {

	private static int indent = 0;
	private static boolean duplicateOutput = false;
	private static PrintStream printStream;
	private static boolean muteSysOut = false;

	/**
	 * Opens a file to log <code>System.Out</code>, and optionally logs
	 * exceptions from standard error. The console can also be disabled.
	 * 
	 * @param logStdError true to send exceptions to a file
	 * @param consoleActive true to display System.Out on console
	 */
	public static PrintStream startLogging(boolean logStdError, boolean consoleActive, boolean muteSysOut) {
		// CONFIGURE LOGGING
		// http://stackoverflow.com/questions/8043356/file-write-printstream-append
		// http://stackoverflow.com/questions/12053075/how-do-i-write-the-exception-from-printstacktrace-into-a-text-file-in-java

		PrintStream ps = null;
		try {
			String fileName = System.getProperty("java.io.tmpdir") + "\\" + "DAR"+DAR.versionDAR+"_log.txt";

			File fSize = new File(fileName);
			boolean append = true;

			if (fSize.length() > 1e7) {
				File fBak = new File(fSize.getAbsolutePath() + ".bak");
				fBak.delete();
				FileUtils.moveFile(fSize, fBak);
				append = false;
			}

			ps = new PrintStream(new FileOutputStream(fileName, append));

			if (logStdError) {
				System.setErr(ps);
			}

			duplicateOutput = true;
			if (!consoleActive) {
				duplicateOutput = false;
				System.setOut(ps);
			}

			DL.muteSysOut = muteSysOut;

			printStream = ps;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ps;
	}

	/**
	 * Prints a <code>String</code> to <code>System.Out</code> and terminate the
	 * line. Adds indenting to match the current hierarchy of method calls.
	 * Optionally, log the same output to a file. Note: indenting can be
	 * disrupted if exceptions are thrown.
	 * 
	 * @param string <code>String</code> to be printed
	 */
	public static void println(String string) {
		if (muteSysOut) {
			return;
		}
		String msg = indenter() + string;
		System.out.println(msg);
		if (duplicateOutput) {
			printStream.println(msg); // capture System.Out to a file
		}
	}

	private static String indenter() {
		String indentS = "";// = Integer.toString(indent);
		for (int i = 0; i < indent; i++) {
			indentS += "    ";
		}
		return indentS;
	}

	/**
	 * Reports the end of a method to <code>System.Out</code>, provided the method closes neatly and does
	 * not throw an exception before it ends.
	 * https://github.com/ericdunbar/g272u2/blob/master/Support/src/DisplayMethodDetails.java
	 */
	public static void methodEnd() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		indent--;
		println("Method end: " + ste[2].getMethodName() + " (" + ste[2].getFileName() + ":" + ste[2].getLineNumber()
				+ ")");
	}

	public static void methodBegin(){
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		println("Method start: " + ste[2].getMethodName() + " (" + ste[2].getFileName() + ":" + ste[2].getLineNumber()
				+ ")");
		indent++;
	}
	
	/**
	 * Reports the initiation of a method to <code>System.Out</code>.
	 * https://github.com/ericdunbar/g272u2/blob/master/Support/src/DisplayMethodDetails.java
	 */
	public static void methodBegin(String s) {
		String append = "";
		if (s!=null) {
			append = " ["+s+"]";
		}
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		println("Method start: " + ste[2].getMethodName() + " (" + ste[2].getFileName() + ":" + ste[2].getLineNumber()
				+ ")" + append);
		indent++;

		// println(ste[2].getMethodName());
		//
		// println("Length ste[] = " + ste.length);
		// for (StackTraceElement s : ste) {
		// println("getMethodName " + s.getMethodName());
		// println("getFileName " + s.getFileName());
		// println("getClassname " + s.getClassName());
		// println("toString " + s.toString());
		// println("getLineNumber " + s.getLineNumber());
		// println("");
		// }
	}

}
