package ca.tdsb.dunbar.dailyattendancereport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;

public class DARProperties {
	public static void xmain(String[] args) {
		DARProperties pW = new DARProperties("configDemo.preferences");
		pW.setProperty(null, null);
		System.out.println("Test get: " + pW.getProperty("eat"));
		pW.setProperty("hello", "me");
		pW.setProperty("bite", "you");
		pW.setProperty("eat", "cheese");
		pW.setProperty("eat", "cake");
		pW.setProperty("eat", "chocolate");
		System.out.println("Test get: " + pW.getProperty("eat"));
	}

	// Universal
	private String prefsFile;

	/**
	 * Creates an instance to manage the storage and retrieval of preferences in
	 * the given file. If the file does not already exist it will be created.
	 * 
	 * @param file
	 */
	public DARProperties(String file) {
		prefsFile = System.getenv("APPDATA") + "\\" + file;

		// lazy fix for null pointer errors
		if (getProperty(DAR.prefCreateNoDatePDF)==null) {
			setProperty(DAR.prefCreateNoDatePDF, "" + true);		
		}
	}

	public String getPreferencesFileName() {
		return prefsFile;
	}

	/**
	 * Does the preferences file already exist?
	 * 
	 * @return true if the preferences file exists
	 */
	public boolean exists() {
		return new File(getPreferencesFileName()).exists();
	}

	/**
	 * Writes the given property to the property file. Fails gracefully on a
	 * null value and returns false.
	 * 
	 * @param key Cannot be null
	 * @param value Cannot be null
	 * @return whether the write succeeded
	 */
	public boolean setProperty(String key, String value) {
		if (key == null || value == null)
			return false;
		Enumeration<?> e = null;
		OutputStream output = null;
		Properties prop = readProperties();

		try {
			// File characterFolder = new File(System.getenv("APPDATA") + "\\" +
			// characterName);

			output = new FileOutputStream(prefsFile);
			e = prop.propertyNames();

			// set the properties value

			while (e.hasMoreElements()) {
				String key1 = (String) e.nextElement();
				String value1 = prop.getProperty(key1);
				System.out.println("ReWriting Key : " + key1 + ", Value : " + value1);
				prop.setProperty(key1, value1);
			}
			System.out.println("  Writing Key : " + key + ", Value : " + value);
			prop.setProperty(key, value);

			// save properties to project root folder
			prop.store(output, "DAR Preferences");

			System.out.println("Done setProperty()");
		} catch (Exception io) {
			io.printStackTrace();
			System.out.println("an error occurred");
			return false;
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		return true;
	}

	/**
	 * Reads properties from a file contained in a class-wide variable.
	 * 
	 * @return Properties object with key, value list of properties.
	 */
	public Properties readProperties() {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream(prefsFile);
			prop.load(input);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException f) {
					f.printStackTrace();
				}
			}
		}
		return prop;
	}

	/**
	 * Returns the value belonging to a key in a property database. Null if
	 * (k,v) combination unavailable.
	 * 
	 * @param key
	 * @return value linked to key, null if unavailable
	 */
	public String getProperty(String key) {
		return readProperties().getProperty(key);
	}
}
