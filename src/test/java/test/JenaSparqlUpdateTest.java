package test;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.update.*;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Iterator;

public class JenaSparqlUpdateTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void dataset() throws Exception {
    	Dataset ds = DatasetFactory.createMem();
    	Iterator<String> it = ds.listNames();
    	while (it.hasNext()) {
    		System.out.println(it.next());
    	}
    	GraphStore graphStore = GraphStoreFactory.create(ds);
		UpdateRequest request = UpdateFactory.read(
				Util.resourceAsStream("/test/insertdata2.ru"));
		UpdateAction.execute(request, graphStore);
    	Dataset ds2 = graphStore.toDataset();
    	it = ds2.listNames();
    	while (it.hasNext()) {
    		System.out.println(it.next());
    	}
    }

}
