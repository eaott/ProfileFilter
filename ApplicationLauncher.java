import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import javax.swing.*;
public class ApplicationLauncher {

	public static void main(String[] args) throws Throwable{
		JFrame frame = new JFrame("Tester");
		JPanel panel = new JPanel(new BorderLayout());
		frame.add(panel, BorderLayout.CENTER);
	    BufferedImage filter = javax.imageio.ImageIO.read(new File("/Users/Evan/Downloads/12571282_10209204612616311_552736146_n.png"));
	    
	    boolean gray = true;
	    double blend = 0.5;
	    double brightness = 0;
	    double contrast = 0;
	    double gamma = 1;
	    
	    panel.add(new JTextField("Stuff"));
	    
	    
	    BufferedImage collage = ImageProcessing.createCollageForDemo(filter, gray, blend, brightness, contrast, gamma);
	    collage = ImageProcessing.crop(collage, 400);
	    
		panel.add(new JLabel("", new ImageIcon(collage), JLabel.CENTER));
		frame.setSize(400, 400);
		frame.setVisible(true);
	}

}
