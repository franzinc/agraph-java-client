package test;

import com.hp.hpl.jena.graph.Graph;

public class AGAnonGraphTest extends AGGraphTest {

	public AGAnonGraphTest(String name) {
		super(name);
	}

	@Override
	public Graph getGraph() {
		return maker.createGraph();
	}

}
