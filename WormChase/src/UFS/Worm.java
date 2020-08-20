package UFS;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Point2D;

public class Worm {

	private static final int MAXPOINTS = 40;

	private static final int DOTSIZE = 12;

	private static final int NUM_PROBS = 9;
	
	private static final int RADIUS = DOTSIZE/2;

	private Point cells[];
	private int nPoints;
	private int tailPosn, headPosn;  // tail and head of buffer

	// additional variables already defined
	{
		cells = new Point[MAXPOINTS]; // initialize buffer
		nPoints = 0;
		headPosn = -1; tailPosn = -1; 
	}

	// compass direction/bearing constants
	private static final int NUM_DIRS = 8;
	private static final int N = 0;
	private static final int NE = 1;
	private static final int E = 2;
	private static final int SE = 3;
	private static final int S = 4;
	private static final int SW = 5;
	private static final int W = 6;
	private static final int NW = 7;

	private int currCompass; 	// the current compass dir/bearing

	Point2D.Double incrs[];

	private int pWidth;

	private int pHeight;
	
	private Obstacles obs;

	int[] probsForOffset = new int[NUM_PROBS];
	{
		probsForOffset[0] = 0; probsForOffset[1] = 0;
		probsForOffset[2] = 0; probsForOffset[3] = 1;
		probsForOffset[4] = 1; probsForOffset[5] = 2;
		probsForOffset[6] = -1; probsForOffset[7] = -1;
		probsForOffset[8] = -2;
	}

	public Worm(int PWIDTH, int PHEIGHT, Obstacles obs) {

		this.pWidth = PWIDTH;
		this.pHeight = PHEIGHT;
		this.obs = obs;

		incrs = new Point2D.Double[NUM_DIRS];
		incrs[N] = new Point2D.Double(0.0,-1.0);
		incrs[NE] = new Point2D.Double(0.7,-0.7);
		incrs[E] = new Point2D.Double(1.0,0.0);
		incrs[SE] = new Point2D.Double(0.7,0.7);
		incrs[S] = new Point2D.Double(0.0,1.0);
		incrs[SW] = new Point2D.Double(-0.7,0.7);
		incrs[W] = new Point2D.Double(-1.0,0.0);
		incrs[NW] = new Point2D.Double(-0.7,-0.7);

	}

	private Point nextPoint(int prevPosn, int bearing) {
		// get the increment for the compass bearing
		Point2D.Double incr = incrs[bearing];

		int newX = cells[prevPosn].x + (int)(DOTSIZE * incr.x);
		int newY = cells[prevPosn].y + (int)(DOTSIZE * incr.y);

		// modify newX/newY if < 0, or > pWidth/pHeight; use wraparound
		if (newX + DOTSIZE < 0)	// is circle off left edge of canvas?
			newX = newX + pWidth;
		else if (newX > pWidth)	// is circle off right edge of canvas?
			newX = newX - pWidth;

		if (newY + DOTSIZE < 0)	// is circle off top of canvas?
			newY = newY + pHeight;
		else if (newY > pHeight) // is circle off bottom of canvas
			newY = newY - pHeight;
		return new Point(newX, newY);
	}	// end of nextPoint()

	private int varyBearing()
	// vary the copass bearing semi-randomly
	{
		int newOffset = probsForOffset[ (int)(Math.random()*NUM_PROBS)];
		return calcBearing(newOffset);
	}

	private int calcBearing(int offset) {
		// use the offset to calculate a new compass bearing based
		// on the current compass direction
		int turn =  currCompass + offset;
		// ensure that the turn is between N to NW (0 to 7)
		if (turn >= NUM_DIRS)
			turn = turn - NUM_DIRS;
		else if (turn < 0)
			turn = NUM_DIRS + turn;
		return turn;
	} // end of calcBearing()

	private void newHead(int prevPosn)
	{

		int fixedOffs[] = {-2, 2, -4}; // offsets to avoid an obstacle
		int newBearing = varyBearing();
		Point newPt = nextPoint(prevPosn, newBearing);

		if (obs.hits(newPt, DOTSIZE)) {
			for (int i = 0; i < fixedOffs.length; i++) {
				newBearing = calcBearing(fixedOffs[i]);
				newPt = nextPoint(prevPosn, newBearing);
				if (!obs.hits(newPt,DOTSIZE))
					break;	// one of the fixed offsets will work
			}
		}
		cells[headPosn] = newPt; 	// new head position
		currCompass = newBearing;	// new compass direction
	} // end of newHead()

	public boolean nearHead(int x, int y) {
		// is (x,y) near the worm's head?
		if (nPoints > 0) {
			if ( (Math.abs( cells[headPosn].x + RADIUS - x) <= DOTSIZE) &&
				 (Math.abs( cells[headPosn].y + RADIUS - y) <= DOTSIZE) )
				return true;
		}
		return false;
	}

	public boolean touchedAt(int x, int y) {
		// is (x,y) near any part of the worm's body?
		int i = tailPosn; 
		while (i != headPosn) {
			if ( (Math.abs( cells[headPosn].x + RADIUS - x) <= DOTSIZE) &&
			     (Math.abs( cells[headPosn].y + RADIUS - y) <= DOTSIZE) )
				return true;
			i = (i+1) % MAXPOINTS;
		}
		return false;
	} // end of touchedAt()

	public void move() {
		int prevPosn = headPosn;
		// save old head posn while creating a new one
		headPosn = (headPosn + 1) % MAXPOINTS;

		if (nPoints == 0) { 	// empty array at start
			tailPosn = headPosn;
			currCompass = (int)(Math.random()*NUM_DIRS); 	// random dir.
			cells[headPosn] = new Point(pWidth/2, pHeight/2);	// center pt
			nPoints++;
		}
		else if (nPoints == MAXPOINTS) {	// array s full
			tailPosn = (tailPosn + 1) % MAXPOINTS;	// forget last tail
			newHead(prevPosn);
		}
		else { 	// still room in cells[]
			newHead(prevPosn);
			nPoints++;
		}
	} // end of move()

	public void draw(Graphics g) {
		// draw a black worm with a red head
		if (nPoints > 0) {
			g.setColor(Color.black);
			int i = tailPosn;
			while(i != headPosn) {
				g.fillOval(cells[i].x, cells[i].y, DOTSIZE, DOTSIZE);
				i = (i+1) % MAXPOINTS;
			}
			g.setColor(Color.red);
			g.fillOval(cells[headPosn].x, cells[headPosn].y, DOTSIZE, DOTSIZE);
		}
	} // end of draw()

}
