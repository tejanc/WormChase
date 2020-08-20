package UFS;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

public class Obstacles {

	private static final int BOX_LENGTH = 12;
	private ArrayList<Rectangle> boxes;
	private WormChase wcTop;

	public Obstacles(WormChase wcTop) {
		this.wcTop = wcTop;
		boxes = new ArrayList<Rectangle>();
	}

	synchronized public void add(int x, int y) {
		boxes.add( new Rectangle(x,y, BOX_LENGTH, BOX_LENGTH));
		wcTop.setBoxNumber( boxes.size() );	// report new no. of boxes
	}

	synchronized public int getNumObstacles() {
		return boxes.size();
	}

	synchronized public void draw(Graphics g) {
	// draw a series of blue boxes
		Rectangle box;
		g.setColor(Color.blue);
		for(int i = 0; i < boxes.size(); i++) {
			box = (Rectangle) boxes.get(i);
			g.fillRect(box.x, box.y, box.width, box.height);
		}
	} // end of draw

	synchronized public boolean hits(Point p, int size) {
		Rectangle r = new Rectangle (p.x, p.y, size, size);
		Rectangle box;
		for (int i = 0; i < boxes.size(); i++) {
			box = boxes.get(i);
			if (box.intersects(r))
				return true;
		}
		return false;
	} // end of hits()

}
