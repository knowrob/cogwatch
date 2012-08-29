package visualizepartialordercogwatch;

import processing.core.PApplet;
import processing.core.PConstants;

public class Node {

	float x, y;
	float dx, dy;
	boolean fixed;
	String label;
	int count;
	float prob;
	private VisualizePartialOrderCogwatch app;

	Node(String label, VisualizePartialOrderCogwatch applet) {
		this.label = label;
		this.app = applet;
		x = app.random(app.width);
		y = app.random(app.height);

	}

	void setProb(float p) {
		this.prob=p;
		this.count=20;
	}

	void increment() {
		count+=20;
	}

	String getName() {
		return this.label;
	}


	void relax() {
		float ddx = 0;
		float ddy = 0;

		for (int j = 0; j < app.nodes.size(); j++) {
			Node n = app.nodes.get(j);
			if (n != this) {
				float vx = x - n.x;
				float vy = y - n.y;
				float lensq = vx * vx + vy * vy;
				if (lensq == 0) {
					ddx += app.random(1);
					ddy += app.random(1);
				} else if (lensq < 100*100) {
					ddx += vx / lensq;
					ddy += vy / lensq;
				}
			}
		}
		float dlen = PApplet.mag(ddx, ddy) / 2;
		if (dlen > 0) {
			dx += ddx / dlen;
			dy += ddy / dlen;
		}
	}


	void update() {

	}


	void draw() {


		app.fill(0xFFFFFF);
		app.stroke(0);
		app.strokeWeight(1);


		app.fill(255, 200, 50);
		app.sphere(100000*prob);
		app.ellipse(x, y, 4000*prob, 600*prob);

		//	    if (count > w+2) {
		app.fill(0);
		app.textAlign(PConstants.CENTER, PConstants.CENTER);
		app.text(label, x, y);
		//	    }
	}
}

