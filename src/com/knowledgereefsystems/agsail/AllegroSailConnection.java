/* Copyright (C) 2008 Knowledge Reef Systems.  All rights reserved. */

package com.knowledgereefsystems.agsail;

import info.aduna.iteration.CloseableIteration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.openrdf.sail.SailChangedListener;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailConnectionListener;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.DefaultSailChangedEvent;

import com.franz.agbase.AllegroGraphConnection;
import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.SPARQLQuery;
import com.franz.agbase.ValueSetIterator;
import com.franz.agsail.AGForSail;
import com.franz.agsail.AGSailCursor;

public class AllegroSailConnection implements SailConnection {
    private final AGForSail aGraph;
    private final AllegroGraphConnection agConnection;
    private final AllegroSail sail;
    private final ValueFactory valueFactory;
    private final Collection<SailChangedListener> sailChangedListeners;
    private final Set<SailConnectionListener> sailConnectionListeners = new HashSet<SailConnectionListener>();

    private boolean open = true;
    private boolean uncommittedInsertions, uncommittedDeletions;

    public boolean isUncommittedInsertions() {
		return uncommittedInsertions;
	}
	public void setUncommittedInsertions(boolean uncommittedInsertions) {
		this.uncommittedInsertions = uncommittedInsertions;
	}
	public boolean isUncommittedDeletions() {
		return uncommittedDeletions;
	}
	public void setUncommittedDeletions(boolean uncommittedDeletions) {
		this.uncommittedDeletions = uncommittedDeletions;
	}
	
    public AllegroSailConnection(final AGForSail aGraph,
                                 final AllegroGraphConnection agConn,
                                 final AllegroSail sail,
                                 final Collection<SailChangedListener> sailChangedListeners
                                 ) {
        this.aGraph = aGraph;
        this.sail = sail;
        this.valueFactory = sail.getValueFactory();
        agConnection = agConn;

        this.sailChangedListeners = sailChangedListeners;

        uncommittedDeletions = false;
        uncommittedInsertions = false;
    }

    public void addConnectionListener(final SailConnectionListener listener) {
        synchronized (sailConnectionListeners) {
            sailConnectionListeners.add(listener);
        }
    }

    public void addStatement(final Resource subject,
                             final URI predicate,
                             final Value object,
                             final Resource... contexts) throws SailException {
        try {
            if (0 == contexts.length) {
                // Add to the null context.
                //aGraph.evalInServer(createAddTriple(subj, pred, obj, null, true));
                aGraph.addStatement(subject, predicate, object);

                statementAdded(subject, predicate, object, null);
            } else {
                for (Resource c : contexts) {
                    // Add t the null context.
                    if (null == c) {
                        //aGraph.evalInServer(createAddTriple(subj, pred, obj, null, true));
                        aGraph.addStatement(subject, predicate, object);

                        statementAdded(subject, predicate, object, null);
                    }

                    // Add to a non-null context.
                    else {
                        //aGraph.evalInServer(createAddTriple(subj, pred, obj, c, true));
                        aGraph.addStatement(subject, predicate, object, c);

                        statementAdded(subject, predicate, object, c);
                    }
                }
            }

            uncommittedInsertions = true;
        }

        catch (Throwable t) {
            throw new AllegroSailException(t);
        }

        // Note: only one event is generated, regardless of how many statements
        // are added in this operation.
        if (sailChangedListeners.size() > 0) {
            DefaultSailChangedEvent event = new DefaultSailChangedEvent(sail);
            event.setStatementsAdded(true);

            for (SailChangedListener listener : sailChangedListeners) {
                listener.sailChanged(event);
            }
        }
    }

    public void clear(final Resource... contexts) throws SailException {
        // Remove all statements in all contexts.
        if (0 == contexts.length) {
            try {
                // TODO
                aGraph.clear();
                uncommittedDeletions = true;
            } catch (AllegroGraphException e) {
                throw new AllegroSailException(e);
            }
            // Remove statements from individual contexts.
        } else {
            // Slow but effective...
            removeStatements(null, null, null, contexts);
        }
    }

    public void clearNamespaces() throws SailException {
        String[] nsArray = agConnection.getNamespaces();

        for (int i = 0; i < nsArray.length; i += 2) {
            removeNamespace(nsArray[i]);
        }
    }

    public void close() throws SailException {
        // There is only a single AllegroGraphConnection, so
        // individual AllegroSailConnections do not close it.
        open = false;
    }

    public void commit() throws SailException {
    	if ( sail.isReindexOnCommit() && (uncommittedInsertions || uncommittedDeletions) )
    	{
    		try {
    			if ( sail.isIndexAllOnCommit() )
    				aGraph.indexAllTriples(!sail.isBackgroundIndexing());
    			else
    				aGraph.indexNewTriples(!sail.isBackgroundIndexing());

    		} catch (AllegroGraphException e) {
    			throw new AllegroSailException(e);
    		}

    		uncommittedInsertions = false;
    		uncommittedInsertions = false;
    	}
    }

