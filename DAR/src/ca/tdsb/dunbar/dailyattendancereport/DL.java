package ca.tdsb.dunbar.dailyattendancereport;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class DL {

	private static int indent = 0;
	private static boolean toFileB = false;
	private static PrintStream printS;

	/**
	 * Opens a file to log <code>System.Out</code> and exceptions. Exceptions
	 * can also be sent to the log.
	 * 
	 * @param toFile whether to log activities to a file
	 * @param logStdError whether to duplicate output to both a log file and to
	 *            the console
	 */
	public static PrintStream startLogging(boolean logStdError) {
		// CONFIGURE LOGGING
		// http://stackoverflow.com/questions/8043356/file-write-printstream-append
		// http://stackoverflow.com/questions/12053075/how-do-i-write-the-exception-from-printstacktrace-into-a-text-file-in-java

		PrintStream ps = null;
		try {
			ps = new PrintStream(
					new FileOutputStream(System.getProperty("java.io.tmpdir") + "\\" + "DAR20161230_1_log.txt", true));
			if (logStdError) {
				System.setErr(ps);
			}
			toFileB = true;
			printS = ps;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ps;
	}

	/**
	 * Provide a minor program update.
	 * 
	 * @param string
	 */
	public static void println(String string) {
		String msg = indenter() + string;
		System.out.println(msg);
		if (toFileB) {
			printS.println(msg); // capture System.out to a file
		}
	}

	private static String indenter() {
		String indentS ="";//= Integer.toString(indent);
		for (int i = 0; i < indent; i++) {
			indentS += "    ";
		}
		return indentS;
	}

	/**
	 * Reports the end of a method, provided the method closes neatly and does
	 * not throw an exception before it ends.
	 * https://github.com/ericdunbar/g272u2/blob/master/Support/src/DisplayMethodDetails.java
	 */
	public static void methodEnd() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		indent--;
		println("Method end: " + ste[2].toString());
	}

	/**
	 * Reports the initiation of a method to <code>System.out</code>.
	 * https://github.com/ericdunbar/g272u2/blob/master/Support/src/DisplayMethodDetails.java
	 */
	public static void methodBegin() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		println("Method start: " + ste[2].toString());
		indent++;

		// majorln(ste[2].getMethodName());
		//
		// minorln("Length ste[] = " + ste.length);
		// for (StackTraceElement s : ste) {
		// minorln("getMethodName " + s.getMethodName());
		// minorln("getFileName " + s.getFileName());
		// minorln("getClassname " + s.getClassName());
		// minorln("toString " + s.toString());
		// minorln("getLineNumber " + s.getLineNumber());
		// minorln("");
		// }
	}

}
