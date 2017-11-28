package test;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Iterator;

public class JenaSparqlUpdateTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void dataset() throws Exception {
        Dataset ds = DatasetFactory.create();
        Iterator<String> it = ds.listNames();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
        UpdateRequest request = UpdateFactory.read(
                Util.resourceAsStream("/test/insertdata2.ru"));
        UpdateAction.execute(request, ds);
        it = ds.listNames();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
        // TODO: It would be **really nice** if this 'test' contained any assertions...
    }

}