    public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(final TupleExpr tupleExpr,
                                                                                       final Dataset dataSet,
                                                                                       final BindingSet bindings,
                                                                                       final boolean includeInferred)
            throws SailException {
        return evaluateByDecomposition(tupleExpr, dataSet, bindings, includeInferred);
    }

    /**
     * @return an iteration containing context IDs.  There does not appear
     *         to be an inexpensive way to find the identifiers of all named 
     *         graphs in an AllegroGraph triple store.
     * @throws SailException
     */
    public CloseableIteration<? extends Resource, SailException> getContextIDs()
            throws SailException {
    	
    	SPARQLQuery sq = new SPARQLQuery();
    	sq.setTripleStore(aGraph.getDirectInstance());
    	sq.setQuery("SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o }}");
		ValueSetIterator it;
		try {
			it = sq.select();
		} catch (AllegroGraphException e) {
			throw new SailException(e); 
		}  
       return new ContextIteration(aGraph, it);
    }

    public String getNamespace(final String prefix) throws SailException {
        String[] nsArray;

        try {
            nsArray = agConnection.getNamespaces();
        }

        catch (Throwable t) {
            throw new AllegroSailException(t);
        }

        for (int i = 0; i < nsArray.length; i += 2) {
            if (nsArray[i].equals(prefix)) {
                return nsArray[i + 1];
            }
        }

        return null;
    }

    public CloseableIteration<? extends Namespace, SailException> getNamespaces()
            throws SailException {
        String[] nsArray;

        try {
            // Can't get individual namespaces from AllegroGraph.
            nsArray = aGraph.getDirectInstance().getNamespaces();
        }

        catch (Throwable t) {
            throw new AllegroSailException(t);
        }

        return new NamespaceIteration(nsArray);
    }

    public CloseableIteration<? extends Statement, SailException> getStatements(final Resource subject,
                                                                                final URI predicate,
                                                                                final Value object,
                                                                                boolean includeInferred,
                                                                                final Resource... contexts)
            throws SailException {
        AGSailCursor[] cursors;

        try {
            if (0 == contexts.length) {
                cursors = new AGSailCursor[1];
                // Get statements in any context.
                cursors[0] = aGraph.getStatements(includeInferred, subject, predicate, object, null);
            } else {
                cursors = new AGSailCursor[contexts.length];
                for (int i = 0; i < contexts.length; i++) {
                    Resource c = contexts[i];
                    cursors[i] = (null == c)
                            // Get statements in any context.
                            ? aGraph.getStatements(includeInferred, subject, predicate, object)
                            // Get statements in a non-null context.
                            : aGraph.getStatements(includeInferred, subject, predicate, object, c);
                }
            }
        }

        catch (Throwable t) {
            throw new AllegroSailException(t);
        }

        return new StatementIteration(cursors, valueFactory);
    }

    public boolean isOpen() throws SailException {
        return open;
    }

    public void removeConnectionListener(final SailConnectionListener listener) {
        synchronized (sailConnectionListeners) {
            sailConnectionListeners.remove(listener);
        }
    }

    public void removeNamespace(final String prefix) throws SailException {
        try {
            agConnection.registerNamespace(prefix, null);
        }

        catch (Throwable t) {
            throw new AllegroSailException(t);
        }
    }

    public void removeStatements(final Resource subject,
                                 final URI predicate,
                                 final Value object,
                                 final Resource... contexts) throws SailException {
        try {
            if (0 == contexts.length) {
                // Remove statements in any context.
                aGraph.removeStatements(subject, predicate, object);

                // Note: this may not be a "real" statement
                statementRemoved(subject, predicate, object, null);

                uncommittedDeletions = true;
            } else {
                for (Resource c : contexts) {
                	aGraph.removeStatements(subject, predicate, object, c);

                    // Note: this may not be a "real" statement
                    statementRemoved(subject, predicate, object, c);

                    uncommittedDeletions = true;
                }
            }
        } catch (Throwable t) {
            throw new AllegroSailException(t);
        }

        // Note: doesn't check whether any statements were actually removed,
        // only that this method was called.  Exactly one event is generated,
        // regardless of how many statements are actually removed in this
        // operation.
        if (sailChangedListeners.size() > 0) {
            DefaultSailChangedEvent event = new DefaultSailChangedEvent(sail);
            event.setStatementsRemoved(true);

            for (SailChangedListener listener : sailChangedListeners) {
                listener.sailChanged(event);
            }
        }
    }

    public void rollback() throws SailException {
        uncommittedInsertions = uncommittedDeletions = false;
    }

