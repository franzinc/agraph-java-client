package com.franz.agbase;

import java.lang.reflect.Array;
import java.util.ArrayList;

import com.franz.agbase.transport.AGConnector;
import com.franz.agbase.util.AGC;

/**
 * This class implements Social Network Analysis tools.
 * <p>
 * An AllegroGraph triple store can be used to store a social network,
 * The methods in this class implement search operations on this social network.
 * <p>
 * This class is instantiated from an AllegroGraph instance with a call to
 * {@link AllegroGraph#getSNAExtension()}.
 *  
 * @author mm
 *
 */
public class SNAExtension {
	
	private AllegroGraph ag = null;
	
	SNAExtension ( AllegroGraph ag ) {
		this.ag = ag;
	}
	
	
	/**
	 * Copy the generators defined in another SNAExtension instance.
	 * @param from the SNAExtension from which the generators are copied
	 */
	public void copyGeneratorsFrom ( SNAExtension from ) throws AllegroGraphException {
//	  one name table per AG access instance
		ag.verifyEnabled().applyAGFn(ag, AGC.AG_COPY_GENERATORS, new Object[]{ new Integer(from.ag.tsx) });
	}
	
	/**
	 * 
	 * @param v The array of values returned from the server.  
	 *          The first value must be an array of UPIs
	 * @return the array of UPIs
	 */
	private UPI[] getUPIArray ( Object[] v ) {
		if ( null==v ) return new UPI[0];
		if ( 0==v.length ) return new UPI[0];
		return AGConnector.toUPIArray(v[0]);
	}
	
	/**
	 * Register a generator in the AllegroGraph server.
	 * This generator can be used in subsequent calls to search methods.
	 * @param name the name of the generator.  If the name matches a previously defined
	 *    generator in this instance, the older definition is overwritten.
	 * @param parts an Object array that defines the generator.
	 *         <ul>
	 *         <li>If this argument is null, the named generator is deleted.
	 *         <li>If the argument is an array of length zero, nothing is done.
	 *         <li>If the first element of the array is a string, the entire array is the
	 *              definition of a single clause.
	 *         <li>otherwise, each element of the array must be a Object array that defines
	 *              a clause of the generator.
	 *         </ul>
	 *   <p>
	 *   A generator clause specifies a set of neighbors that can be reached from 
	 *   a given node.  The result of the generator is the union of the nodes from
	 *   all the clauses.
	 *   <ul>
	 *   <li>The first element of a clause is a string that identifies the nature
	 *       of the clause and determines the format of the remaining elements.
	 *   <li>If the first element is the string  "objects-of", "subjects-of", or "undirected",
	 *       then the remaining elements of the clause are part references that may be strings
	 *       in ntriples format, Value instances, or UPI instances.
	 *       <ul>
	 *       <li>"objects-of" -- this clause collects all the nodes that are objects of
	 *             triples with the specified predicates.
	 *       <li>"subjects-of" -- this clause collects all the nodes that are subjects of
	 *             triples with the specified predicates.
	 *       <li>"undirected" -- this clause collects all the nodes that are subjects
	 *             or objects of triples with the specified predicates.
	 *       </ul> 
	 *       This statement defines a generator with as single clause:
	 *       <pre>
	 *       registerGenerator("involved-with",
	 *            new Object[]{ "objects-of", "!ex:kisses", "!ex:intimateWith" });
	 *       </pre>
	 *   <li>If the first element is the string "select", the second element is a string
	 *        containing a prolog query as in {@link AllegroGraph#select(String)}.  The query
	 *        must specify a single result variable; this variable defines the result set
	 *        of the generator clause.  The pseudo-variable "node" denotes the origin node
	 *        of the search.
	 *        <pre>
	 *        registerGenerator("in-movie-with",
	 *            new Object[]{ "select", "(?x) (q ?x !franz:stars node)" });
	 *        </pre>
	 *        <p>
	 *        Additional elements in the clause specify a prolog variable and a value.
	 *        To specify a predicate obtained from the application:
	 *        <pre>
	 *        registerGenerator("in-movie-with",
	 *            new Object[]{ "select", "(?x) (q ?x ?pred node)", "?pred", v });
	 *        </pre>
	 *   </ul>
	 * @throws AllegroGraphException
	 */
	public void registerGenerator ( String name, Object[] parts ) throws AllegroGraphException {
		// parts -> single-part
		//       -> Object[]{ single-part, ... }
		// single-part -> Object[]{ String-kind, Object-arg... }
		//       kind              arg
		//     "objects-of"       part-ref ...
		//    "subjects-of"
		//    "undirected"
		//    "select"            String-clauses,   String-var, Object-value. ... }
		//  part-ref is any argument acceptable to addStatement.
		if ( null==parts ) {
			ag.verifyEnabled().applyAGFn(ag, AGC.AG_REGISTER_GENERATOR, new Object[0]);
			return;
		}
		if ( 0==parts.length ) return;
		Object part = parts[0];
		ArrayList<Object> decoded = new ArrayList<Object>();
		decoded.add(name);
		if ( part instanceof String )
			decodePart(parts, decoded);
		else if ( part instanceof Object[] )
			for (int i = 0; i < parts.length; i++) {
				decodePart((Object[]) parts[i], decoded);
			}
		else throw new IllegalArgumentException("Generator parts array is not well-formed.");

		ag.verifyEnabled().applyAGFn(ag, AGC.AG_REGISTER_GENERATOR, decoded.toArray());
	}
	
