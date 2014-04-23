package test;

import java.util.Iterator;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;

public class JenaSparqlUpdateTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void dataset() throws Exception {
    	Dataset ds = DatasetFactory.createMem();
    	Iterator<String> it = ds.listNames();
    	while (it.hasNext()) {
    		System.out.println(it.next());
    	}
    	GraphStore graphStore = GraphStoreFactory.create(ds) ;
    	UpdateAction.readExecute("src/test/insertdata2.ru", graphStore);
    	Dataset ds2 = graphStore.toDataset();
    	it = ds2.listNames();
    	while (it.hasNext()) {
    		System.out.println(it.next());
    	}
    }

}
