package ca.tdsb.dunbar.dailyattendancereport;

import javax.swing.JFrame;

public class Byling extends JFrame {

	public static String title;
	public static int height;
	public static int width;
	
	public Byling(String t, int h, int w){
		title = t;
		height = h;
		width = w;
	}
	
	public static void run(String title, int height, int width){
		JFrame frame = new JFrame(title);
		frame.setVisible(false);
		frame.setEnabled(true);
		frame.setSize(height, width);
	}
	
	public static void main(String [] args){
		Byling thing = new Byling("Test", 600, 800);
		thing.run(title, height, width);
		String s = "6ello";
		String t = "6ell";
		String y = "Bc";
		String z = "Abc";
		System.out.println("t: y" + t.compareTo(y));
		System.out.println("s: y" + s.compareTo(y));
		System.out.println("t: " + t.compareTo(z));
		System.out.println("s: " + s.compareTo(z));
	}
}
