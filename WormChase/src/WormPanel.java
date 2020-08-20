import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class WormPanel extends JPanel implements Runnable{
	
	// record stats ever 1 second (roughly)
	private static final long MAX_STATS_INTERVAL = 1000000000L;

	// number of FPS values stored to get an average
	private static final int NUM_FPS = 20;
	
	private static final int MAX_FRAME_SKIPS = 25;
	
	private static final int NO_DELAYS_PER_YIELD = 1000;

	// used for gathering statistics
	private long statsInterval = 0L; 	// in ms
	private long prevStatsTime;
	private long totalElapsedTime = 0L;
	
	private long frameCount = 0;
	private double fpsStore[];
	private long statsCount = 0;
	private double averageFPS = 0.0;
	private double[] upsStore;
	private double averageUPS = 0.0;

	private WormChase wcTop;
	private Obstacles obs;
	private Worm fred;
	private Font font;
	private Object metrics;

	private boolean isPaused;
	private boolean gameOver;
	private boolean running;

	private int score;
	private int PWIDTH;
	private int PHEIGHT;

	private long gameStartTime;
	private int timeSpentInGame; 	// in seconds

	private long framesSkipped = 0;
	private long totalFramesSkipped = 0;
	
	private DecimalFormat timedf = new DecimalFormat("0.##");	// 2 dp
	private DecimalFormat df = new DecimalFormat("0.####");		// 4 dp
	
	private long period; // period between drawing in ms

	private Graphics dbg;

	private Image dbImage;

	public WormPanel(WormChase wc, long period, int pWidth, int pHeight) {
		wcTop = wc;
		this.period = period;
		PWIDTH = pWidth;
		PHEIGHT = pHeight;

		setBackground(Color.white);
		setPreferredSize (new Dimension(pWidth,pHeight));

		setFocusable(true);
		requestFocus();	// now has focus, so receives key events
		readyForTermination();

		// create game components
		obs = new Obstacles(wcTop);
		fred = new Worm(PWIDTH, PHEIGHT, obs);

		addMouseListener( new MouseAdapter() {
			public void mousePressed(MouseEvent e)
			{
				testPress(e.getX(), e.getY());
			}
		});

		// set up message font
		font = new Font("SansSerif", Font.BOLD, 24);
		metrics = this.getFontMetrics(font);

		// initialize timing elements
		fpsStore = new double[NUM_FPS];
		upsStore = new double[NUM_FPS];
		for (int i = 0; i < NUM_FPS; i++) {
			fpsStore[i] = 0.0;
			upsStore[i] = 0.0;
		}
	} // end of WormPanel()

	private void readyForTermination() {
		addKeyListener( new KeyAdapter(){
			// listen for esc, q and ctrl-c
			public void keyPressed(KeyEvent e){
				int keyCode = e.getKeyCode();
				if (	(keyCode == KeyEvent.VK_ESCAPE) ||
						(keyCode == KeyEvent.VK_Q) ||
						(keyCode == KeyEvent.VK_END) ||
						(keyCode == KeyEvent.VK_C && e.isControlDown()) ) {
					running = false;
				}
			}
		});
	} // end readyForTermination()

	private void testPress (int x, int y)
	// is (x,y) near the head or should an obstacle be added?
	{
		if (!isPaused && !gameOver) {
			if (fred.nearHead(x,y)) { 	// was mouse press near the head?
				gameOver = true;
				score = (40 - timeSpentInGame) + 40 - obs.getNumObstacles();
				// hack together a score
			}
			else { 	// add an obstacle if possible
				if (!fred.touchedAt(x,y)) 	// was worm's body not touched?
					obs.add(x,y);
			}
		}
	} // end of testPress()

	public void resumeGame()
	// called when the JFrame is activated/ de-iconified
	{
		isPaused = false;
	}

	public void pauseGame()
	// called when the JFrame is deactivated/ iconified
	{
		isPaused = true;
	}

	public void stopGame() {
		// called when the JFrame is closing
		running = false;
	}

	public void run()
	/* The frames of the animations are drawn inside the while loop */
	{
		long beforeTime, afterTime, timeDiff, sleepTime;
		long overSleepTime = 0L;
		int noDelays = 0;
		long excess = 0L;

		gameStartTime = System.nanoTime();
		prevStatsTime = gameStartTime;
		beforeTime = gameStartTime;

		running = true;
		while(running) {
			gameUpdate();
			gameRender();
			paintScreen();

			afterTime = System.nanoTime();
			timeDiff = afterTime - beforeTime;
			sleepTime = (period - timeDiff) - overSleepTime;

			if (sleepTime > 0) { 	// some time left in this cycle
				try {
					Thread.sleep(sleepTime/10000000);	// nano - ms
				}
				catch (InterruptedException ex) {}
				overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
			}
			else { 		// sleepTime <= 0; frame took longer than the period
				excess -= sleepTime; 	// stores excess time value
				overSleepTime = 0;

				if (++noDelays >= NO_DELAYS_PER_YIELD) {
					Thread.yield();		// give another thread a chance to run
					noDelays = 0;
				}
			}

			beforeTime = System.nanoTime();

			/* If frame animation is taking too long, update the game state
			 * without rendering it, to get the updates/sec nearer to the
			 * required FPS. 
			 */
			int skips = 0;
			while ((excess > period) && (skips < MAX_FRAME_SKIPS)) {
				excess -= period;
				gameUpdate();	// update state but don't render.
				skips++;
			}
			framesSkipped += skips;

			storeStats();
		}

		printStats();
		System.exit(0);		// window disappears
	}	// end of run()

	private void gameRender() {
		if (dbImage == null) {
			dbImage = createImage(PWIDTH, PHEIGHT);
			if (dbImage == null) {
				System.out.println("dbImage is null");
				return;
			}
			else
				dbg = dbImage.getGraphics();
		}
		
		// clear the background
		dbg.setColor(Color.white);
		dbg.fillRect(0, 0, PWIDTH, PWIDTH);
		
		dbg.setColor(Color.blue);
		dbg.setFont(font);
		
		// report average FPS and UPS at top left
		dbg.drawString("Average FPS/UPS: " + df.format(averageFPS) +
				", " + df.format(averageUPS), 20, 25);
		
		// report time used and boxes used at bottom left
		dbg.drawString("Time spent: " + timeSpentInGame + " secs", 10, PHEIGHT-15);
		dbg.drawString("Boxes used: " + obs.getNumObstacles(), 260, PHEIGHT-15);
		
		dbg.setColor(Color.black);
		
		// draw game elements: the obstacles and the worm
		obs.draw(dbg);
		fred.draw(dbg);
		
		if(gameOver)
			gameOverMessage(dbg);

	} // end of gameRender()

	private void gameOverMessage(Graphics g) {
		g.drawString("YOU CAUGHT THE WORM!", PWIDTH/2-10, PHEIGHT/2-10);
		g.drawString("YOU SCORED " + score + " POINTS!", PWIDTH/2-10, PHEIGHT/2+10);
	}

	// use active rendering to put the buffered image on-screen
	private void paintScreen() {
		Graphics g;
		try {
			g = this.getGraphics();
			if ((g != null) && (dbImage != null))
				g.drawImage(dbImage, 0, 0, null);
			Toolkit.getDefaultToolkit().sync(); // sync the display on some systems
			g.dispose();
		}
		catch (Exception e)
		{	System.out.println("Graphics context error: " + e); }
	} // end of paintScreen()

	private void gameUpdate() {
		if (!isPaused && !gameOver)
			fred.move();
	}

	private void printStats() {
		System.out.println("Frame Count/Loss: " + frameCount + " / " + totalFramesSkipped);
		System.out.println("Average FPS: " + df.format(averageFPS));
		System.out.println("Average UPS: " + df.format(averageUPS));
		System.out.println("Time Spent: " + timeSpentInGame + " secs");
		System.out.println("Boxes used: " + obs.getNumObstacles());
	} // end of printStats()

	private void storeStats() {

		frameCount++;
		statsInterval += period;

		if (statsInterval >= MAX_STATS_INTERVAL) {
			long timeNow = System.nanoTime();
			timeSpentInGame = (int) ((timeNow - gameStartTime)/1000000000L);	// ns --> secs
			wcTop.setTimeSpent( timeSpentInGame );

			long realElapsedTime = timeNow - prevStatsTime;
			// time since last stats collection
			totalElapsedTime += realElapsedTime;

			double timingError = ((double)Math.abs((realElapsedTime - statsInterval)) / statsInterval)*100.0;

			totalFramesSkipped += framesSkipped;

			double actualFPS = 0;	// calculate the latest FPS and UPS
			double actualUPS = 0;
			if (totalElapsedTime > 0) {
				actualFPS = (((double)frameCount / totalElapsedTime) * 1000000000L);
				actualUPS = (((double)(frameCount + totalFramesSkipped) / totalElapsedTime) * 1000000000L);
			}

			// store the latest FPS and UPS
			fpsStore[ (int)statsCount%NUM_FPS ] = actualFPS;
			upsStore[ (int)statsCount%NUM_FPS ] = actualUPS;
			statsCount++;

			double totalFPS = 0.0;	// total the stored FPSs and UPSs
			double totalUPS = 0.0;
			for (int i = 0; i < NUM_FPS; i++) {
				totalFPS += fpsStore[i];
				totalUPS += upsStore[i];
			}

			if (statsCount < NUM_FPS) {	// obtain the average FPS and UPS
				averageFPS = totalFPS/statsCount;
				averageUPS = totalUPS/statsCount;
			}
			else {
				averageFPS = totalFPS/NUM_FPS;
				averageUPS = totalUPS/NUM_FPS;
			}
			
			// Comment this section
/*			
			System.out.println(
					timedf.format((double) statsInterval/1000000000) + " " +
					timedf.format((double) realElapsedTime/1000000000) + "s " +
					df.format(timingError) + "% " +
					frameCount + "c " +
					framesSkipped + "/" + totalFramesSkipped + " skip; " +
					df.format(actualFPS) + " " + df.format(averageFPS) + " afps; " + 
					df.format(actualUPS) + " " + df.format(averageUPS) + " aups; " );
			*/
			// to here
			
			framesSkipped = 0;
			prevStatsTime = timeNow;
			statsInterval = 0;	// reset
		}     
	}	// end of storeStats()
}

