package visualizepartialordercogwatch;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import processing.core.PApplet;
import processing.core.PFont;

import processing.pdf.*;
import processing.xml.XMLElement;

public class VisualizePartialOrderCogwatch extends PApplet {

	private static final long serialVersionUID = 8434199615180345429L;
	static final int nodeColor = 0xFFCC33;
	static final int edgeColor = 0xFF9900;
	PFont font;

	boolean record;
	Node selection; 



	// list of all nodes
	public ArrayList<Edge> edges = new ArrayList<Edge>();
	public ArrayList<Node> nodes = new ArrayList<Node>();

	// list of all edges
	public HashMap<String, Edge> edgeTable = new HashMap<String, Edge>();
	public HashMap<String, Node> nodeTable = new HashMap<String, Node>();


	HashMap<String, ArrayList<String>> domains;
	HashMap<String, Float> probabilities;




	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	public void setup() {

		size(600, 600);  
		font = createFont("SansSerif", 10);
		domains = new HashMap<String, ArrayList<String>>();
		probabilities = new HashMap<String, Float>();


		loadData();

		////////////////////////////////////////////////
		// whole loop: prune graph
		for(int from=0;from<nodes.size();from++) {
			for(int to=0;to<nodes.size();to++) {

				String edge = ((Node)nodes.get(from)).getName()+"_"+((Node) nodes.get(to)).getName();

				if(edgeTable.containsKey( edge )) {
					
					for(int via=0;via<nodes.size();via++) {

						Edge from_via = ((Edge)edgeTable.get(((Node)nodes.get(from)).getName()+"_"+((Node)nodes.get(via)).getName()));   
						Edge via_to   = ((Edge)edgeTable.get(((Node)nodes.get(via)).getName()+"_"+((Node) nodes.get(to)).getName()));


						if( from_via != null) {

							if(via_to != null && from_via.prob==1.0 && via_to.prob==1.0) {
								edges.remove(edgeTable.get(edge));
								edgeTable.remove(edge);
								//print("removed "+edge+"\n");
							} 

							// prune edges with two stops
							for(int via2=0;via2<nodes.size();via2++) {
								Edge via_via2 = ((Edge)edgeTable.get(((Node)nodes.get(via)).getName()+"_"+((Node) nodes.get(via2)).getName()));
								Edge via2_to  = ((Edge)edgeTable.get(((Node)nodes.get(via2)).getName()+"_"+((Node) nodes.get(to)).getName()));


								if( via_via2 != null && via2_to != null) {                 
									if(from_via.prob==1.0 && via_via2.prob==1.0 && via2_to.prob==1.0) {
										edges.remove(edgeTable.get(edge));
										edgeTable.remove(edge);
										//print("removed "+edge+"\n");
									} 
								}
							}
						}
					}
				}
				
				
				// merge antiparallel edges
				String anti_edge = ((Node)nodes.get(to)).getName()+"_"+((Node) nodes.get(from)).getName();

				if(edgeTable.containsKey( edge ) && edgeTable.containsKey(anti_edge)) {
					
					Edge fwd  = edgeTable.get(edge);
					Edge back = edgeTable.get(anti_edge);
					
					if(fwd.prob > back.prob) {
						fwd.setProb(fwd.prob - back.prob);
						edgeTable.remove(back);
					} else {
						back.setProb(back.prob - fwd.prob);
						edgeTable.remove(fwd);
					}
				}
			}
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	public void draw() {

		if (record) {
			beginRecord(PDF, "output.pdf");
		}

		background(255);
		textFont(font);  
		smooth();  


		float prob_threshold = 0.002f;

		for (int i = 0 ; i < edges.size() ; i++) {

			if(edges.get(i).from.prob > prob_threshold && edges.get(i).to.prob > prob_threshold)
				edges.get(i).draw();
		}

		for (int i = 0 ; i < nodes.size() ; i++) {

			if(nodes.get(i).prob > prob_threshold)
				((Node)nodes.get(i)).draw();
		}
		if (record) {
			endRecord();
			record = false;
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	public Edge addEdge(String fromLabel, String toLabel, float prob) {

		Node from = findNode(fromLabel);
		Node to = findNode(toLabel);

		// search for existing edges, set prob if the new one is higher
		for (int i = 0; i < edges.size(); i++) {
			if ( ((Edge) edges.get(i)).from == from && ((Edge) edges.get(i)).to == to && ((Edge) edges.get(i)).prob<prob) {
				((Edge) edges.get(i)).setProb(prob);
				return ((Edge) edges.get(i));
			}
		} 

		Edge e = new Edge(from, to, prob, this);
		edges.add(e);
		edgeTable.put(fromLabel+"_"+toLabel, e);

		// debug:
		//print(from.label + "_" + to.label + "\n");

		return e;
	}

	public Node addNode(String label) {

		Node n = new Node(label, this);
		nodeTable.put(label, n);
		nodes.add(n);  
		return n;

	}

	Node findNode(String label) {
		Node n = (Node) nodeTable.get(label);
		if (n == null) {
			return addNode(label);
		}
		return n;
	}



	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	//String filename = "segm.learnt.pmml";
	//String actionT  = "segmentT(s1)";
	//String precedes = "precedes(s1, s2)";
	//int numPrecParents = 2;


	String filename    = "actionrec.patients.pmml";
	String actionT     = "actionT(a1)";
	String objActedOn  = "objActedOn(a1)";
	String toLocation  = "toLocation(a1)";
	String groupT      = "groupT(g)";
	String errorT      = "errorT(e)";

	String precedes = "precedes(a1, a2, g, e)";
	int numPrecParents = 8;

	public void loadData() {

		XMLElement xml = new XMLElement(this, filename);

		// pmml
		for (int i = 0; i < xml.getChildCount(); i++) {

			if(!xml.getChild(i).getName().equals("DataDictionary")) {
				continue;
			}

			XMLElement datadict = xml.getChild(i);


			// read node domains from PMML
			readDomain(actionT, datadict);
			readDomain(objActedOn, datadict);
			readDomain(toLocation, datadict);
			readDomain(errorT, datadict);
			readDomain(groupT, datadict);

			// TODO: create separate graphs for each activity


			///////////////////////////////////////
			// read edges

			// find precedes predicate
			for (int df = 0; df < datadict.getChildCount(); df++) {
				if(datadict.getChild(df).getStringAttribute("name").equals(precedes)) {

					// find Extension module
					for (int val = 0; val < datadict.getChild(df).getChildCount(); val++) {
						if(datadict.getChild(df).getChild(val).getName().equals("Extension")) {
							XMLElement ext = datadict.getChild(df).getChild(val);


							// find the X-Table inside the X-Definition
							for (int x = 0; x < ext.getChildCount(); x++) {
								if(ext.getChild(x).getName().equals("X-Definition")) {

									for (int xx = 0; xx < ext.getChild(x).getChildCount(); xx++) {
										if(!ext.getChild(x).getChild(xx).getName().equals("X-Table")) {continue;}

										// read all the cond. probabilities
										String[] cpt = ext.getChild(x).getChild(xx).getContent().split(" ");

										// iterate across the table of conditional probabilities and link the nodes accordingly 
										int numNodes = nodeTable.keySet().size();
										int numActivities=2;


										//	<X-Given>0</X-Given> <!-- objActedOn(a1) -->
										//	<X-Given>1</X-Given> <!-- errorT(e) -->
										//	<X-Given>2</X-Given> <!-- groupT(g) -->
										//	<X-Given>3</X-Given> <!-- toLocation(a1) -->
										//	<X-Given>4</X-Given> <!-- actionT(a1) -->
										//	<X-Given>6</X-Given> <!-- #actionT(a2) -->
										//	<X-Given>7</X-Given> <!-- #objActedOn(a2) -->
										//	<X-Given>8</X-Given> <!-- #toLocation(a2) -->
										//	<X-Given>9</X-Given> <!-- !(a1=a2) -->


										int blocksize_obj1    = domains.get(errorT).size() * domains.get(groupT).size() * domains.get(toLocation).size() * domains.get(actionT).size() * domains.get(actionT).size() * domains.get(objActedOn).size() * domains.get(toLocation).size();
										int blocksize_error   = domains.get(groupT).size() * domains.get(toLocation).size() * domains.get(actionT).size() * domains.get(actionT).size() * domains.get(objActedOn).size()  * domains.get(toLocation).size();
										int blocksize_group   = domains.get(toLocation).size() * domains.get(actionT).size() * domains.get(actionT).size() * domains.get(objActedOn).size()  * domains.get(toLocation).size();
										int blocksize_toLoc1  = domains.get(actionT).size() * domains.get(actionT).size() * domains.get(objActedOn).size()  * domains.get(toLocation).size();
										int blocksize_action1 = domains.get(actionT).size() * domains.get(objActedOn).size()  * domains.get(toLocation).size();
										int blocksize_action2 = domains.get(objActedOn).size()  * domains.get(toLocation).size();
										int blocksize_obj2    = domains.get(toLocation).size();
										int blocksize_toLoc2  = 1;


										for(int obj1=0; obj1<domains.get(objActedOn).size(); obj1++) {  											 // #elem 10
											for(int error=0; error<domains.get(errorT).size(); error++) {										 // #elem 1
												for(int group=0; group<domains.get(groupT).size(); group++) {									 // #elem 1
													for(int toLoc1=0; toLoc1<domains.get(toLocation).size(); toLoc1++) {						 // #elem 5
														for(int action1=0; action1<domains.get(actionT).size(); action1++) {					 // #elem 3
															for(int action2=0; action2<domains.get(actionT).size(); action2++) {				 // #elem 3
																for(int obj2=0; obj2<domains.get(objActedOn).size() ; obj2++) {					 // #elem 10
																	for(int toLoc2=0; toLoc2<domains.get(toLocation).size(); toLoc2++) {		 // #elem 5


																		// total: 22500 blocks in the CPT, each block four numbers ((a1=a2) plus true/false)

																		// TODO: modify this loop to take the different parents into account
																		//       -> also read labels for parents and add separate nodes for each combination


																		float condA1A2 = Float.valueOf(cpt[ 4* (obj1    * blocksize_obj1 + 
																				error   * blocksize_error + 
																				group   * blocksize_group + 
																				toLoc1  * blocksize_toLoc1 +
																				action1 * blocksize_action1 +
																				action2 * blocksize_action2 +
																				obj2    * blocksize_obj2 +
																				toLoc2  * blocksize_toLoc2)] );

																		if(condA1A2>0.0) {

																			// search for nodes, create if don't exist yet

																			Node from = this.findNode(
																					domains.get(actionT).get(action1) + " " + 
																					domains.get(objActedOn).get(obj1) + " to " +
																					domains.get(toLocation).get(toLoc1));

																			from.prob = probabilities.get(domains.get(actionT).get(action1)) *
																			probabilities.get(domains.get(objActedOn).get(obj1)) *
																			probabilities.get(domains.get(toLocation).get(toLoc1));

																			Node to = this.findNode(
																					domains.get(actionT).get(action2) + " " + 
																					domains.get(objActedOn).get(obj2) + " to " +
																					domains.get(toLocation).get(toLoc2));

																			to.prob = 	probabilities.get(domains.get(actionT).get(action2)) *
																			probabilities.get(domains.get(objActedOn).get(obj2)) *
																			probabilities.get(domains.get(toLocation).get(toLoc2));

																			this.addEdge(from.label, to.label, condA1A2);
																		}

																	}
																}
															}
														}
													}
												}
											}
										}

										break;
									}
								}
							}

							break;
						}
					}
					break;
				}
			}
		}

	}


	private void readDomain(String element, XMLElement datadict) {

		for (int df = 0; df < datadict.getChildCount(); df++) {
			if(datadict.getChild(df).getStringAttribute("name").equals(element)) {

				// add the prob for each node
				String[] probs = {};
				for (int val = 0; val < datadict.getChild(df).getChildCount(); val++) {
					if(datadict.getChild(df).getChild(val).getName().equals("Extension")) {
						XMLElement ext = datadict.getChild(df).getChild(val);

						for (int x = 0; x < ext.getChildCount(); x++) {
							if(ext.getChild(x).getName().equals("X-Definition")) {
								probs = ext.getChild(x).getChild(0).getContent().split(" ");
								break;
							}
						}

						break;
					}
				}

				// create a node for each value
				int valIdx=0;
				for (int val = 0; val < datadict.getChild(df).getChildCount(); val++) {
					if(datadict.getChild(df).getChild(val).getName().equals("Value")) {
						float prob = Float.valueOf(probs[valIdx++]);
						//							this.addNode(datadict.getChild(df).getChild(val).getStringAttribute("value")).setProb(prob);

						if(!this.domains.containsKey(element))
							this.domains.put(element, new ArrayList<String>());

						this.domains.get(element).add(datadict.getChild(df).getChild(val).getStringAttribute("value"));
						this.probabilities.put(datadict.getChild(df).getChild(val).getStringAttribute("value"), prob);
					}
				}

				break;        
			}
		}
	}



	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	public void keyPressed() {
		if (key == 'r') {
			record = true;
		}
	}


	public void mousePressed() {
		// Ignore anything greater than this distance
		float closest = 20;
		for (int i = 0; i < nodes.size(); i++) {
			Node n = ((Node)nodes.get(i));
			float d = dist(mouseX, mouseY, n.x, n.y);
			if (d < closest) {
				selection = n;
				closest = d;
			}
		}
		if (selection != null) {
			if (mouseButton == LEFT) {
				selection.fixed = true;
			} else if (mouseButton == RIGHT) {
				selection.fixed = false;
			}
		}
	}


	public void mouseDragged() {
		if (selection != null) {
			selection.x = mouseX;
			selection.y = mouseY;
		}
	}


	public void mouseReleased() {
		selection = null;
	}



}