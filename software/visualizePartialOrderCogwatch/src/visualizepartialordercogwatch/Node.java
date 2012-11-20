package visualizepartialordercogwatch;

import processing.core.PApplet;
import processing.core.PConstants;

public class Node {

	float x, y;
	float dx, dy;
	boolean fixed;
	String label;
	int count;
	double prob;
	private VisualizePartialOrderCogwatch app;

	Node(String label, VisualizePartialOrderCogwatch applet) {
		this.label = label;
		this.app = applet;
		x = app.random(app.width);
		y = app.random(app.height);

	}

	void setProb(double p) {
		this.prob=p;
		this.count=20;
	}

	String getName() {
		return this.label;
	}


	void draw() {


		app.fill(0xFFFFFF);
		app.stroke(0);
		app.strokeWeight(1);


		app.fill(255, 200, 50, 200);
//		app.sphere(((float) (1000000000*prob)));
		app.ellipse(x, y, (float)(4000*prob), (float)(600*prob));

		//	    if (count > w+2) {
		app.fill(0);
		app.textAlign(PConstants.CENTER, PConstants.CENTER);

		label = label.replace("CowsMilkProduct", "Milk");
		label = label.replace("PuttingSomethingSomewhere", "Put");
		label = label.replace("PouringSomethingInto",      "Pour");
		label = label.replace("RemovingSomething",      "Remove");
		label = label.replace("CoffeeBeverage",      "Coffee");
		label = label.replace("TeaBeverage",      "Tea");
		
		app.text(label, x, y);
		//	    }
	}
}

