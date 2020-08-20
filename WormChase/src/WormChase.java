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

public class WormChase extends JFrame implements WindowListener{

	private static int DEFAULT_FPS = 80;
	
	private static WormPanel wp;
	
	private JTextField jtfBox;
	
	private JTextField jtfTime;

	private int pWidth;

	private int pHeight;
	
	public WormChase(long period) {
		super("THE WORM CHASE");
		
		jtfBox = new JTextField();
		jtfTime = new JTextField();
	
		pack();	// first pack (the GUI doesn't include the JPanel yet)
		setResizable(false);	// so sizes are for nonresizable GUI elems.
		calcSizes();
		setResizable(true);	// so panel can be added
		
		Container c = getContentPane();
		wp = new WormPanel(this, period, pWidth, pHeight);
		c.add(wp, "Center");
		pack();	// second pack, after JPanel is added
		
		addWindowListener(this);
		
		addComponentListener(new ComponentAdapter() {
			public void componentMoved(ComponentEvent e)
			{
				setLocation(0,0);
			}
		});
		
		setResizable(false);
		setVisible(true);
		
		wp.run();
	}

	private void calcSizes() {
		GraphicsConfiguration gc = getGraphicsConfiguration();
		Rectangle screenRect = gc.getBounds(); // screen dimensions
		
		Toolkit tk = Toolkit.getDefaultToolkit();
		Insets desktopInsets = tk.getScreenInsets(gc);
		
		Insets frameInsets = getInsets(); 	// only works after pack()
		
		Dimension tfDim = jtfBox.getPreferredSize(); 	// textfield size
		
		pWidth = screenRect.width 
				- (desktopInsets.left + desktopInsets.right)
				- (frameInsets.left + frameInsets.right);
		
		pHeight = screenRect.height
				- (desktopInsets.top + desktopInsets.bottom)
				- (frameInsets.top + frameInsets.bottom)
				- tfDim.height;
		
	} // end of calcSizes()

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
	
	public void windowActivated(WindowEvent e) {
		wp.resumeGame();
	}
	
	public void windowDeactivated(WindowEvent e) {
		wp.pauseGame();
	}
	
	public void windowDeiconified(WindowEvent e) {
		wp.resumeGame();
	}
	
	public void windowIconified(WindowEvent e) {
		wp.resumeGame();
	}
	
	public void windowClosing(WindowEvent e) {
		wp.stopGame();
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		wp.stopGame();
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		wp.resumeGame();
	}
}
