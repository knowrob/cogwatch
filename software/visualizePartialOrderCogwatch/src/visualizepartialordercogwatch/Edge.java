package visualizepartialordercogwatch;

import javax.vecmath.Vector2f;

import processing.core.PApplet;

public class Edge {

	Node from;
	Node to;
	float len;
	float prob;
	private VisualizePartialOrderCogwatch app;


	Edge(Node from, Node to, float p, VisualizePartialOrderCogwatch applet) {
		this.from = from;
		this.to = to;
		this.len = 50;
		this.prob=p;
		this.app = applet;
	}



	void relax() {
		float vx = to.x - from.x;
		float vy = to.y - from.y;
		float d = PApplet.mag(vx, vy);
		if (d > 0) {
			float f = (len - d) / (d * 3);
			float dx = f * vx;
			float dy = f * vy;
			to.dx += dx;
			to.dy += dy;
			from.dx -= dx;
			from.dy -= dy;
		}
	}

	void setProb(float p) {
		this.prob=p;
	}


	void draw() {

		app.stroke(0);    
		app.noFill();
		
		float stroke = 3000000*prob * from.prob * from.prob;
		app.strokeWeight(stroke);

		
		if(from.label.equals(to.label)) {
			return;
		}

		
		// adapt points to end at the top/bottom end of ellipses
		Vector2f p_to = new Vector2f(to.x, to.y);
		
		if(from.y > to.y) { // end at top part of ellipse
			p_to.y += 300*to.prob;
		} else {
			p_to.y -= 300*to.prob;
		}
		
		
		app.line(from.x, from.y, p_to.x, p_to.y);
		float a = (float)Math.atan2(p_to.y-from.y, p_to.x-from.x);
		
		a -= (float) Math.PI / 8f;
		app.line(p_to.x, p_to.y, -(float) Math.cos(a)*20 + p_to.x, -(float)Math.sin(a)*20 + p_to.y);
		a += (float) Math.PI / 4f;
		app.line(p_to.x, p_to.y, -(float) Math.cos(a)*20 + p_to.x, -(float)Math.sin(a)*20 + p_to.y);
		
	}
}
