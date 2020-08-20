package UFS;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JTextField;

public class WormChase extends JFrame{

	private static int DEFAULT_FPS = 80;
	
	private JTextField jtfBox;
	
	private JTextField jtfTime;
	
	public WormChase(long period) {
		
		super("THE WORM CHASE");
		
		Container c = getContentPane();
		c.setLayout(new BorderLayout() );
		
		WormPanel wp = new WormPanel(this, period);
		c.add(wp, "Center");
		
		setUndecorated(true); 	// no borders or title bars
		setIgnoreRepaint(true);	// turn off paint events since doing active rendering
		
		jtfBox = new JTextField();
		jtfTime = new JTextField();
	
		pack();
		setResizable(false);
		setVisible(true);	
		
		wp.run();
	}

	public static void main (String [] args) {
		int fps = DEFAULT_FPS;
		if (args.length != 0)
			fps = Integer.parseInt(args[0]);
		
		long period = (long) 1000.0/fps;
		System.out.println("fps: " + fps + "; period: " + period + " ms");
		
		new WormChase(period*1000000L);		// ms --> nanosecs
		
	}
	
	public void setBoxNumber(int no) {
		jtfBox.setText("Boxes used: " + no);
	}
	
	public void setTimeSpent(long t) {
		jtfTime.setText("Time spent: " + t + " secs");
	}

}