	private void decodePart ( Object[] part, ArrayList<Object> decoded ) {
			if ( part==null ) return;
			decoded.add(new Integer(part.length));
			boolean refParts = false;
			for (int i = 0; i < part.length; i++) {
				Object item = part[i];
				String key;
				if ( 0==i ) 
				{
					if ( !(item instanceof String) )
						throw new IllegalArgumentException("First item in generator part must be a string.");
					key = (String) item;
					if ( "objects-of".equalsIgnoreCase(key) 
							|| "subjects-of".equalsIgnoreCase(key)
							|| "undirected".equalsIgnoreCase(key) )
						refParts = true;
					else if ( "select".equalsIgnoreCase(key) )
						refParts = false;
					else throw new IllegalArgumentException("Unknown generator part " + key);				 
				}
				else if ( refParts ) item = ag.validRef(item);
				decoded.add(item);
			}
		}
		 
	/**
	 * Search the triple store in breadth first fashion starting at start and
	 * looking for end, using generator to expand the search space.
	 * @param start a string, Value instance or UPI instance that identifies the start
	 *      node of the search.
	 * @param end a string, Value instance or UPI instance that identifies the end
	 *      node of the search.
	 * @param generator a string that specifies a generator defined previously 
	 *     with {@link #registerGenerator(String, Object[])}
	 * @param depth an integer that limits the maximum depth of the search. 
	 *     A zero or negative value specifies an unrestricted search.
	 *     In large graphs, this can be <strong>very</strong> expensive.
	 * @return an array of UPI instances containing the nodes on the first path found.
	 *     If no path was found, this is an array of length zero.
	 *     If a path is found, the start and end nodes will always be included.
	 * @throws AllegroGraphException
	 */
		public UPI[] breadthFirstSearch ( Object start, Object end, String generator, int depth ) throws AllegroGraphException {
			return getUPIArray(ag.verifyEnabled().applyAGFn(ag, AGC.AG_BREADTH_FIRST_SEARCH, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator, new Integer(depth)
							}
						));
		}
		
		/**
		 * Search the triple store in breadth first fashion starting at start and
		 * looking for end, using generator to expand the search space, and apply a server function
		 * to each path that is found.
		 * @param start a string, Value instance or UPI instance that identifies the start
		 *      node of the search.
		 * @param end a string, Value instance or UPI instance that identifies the end
		 *      node of the search.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param fn a string that specifies a function in the server.
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 *     In large graphs, this can be <strong>very</strong> expensive.
		 * @throws AllegroGraphException
		 */
		public void mapBreadthFirstSearch ( Object start, Object end, String generator, String fn, int depth ) throws AllegroGraphException {
			ag.verifyEnabled().applyAGFn(ag, AGC.AG_BREADTH_FIRST_MAP, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator, fn, new Integer(depth)
							}
						);
		}
		
		/**
		 * Expect { rows len0 UPI00 UPI01 ... len1 UPI10 UPI11 ... }
		 * @param w the array of values returned from the server (without the leading 2 slots)
		 * @return
		 */
		private UPI[][] toPathArray ( Object [] w ) {
			Object w0 = w[0];
			Object[] v;
			if ( w0==null ) return new UPI[0][0];
			if ( w0 instanceof UPI[] ) v = (Object[]) w0;
			else if ( w0.getClass().isArray() )
			{
				w0 = Array.get(w0, 0);
				if ( (AGConnector.hasLongValue(w0)) && 0==AGConnector.longValue(w0) )
					return new UPI[0][0];
				throw new IllegalStateException("Unexpected path array " + w0);
			}
			else throw new IllegalStateException("Unexpected path array " + w0);
			int len = (int) AGConnector.longValue(v[0]);
			UPI[][] r = new UPI[len][];  int row = 0;
			int i = 1;
			while ( i<v.length ) {
				int width = (int) AGConnector.longValue(v[i]); i++;
				UPI[] p = new UPI[width];
				for ( int j=0; j<width; j++ ) {
					p[j] = (UPI)v[i]; i++;
				}
				r[row] = p; row++;
			}
			return r; 
		}
		
		/**
		 * Search the triple store in breadth first fashion starting at start and
		 * looking for end, using generator to expand the search space.
		 * @param start a string, Value instance or UPI instance that identifies the start
		 *      node of the search.
		 * @param end a string, Value instance or UPI instance that identifies the end
		 *      node of the search.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 *     In large graphs, this can be <strong>very</strong> expensive.
		 * @return an array of paths.  Each element is an array of UPI instances 
		 *     representing the nodes in one path.
		 *     If no paths were found, this is an array of length zero.
		 *
		 * @throws AllegroGraphException
		 */
		public UPI[][] allBreadthFirstSearchPaths ( Object start, Object end, String generator, int depth ) throws AllegroGraphException {
			return toPathArray(ag.verifyEnabled().applyAGFn(ag, AGC.AG_BREADTH_FIRST_ALL, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator, new Integer(depth)
							}
						));                  
		}
		
		/**
		 * Search the triple store in depth first fashion starting at start and
		 * looking for end, using generator to expand the search space.
		 * @param start a string, Value instance or UPI instance that identifies the start
		 *      node of the search.
		 * @param end a string, Value instance or UPI instance that identifies the end
		 *      node of the search.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 *     In large graphs, this can be <strong>very</strong> expensive.
		 * @return an array of UPI instances containing the nodes on the first path found.
		 *     If no path was found, this is an array of length zero.
		 *     If a path is found, the start and end nodes will always be included.
		 * @throws AllegroGraphException
		 */
		public UPI[] depthFirstSearch ( Object start, Object end, String generator, int depth ) throws AllegroGraphException {
			return getUPIArray(ag.verifyEnabled().applyAGFn(ag, AGC.AG_DEPTH_FIRST_SEARCH, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator, new Integer(depth)
							}
						));
		}
		
		
		/**
		 * Search the triple store in depth first fashion starting at start and
		 * looking for end, using generator to expand the search space, and apply a server function
		 * to each path that is found.
		 * @param start a string, Value instance or UPI instance that identifies the start
		 *      node of the search.
		 * @param end a string, Value instance or UPI instance that identifies the end
		 *      node of the search.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param fn a string that specifies a function in the server.
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 *     In large graphs, this can be <strong>very</strong> expensive.
		 * @throws AllegroGraphException
		 */
		public void mapDepthFirstSearch ( Object start, Object end, String generator, String fn, int depth ) throws AllegroGraphException {
			ag.verifyEnabled().applyAGFn(ag, AGC.AG_DEPTH_FIRST_MAP, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator, fn, new Integer(depth)
							}
						);
		}
		
		/**
		 * Search the triple store in depth first fashion starting at start and
		 * looking for end, using generator to expand the search space.
		 * @param start a string, Value instance or UPI instance that identifies the start
		 *      node of the search.
		 * @param end a string, Value instance or UPI instance that identifies the end
		 *      node of the search.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 *     In large graphs, this can be <strong>very</strong> expensive.
		 * @return an array of paths.  Each element is an array of UPI instances 
		 *     representing the nodes in one path.
		 *     If no paths were found, this is an array of length zero.
		 *
		 * @throws AllegroGraphException
		 */
		public UPI[][] allDepthFirstSearchPaths ( Object start, Object end, String generator, int depth ) throws AllegroGraphException {
			return toPathArray(ag.verifyEnabled().applyAGFn(ag, AGC.AG_DEPTH_FIRST_ALL, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator, new Integer(depth)
							}
						));  
		}
		
		/**
		 * Find one path using bidirectional search.
		 * @param start a string, Value instance or UPI instance that identifies the start
		 *      node of the search.
		 * @param end a string, Value instance or UPI instance that identifies the end
		 *      node of the search.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 *     In large graphs, this can be <strong>very</strong> expensive.
		 * 
		 * @return an array of UPI instances containing the nodes on the first path found.
		 *     If no path was found, this is an array of length zero.
		 *     If a path is found, the start and end nodes will always be included.
		 * @throws AllegroGraphException
		 */
		public UPI[] bidirectionalSearch ( Object start, Object end, String generator, int depth ) throws AllegroGraphException {
			return getUPIArray(ag.verifyEnabled().applyAGFn(ag, AGC.AG_BIDIRECTIONAL_SEARCH, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator, new Integer(depth)
							}
						));
		}
		
		/**
		 * Search for paths using bidirectional search and apply a server function to each.
		 * @param start a string, Value instance or UPI instance that identifies the start
		 *      node of the search.
		 * @param end a string, Value instance or UPI instance that identifies the end
		 *      node of the search.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param fn a string that names a suitable server function.
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 *     In large graphs, this can be <strong>very</strong> expensive.
		 * @throws AllegroGraphException
		 */
		public void mapBidirectionalSearch ( Object start, Object end, String generator, String fn, int depth ) throws AllegroGraphException {
			ag.verifyEnabled().applyAGFn(ag, AGC.AG_BIDIRECTIONAL_MAP, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator, fn, new Integer(depth)
							}
						);
		}
		
		/**
		 * Find all paths using bidirectional search.
		 * @param start a string, Value instance or UPI instance that identifies the start
		 *      node of the search.
		 * @param end a string, Value instance or UPI instance that identifies the end
		 *      node of the search.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 *     In large graphs, this can be <strong>very</strong> expensive.
		 * @return an array of paths.  Each element is an array of UPI instances 
		 *     representing the nodes in one path.
		 *     If no paths were found, this is an array of length zero.
		 * @throws AllegroGraphException
		 */
		public UPI[][] allBidirectionalSearchPaths ( Object start, Object end, String generator, int depth ) throws AllegroGraphException {
			return toPathArray(ag.verifyEnabled().applyAGFn(ag, AGC.AG_BIDIRECTIONAL_ALL, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator, new Integer(depth)
							}
						));  
		}
		
		/**
		 * Bipartite breadth first search.
		 * @param start a string, Value instance or UPI instance that identifies the start
		 *      node of the search.
		 * @param end a string, Value instance or UPI instance that identifies the end
		 *      node of the search.
		 * @param generator1 a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param generator2 a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param fn a string that names a suitable server function.
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 * @throws AllegroGraphException
		 */
		public void mapBreadthFirstSearchPathsBipartite ( Object start, Object end, String generator1, String generator2, String fn, int depth ) throws AllegroGraphException {
			ag.verifyEnabled().applyAGFn(ag, AGC.AG_BREADTH_FIRST_BIPARTITE, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator1, generator2, fn, new Integer(depth)
							}
						);
		}
		
		/**
		 * Bipartite depth first search.
		 * @param start a string, Value instance or UPI instance that identifies the start
		 *      node of the search.
		 * @param end a string, Value instance or UPI instance that identifies the end
		 *      node of the search.
		 * @param generator1 a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param generator2 a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param fn a string that names a suitable server function.
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 * @throws AllegroGraphException
		 */
		public void mapDepthFirstSearchPathsBipartite ( Object start, Object end, String generator1, String generator2, String fn, int depth ) throws AllegroGraphException {
			ag.verifyEnabled().applyAGFn(ag, AGC.AG_DEPTH_FIRST_BIPARTITE, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator1, generator2, fn, new Integer(depth)
							}
						);
		}
		
		/**
		 * Bipartite bidirectional first search.
		 * @param start a string, Value instance or UPI instance that identifies the start
		 *      node of the search.
		 * @param end a string, Value instance or UPI instance that identifies the end
		 *      node of the search.
		 * @param generator1 a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param generator2 a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param fn a string that names a suitable server function.
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 * @throws AllegroGraphException
		 */
		public void mapBidirectionalSearchPathsBipartite ( Object start, Object end, String generator1, String generator2, String fn, int depth ) throws AllegroGraphException {
			ag.verifyEnabled().applyAGFn(ag, AGC.AG_BIDIRECTIONAL_BIPARTITE, 
						new Object[] {
						    ag.validRef(start), ag.validRef(end), generator1, generator2, fn, new Integer(depth)
							}
						);
		}
		
		
		
		
		/**
		 * Find the nodes connected to a given node.
		 * The direction of the arcs is determined by the generator.
		 * @param node a string, Value instance or UPI instance that identifies a node in the graph.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @return an array of UPI instances
		 * @throws AllegroGraphException
		 */
		public UPI[] getNodalNeighbors ( Object node, String generator ) throws AllegroGraphException {
			return getUPIArray(ag.verifyEnabled().applyAGFn(ag, AGC.AG_NODAL_NEIGHBORS, 
					new Object[] { ag.validRef(node), generator }
			));
		}
		
		

		/**
		 * Find the number of nodes connected to a given node.
		 * The direction of the arcs is determined by the generator.
		 * @param node a string, Value instance or UPI instance that identifies a node in the graph.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @return an integer
		 * @throws AllegroGraphException
		 */
		public long getNodalDegree ( Object node, String generator ) throws AllegroGraphException {
			Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_NODAL_DEGREE, 
					new Object[] { ag.validRef(node), generator } );
			if ( v==null ) return 0;
			if ( 0==v.length ) return 0;
			return AGConnector.longValue(v[0]);
		}
		
		/**
		 * Find the ego group of a node.
		 * The ego group of a node is the set of other nodes in the graph 
		 * (represented by the triple-store) that can be reached by following 
		 * paths of at most length depth starting at node. 
		 * The paths are determined using the generator to select neighbors 
		 * of each node in the ego-group. 
		 * @param node a string, Value instance or UPI instance that identifies a node in the graph.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param depth an integer that limits the maximum depth of the search. 
		 *     A zero or negative value specifies an unrestricted search.
		 * @return an array of UPI instances representing the nodes in the ego group
		 * @throws AllegroGraphException
		 */
		public UPI[] getEgoGroup ( Object node, String generator, int depth ) throws AllegroGraphException {
			return getUPIArray(ag.verifyEnabled().applyAGFn(ag, AGC.AG_EGO_GROUP, 
					new Object[] { ag.validRef(node), new Integer(depth), generator } ));
		}
		
		/**
		 * Determine the density of a subgraph.
		 * This is the normalized average degree of the actors in the graph.
		 * Roughly, how many of the possible connections between actors are
		 * realized.
		 * @param subgraph a string, Value instance or UPI instance, or an array of these.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @return the numeric value
		 * @throws AllegroGraphException
		 */
		public double getDensity ( Object subgraph, String generator ) throws AllegroGraphException {
			Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_DENSITY, 
					new Object[] { ag.validRefs(subgraph), generator } );
			if ( v==null ) return 0;
			if ( 0==v.length ) return 0;
			return AGConnector.doubleValue(v[0]);
		}
		
		/**
		 * Determine the centrality of an actor or group.
		 * An actor centrality measure provides insight into how central or
		 * important an actor is in the group. Degree-centrality uses the
		 * number of actor connections (the degree) as a proxy for importance.
		 * @param actor If this argument is null, determine the group centrality.
		 *     Otherwise, this argument is a string, Value instance or UPI instance 
		 *     that identifies a node in the graph.
		 * @param group a string, Value instance or UPI instance, or an array of these.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @return the numeric value
		 * @throws AllegroGraphException
		 */
		public double getDegreeCentrality ( Object actor, Object group, String generator ) throws AllegroGraphException {
		// Actor is a node in the graph;  or null for group centrality
		// group is a list of nodes (e.g., the actor's ego group)
			Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_DEGREE_CENTRALITY, 
					new Object[] { ag.validRef(actor, null), ag.validRefs(group), generator } );
			if ( v==null ) return 0;
			if ( 0==v.length ) return 0;
			return AGConnector.doubleValue(v[0]);
		}
		
		/**
		 * Determine the closeness centrality of an actor or group.
		 * An actor centrality measure provides insight into how central
		 * or important an actor is in the group. Closeness-centrality is
		 * the (normalized) inverse average path length of all the shortest
		 * paths between the actor and every other member of the group.
		 * (The inverse so that higher values indicate more central actors).
		 * @param actor If this argument is null, determine the group centrality.
		 *     Otherwise, this argument is a string, Value instance or UPI instance 
		 *     that identifies a node in the graph.
		 * @param group a string, Value instance or UPI instance, or an array of these.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @return the numeric value
		 * @throws AllegroGraphException
		 */
		public double getClosenessCentrality ( Object actor, Object group, String generator ) throws AllegroGraphException {
			Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_CLOSENESS_CENTRALITY, 
					new Object[] { ag.validRef(actor, null), ag.validRefs(group), generator } );
			if ( v==null ) return 0;
			if ( 0==v.length ) return 0;
			return AGConnector.doubleValue(v[0]);
		}
		
		/**
		 * Determine the betweenness centrality of an actor or group.
		 * An actor centrality measure provides insight into how central
		 * or important an actor is in the group. Betweenness-centrality
		 * measures how much control an actor has over communication
		 * in the group.
		 * <p>
		 * The actor-betweenness-centrality of actor <i>i</i> is computed by
		 * counting the number of shortest paths between all pairs of
		 * actors (not including <i>i</i>) that pass through actor <i>i</i>. The
		 * assumption being that this is the chance that actor <i>i</i> can control
		 * the interaction between <i>j</i> and <i>k</i>.
		 * 
		 * @param actor If this argument is null, determine the group centrality.
		 *     Otherwise, this argument is a string, Value instance or UPI instance 
		 *     that identifies a node in the graph.
		 * @param group a string, Value instance or UPI instance, or an array of these.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @return the numeric value
		 * @throws AllegroGraphException
		 */
		public double getBetweennessCentrality ( Object actor, Object group, String generator ) throws AllegroGraphException {
			Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_BETWEENNESS_CENTRALITY, 
					new Object[] { ag.validRef(actor, null), ag.validRefs(group), generator } );
			if ( v==null ) return 0;
			if ( 0==v.length ) return 0;
			return AGConnector.doubleValue(v[0]);
		}
		
		/**
		 * Determine if a subgraph represents a clique.
		 *
		 * @param subgraph a string, Value instance or UPI instance, or an array of these.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @return true if every member of subgraph is linked to every other
		 * @throws AllegroGraphException
		 */
		public boolean isClique ( Object subgraph, String generator ) throws AllegroGraphException {
			Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_IS_CLIQUE, 
						new Object[] {
						    ag.validRefs(subgraph), generator } );  
			if ( null==v ) return false;
			if ( 0==v.length ) return false;
			if ( null!=v[0] ) return true;
			return false;
		}
		
		/**
		 * Find all the cliques of which node is a member
		 * @param node a string, Value instance or UPI instance that identifies a node in the graph.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param minSize an integer that specifies the minimum size of each clique. 
		 *     A zero or negative value specifies an unlimited size.
		 * @return an array of cliques.  Each element is an array of UPI instances 
		 *     representing the nodes in one clique.
		 *     If no cliques were found, this is an array of length zero.
		 * @throws AllegroGraphException
		 */
		public UPI[][] getCliques ( Object node, String generator, int minSize ) throws AllegroGraphException {
			return toPathArray(ag.verifyEnabled().applyAGFn(ag, AGC.AG_CLIQUES, 
						new Object[] {
						    ag.validRef(node), generator, new Integer(minSize)
							}
						));  
		}
		
		/**
		 * Apply a server function to each clique found.
		 * @param node a string, Value instance or UPI instance that identifies a node in the graph.
		 * @param generator a string that specifies a generator defined previously 
		 *     with {@link #registerGenerator(String, Object[])}
		 * @param fn a string that names a suitable server function.
		 * @param minSize an integer that specifies the minimum size of each clique. 
		 *     A zero or negative value specifies an unlimited size.
		 * @throws AllegroGraphException
		 */
		public void mapCliques ( Object node, String generator, String fn, int minSize ) throws AllegroGraphException {
			ag.verifyEnabled().applyAGFn(ag, AGC.AG_MAP_CLIQUES, 
						new Object[] {
						    ag.validRef(node), generator, fn, new Integer(minSize)
							}
						);  
		}
}
