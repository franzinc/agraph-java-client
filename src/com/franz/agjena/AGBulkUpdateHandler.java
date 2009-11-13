package com.franz.agjena;

import java.util.Iterator;
import java.util.List;

import com.franz.agbase.AllegroGraph;
import com.franz.agbase.AllegroGraphException;
import com.franz.agjena.exceptions.NiceException;
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.GraphWithPerform;
import com.hp.hpl.jena.graph.impl.SimpleBulkUpdateHandler;

public class AGBulkUpdateHandler extends SimpleBulkUpdateHandler implements
		BulkUpdateHandler {

	public AGBulkUpdateHandler(GraphWithPerform graph) {
		super(graph);
	}
	
	private String[] makeArray ( String[] old, int batch ) {
		if ( old==null ) return new String[batch];
		if ( batch==old.length ) return old;
		return new String[batch];
	}
	
	private void setArrays ( AllegroGraphGraph agg, Triple t, int j, 
			String[] s, String[] p, String[] o, String[] c ) {
		JenaToAGManager j2ag = agg.getJ2AG();
		s[j] = j2ag.jenaNodeToAgStringTerm(t.getSubject());
		p[j] = j2ag.jenaNodeToAgStringTerm(t.getPredicate());
		o[j] = j2ag.jenaNodeToAgStringTerm(t.getObject());		
		c[j] = agg.getContextArgumentString();	
	}

	@Override
	protected void add(List triples, boolean notify) {
		int count = triples.size();
		
		AllegroGraphGraph agg = (AllegroGraphGraph) graph;
		AllegroGraph ags = agg.getAllegroGraphStore();
		int sl = ags.getSelectLimit();
		if ( sl<1000 ) sl = 1000;
		
		String[] s = null;
		String[] p = null;
		String[] o = null;
		String[] c = null;
		
		int i = 0; int left = count;
		while (0<left) {
			int batch = (left<sl)?left:sl;
			
			s = makeArray(s, batch);
			p = makeArray(p, batch);
			o = makeArray(o, batch);
			c = makeArray(c, batch);
			
			for (int j = 0; j < batch; j++) {
				Triple t = (Triple) triples.get(i);
				setArrays(agg, t, j, s, p, o, c);
				i++;
			}
			try {
				ags.verifyEnabled().addTriples(ags, s, p, o, c);
			} catch (AllegroGraphException e) {
				throw new NiceException(e);
			}
			left = left - batch;
		}
		
        if (notify) manager.notifyAddList( graph, triples );
	}

	@Override
	public void add(Triple[] triples) {
		int count = triples.length;
		
		AllegroGraphGraph agg = (AllegroGraphGraph) graph;
		AllegroGraph ags = agg.getAllegroGraphStore();
		int sl = ags.getSelectLimit();
		if ( sl<1000 ) sl = 1000;
		
		String[] s = null;
		String[] p = null;
		String[] o = null;
		String[] c = null;
		
		int i = 0; int left = count;
		while (0<left) {
			int batch = (left<sl)?left:sl;
			
			s = makeArray(s, batch);
			p = makeArray(p, batch);
			o = makeArray(o, batch);
			c = makeArray(c, batch);
			
			for (int j = 0; j < batch; j++) {
				Triple t = triples[i];
				setArrays(agg, t, j, s, p, o, c);
				i++;
			}
			try {
				ags.verifyEnabled().addTriples(ags, s, p, o, c);
			} catch (AllegroGraphException e) {
				throw new NiceException(e);
			}
			left = left - batch;
		}
        manager.notifyAddArray( graph, triples );
	}

}
