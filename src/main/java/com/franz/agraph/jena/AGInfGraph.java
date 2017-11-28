/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import com.franz.agraph.repository.AGAbstractRepository;
import com.franz.agraph.repository.AGVirtualRepository;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.reasoner.Derivation;
import org.apache.jena.reasoner.InfGraph;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.impl.DatasetImpl;
import org.eclipse.rdf4j.repository.RepositoryException;

import java.util.Iterator;

import static com.franz.agraph.repository.AGVirtualRepository.filteredSpec;
import static com.franz.agraph.repository.AGVirtualRepository.reasoningSpec;

/**
 * Implements the Jena InfGraph interface for AllegroGraph.
 */
public class AGInfGraph extends AGGraph implements InfGraph {

    private final AGReasoner reasoner;
    private final AGGraph rawGraph;

    AGVirtualRepository infRepo;

    AGInfGraph(AGReasoner reasoner, AGGraph rawGraph) {
        super(rawGraph.getGraphMaker(), rawGraph.getGraphContext());
        this.reasoner = reasoner;
        this.rawGraph = rawGraph;
        entailmentRegime = reasoner.getEntailmentRegime();
        if (rawGraph.getGraphContexts().length > 0) {
            // create a reasoning, graph-filtered store over rawGraph's contexts
            AGAbstractRepository repo = rawGraph.getConnection().getRepository();
            String infSpec = reasoningSpec(filteredSpec(repo, rawGraph.getGraphContexts()), entailmentRegime);
            infRepo = repo.getCatalog().getServer().virtualRepository(infSpec);
            try {
                conn = infRepo.getConnection();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            vf = conn.getValueFactory();
        } else {
            // for this common case of reasoning over the whole store,
            // no need to create a reasoning, graph-filtered store
            infRepo = null;
        }
    }

    @Override
    public void close() {
        if (infRepo != null) {
            try {
                conn.close();
                infRepo.close();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
        super.close();
    }

    @Override
    protected Dataset getDataset() {
        // use the whole underlying repository, it is designed to
        // contain just the right set of graphs.
        return new DatasetImpl();
    }

    @Override
    public ExtendedIterator<Triple> find(Node subject, Node property,
                                         Node object, Graph param) {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public Graph getDeductionsGraph() {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public Iterator<Derivation> getDerivation(Triple triple) {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public Node getGlobalProperty(Node property) {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public AGGraph getRawGraph() {
        return rawGraph;
    }

    @Override
    public Reasoner getReasoner() {
        return reasoner;
    }

    @Override
    public void prepare() {
    }

    @Override
    public void rebind() {
    }

    @Override
    public void rebind(Graph data) {
    }

    @Override
    public void reset() {
    }

    @Override
    public void setDerivationLogging(boolean logOn) {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public boolean testGlobalProperty(Node property) {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public ValidityReport validate() {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }


}
