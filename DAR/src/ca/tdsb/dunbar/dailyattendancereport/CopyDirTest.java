package ca.tdsb.dunbar.dailyattendancereport;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class CopyDirTest {

	public static void main(String[] args) {
		thisWorks();
	}

	public static void thisWorks() {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		File masterSejdaDir = new File("sejda-console-2.10.4");

		// allow for flexibility in future versions of sejda provided the
		// version number is removed
		if (!masterSejdaDir.exists())
			masterSejdaDir = new File("sejda-console");

		// throw exception if sejda-console missing
		if (!masterSejdaDir.exists())
			System.out.println("can't find the directory");

		System.out.println("From: " + masterSejdaDir.getAbsolutePath());

		File destDir = new File(System.getenv("LOCALAPPDATA") + "\\abc\\");// +
																			// masterSejdaDir.getName());

		System.out.println("To:   " + destDir.getAbsolutePath());

		// 1. a. Recurse through the directory and copy each file
		try {
			System.out.println("XXXXbegin copy");
			FileUtils.copyDirectory(masterSejdaDir, destDir);
			System.out.println("XXXXend copy");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
