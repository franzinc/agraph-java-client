package test;

import junit.framework.Assert;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.junit.Test;

public class ContextsVarargsTest {

    /**
     * Demos various ways of passing in contexts using
     * varargs or an explicit Resource array, to help
     * clarify and avoid pitfalls.
     */
    @Test
    public void howToPassContexts() {
        ValueFactory vf = new ValueFactoryImpl();

        // null array
        Assert.assertEquals(null, m((Resource[]) null)); // this is *not* the null context

        // empty array
        Assert.assertEquals(0, m().length); // using varargs
        Assert.assertEquals(0, m(new Resource[0]).length); // using explicit array

        // non-empty array, using varargs
        Assert.assertEquals(1, m((Resource) null).length); // just the null context
        Assert.assertEquals(1, m(vf.createIRI("http://a")).length);
        Assert.assertEquals(3, m(null, vf.createIRI("http://a"), vf.createIRI("http://b")).length);

        // non-empty, using an explicit array
        Assert.assertEquals(1, m(new Resource[] {null}).length); // just the null context
        Assert.assertEquals(1, m(new Resource[] {vf.createIRI("http://a")}).length);
        Assert.assertEquals(3, m(new Resource[] {null, vf.createIRI("http://a"), vf.createIRI("http://b")}).length);
    }

    /**
     * An arbitrary method taking contexts as a vararg.
     *
     * @param contexts the contexts as a vararg
     * @return the underlying Resource array of the supplied contexts
     */
    public Resource[] m(Resource... contexts) {
        return contexts;
    }
}
