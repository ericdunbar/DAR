package ca.tdsb.dunbar.dailyattendancereport;

import java.io.File;

public class FilePathTest {

	public static void main(String... args){

	    String basePath = new File("").getAbsolutePath();
	    System.out.println(basePath);

	    String path = new File("src/main/resources/conf.properties")
	                                                           .getAbsolutePath();
	    System.out.println(path);
	    System.out.println(new File("src/main/resources/conf.properties").exists());
	    
	    System.out.println();
	    System.out.println(new File("hello.txt").getPath());
	}
}
