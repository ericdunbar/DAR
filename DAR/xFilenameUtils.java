package ca.tdsb.dunbar.dailyattendancereport;

//http://stackoverflow.com/questions/924394/how-to-get-the-filename-without-the-extension-in-java
//http://commons.apache.org/proper/commons-io/javadocs/api-release/index.html?org/apache/commons/io/package-summary.html

public class xFilenameUtils {

	private static final int NOT_FOUND = -1;

	/**
	 * The extension separator character.
	 * 
	 * @since 1.4
	 */
	public static final char EXTENSION_SEPARATOR = '.';

	/**
	 * The Unix separator character.
	 */
	private static final char UNIX_SEPARATOR = '/';

	/**
	 * The Windows separator character.
	 */
	private static final char WINDOWS_SEPARATOR = '\\';

	
	/**
	 * Removes the extension from a filename.
	 * <p>
	 * This method returns the textual part of the filename before the last dot.
	 * There must be no directory separator after the dot.
	 * 
	 * <pre>
	 * foo.txt    --&gt; foo
	 * a\b\c.jpg  --&gt; a\b\c
	 * a\b\c      --&gt; a\b\c
	 * a.b\c      --&gt; a.b\c
	 * </pre>
	 * <p>
	 * The output will be the same irrespective of the machine that the code is
	 * running on.
	 *
	 * @param filename
	 *            the filename to query, null returns null
	 * @return the filename minus the extension
	 */
	public static String removeExtension(final String filename) {
		if (filename == null) {
			return null;
		}
		failIfNullBytePresent(filename);

		final int index = indexOfExtension(filename);
		if (index == NOT_FOUND) {
			return filename;
		} else {
			return filename.substring(0, index);
		}
	}

	/**
	 * Check the input for null bytes, a sign of unsanitized data being passed
	 * to to file level functions.
	 *
	 * This may be used for poison byte attacks.
	 * 
	 * @param path
	 *            the path to check
	 */
	private static void failIfNullBytePresent(String path) {
		int len = path.length();
		for (int i = 0; i < len; i++) {
			if (path.charAt(i) == 0) {
				throw new IllegalArgumentException("Null byte present in file/path name. There are no "
						+ "known legitimate use cases for such data, but several injection attacks may use it");
			}
		}
	}

	/**
	 * Returns the index of the last extension separator character, which is a
	 * dot.
	 * <p>
	 * This method also checks that there is no directory separator after the
	 * last dot. To do this it uses {@link #indexOfLastSeparator(String)} which
	 * will handle a file in either Unix or Windows format.
	 * </p>
	 * <p>
	 * The output will be the same irrespective of the machine that the code is
	 * running on.
	 * </p>
	 * 
	 * @param filename
	 *            the filename to find the last extension separator in, null
	 *            returns -1
	 * @return the index of the last extension separator character, or -1 if
	 *         there is no such character
	 */
	public static int indexOfExtension(final String filename) {
		if (filename == null) {
			return NOT_FOUND;
		}
		final int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
		final int lastSeparator = indexOfLastSeparator(filename);
		return lastSeparator > extensionPos ? NOT_FOUND : extensionPos;
	}
	
	/**
	 * Returns the index of the last directory separator character.
	 * <p>
	 * This method will handle a file in either Unix or Windows format.
	 * The position of the last forward or backslash is returned.
	 * <p>
	 * The output will be the same irrespective of the machine that the code is running on.
	 *
	 * @param filename  the filename to find the last path separator in, null returns -1
	 * @return the index of the last separator character, or -1 if there
	 * is no such character
	 */
	public static int indexOfLastSeparator(final String filename) {
	    if (filename == null) {
	        return NOT_FOUND;
	    }
	    final int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
	    final int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
	    return Math.max(lastUnixPos, lastWindowsPos);
	}


}