    public void setNamespace(final String prefix,
                             final String name) throws SailException {
        try {
            agConnection.registerNamespace(prefix, name);
        }

        catch (Throwable t) {
            throw new AllegroSailException(t);
        }
    }

    public long size(final Resource... contexts) throws SailException {
        if (0 == contexts.length) {
            try {
                return aGraph.numberOfTriples();
            } catch (AllegroGraphException e) {
                throw new AllegroSailException(e);
            }
        }

        // there is no inexpensive way to implement size() for specific contexts
        else {
            return 0;
        }
    }

    // public methods not in the SailConnection API ////////////////////////////

    public void registerFreetextPredicate(final URI predicate) throws SailException {
        try {
            aGraph.registerFreetextPredicate(predicate);
        } catch (Throwable t) {
            throw new SailException(t);
        }
    }

    public CloseableIteration<? extends Statement, SailException> getFreetextStatements(final String pattern) throws SailException {
        AGSailCursor[] c = new AGSailCursor[1];

        try {
            c[0] = aGraph.getFreetextStatements(pattern);
        }

        catch (Throwable t) {
            throw new AllegroSailException(t);
        }

        return new StatementIteration(c, valueFactory);
    }

    public CloseableIteration<? extends Resource, SailException> getFreetextUniqueSubjects(final String pattern) throws SailException {
        ValueSetIterator c;

        try {
            c = aGraph.getFreetextUniqueSubjects(pattern);
        }

        catch (Throwable t) {
            throw new AllegroSailException(t);
        }

        return new ResourceIteration(aGraph, c);
    }

    // supporting methods //////////////////////////////////////////////////////

    // Evaluates tuple queries by decomposing them into getStatements queries

    private CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateByDecomposition(final TupleExpr tupleExpr,
                                                                                                       final Dataset dataSet,
                                                                                                       final BindingSet bindings,
                                                                                                       final boolean includeInferred)
            throws SailException {
//evaluateNativeSelect(tupleExpr, dataSet, bindings, includeInferred);
        try {
            TripleSource tripleSource = new SailConnectionTripleSource(this, valueFactory, includeInferred);
            EvaluationStrategyImpl strategy = new EvaluationStrategyImpl(tripleSource, dataSet);

            return strategy.evaluate(tupleExpr, bindings);
        }

        catch (QueryEvaluationException e) {
            throw new SailException(e);
        }
    }

    // TODO: write a SPARQL reserializer
//    private CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateNativeSelect(final TupleExpr tupleExpr,
//                                                                                                    final Dataset dataSet,
//                                                                                                    final BindingSet bindings,
//                                                                                                    final boolean includeInferred)
//            throws SailException {
//        String queryStr = new ParsedTupleQuery(tupleExpr).toString();
//        //System.out.println("queryStr = " + queryStr);
//        int limit = -1;
//        int offset = 0;
//
//        SPARQLQuery sq = new SPARQLQuery();
//        sq.setLimit(limit);
//        sq.setOffset(offset);
//        sq.setIncludeInferred(includeInferred);
//        
//        ValueSetIterator vo;
//		try {
//			vo = sq.select(aGraph.getDirectInstance(), queryStr);
//		} catch (AllegroGraphException e) {
//			throw new SailException(e);
//		}
//
//		int i = 0;
//		while ( vo.hasNext() ) {
//		    System.out.println("vo[" + i + "] = " + vo.get());
//		}
//		return null;
//    }

    private void statementAdded(final Resource subject,
                                final URI predicate,
                                final Value object,
                                final Resource context) {
        if (sailConnectionListeners.size() > 0) {
            Statement st = (null == context)
                    //? new StatementImpl(subj, pred, obj)
                    //: new ContextStatementImpl(subj, pred, obj, context);
                    ? aGraph.createStatement(subject, predicate, object)
                    : aGraph.createStatement(subject, predicate, object, context);
            synchronized (sailConnectionListeners) {
                for (SailConnectionListener l : sailConnectionListeners) {
                    l.statementAdded(st);
                }
            }
        }
    }

    private void statementRemoved(final Resource subject,
                                  final URI predicate,
                                  final Value object,
                                  final Resource context) {
        if (sailConnectionListeners.size() > 0) {
            Statement st = (null == context)
                    ? new StatementImpl(subject, predicate, object)
                    : new ContextStatementImpl(subject, predicate, object, context);
                    //mm: there is no need to create an agraph Statement instance.
                    //   Let Sesame worry about what a null context value means.   [bug18178]
//                    ? aGraph.createStatement(subject, predicate, object)
//                    : aGraph.createStatement(subject, predicate, object, context);
            synchronized (sailConnectionListeners) {
                for (SailConnectionListener l : sailConnectionListeners) {
                    l.statementRemoved(st);
                }
            }
        }
    }
}
