package test;

import junit.framework.Assert;

import org.junit.Test;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

public class ContextsVarargsTest {

	/**
	 * Demos various ways of passing in contexts using 
	 * varargs or an explicit Resource array, to help
	 * clarify and avoid pitfalls.  
	 */
	@Test
	public void howToPassContexts () {
		ValueFactory vf = new ValueFactoryImpl();
		
		// null array
		Assert.assertEquals(null,m((Resource[])null)); // this is *not* the null context

		// empty array
		Assert.assertEquals(0, m().length); // using varargs
		Assert.assertEquals(0, m(new Resource[0]).length); // using explicit array
		
		// non-empty array, using varargs
		Assert.assertEquals(1, m((Resource)null).length); // just the null context
		Assert.assertEquals(1, m(vf.createURI("http://a")).length);
		Assert.assertEquals(3, m(null,vf.createURI("http://a"),vf.createURI("http://b")).length);
		
		// non-empty, using an explicit array
		Assert.assertEquals(1, m(new Resource[]{null}).length); // just the null context
		Assert.assertEquals(1, m(new Resource[]{vf.createURI("http://a")}).length);
		Assert.assertEquals(3, m(new Resource[]{null,vf.createURI("http://a"),vf.createURI("http://b")}).length);
	}
	
	/**
	 * An arbitrary method taking contexts as a vararg.
	 * 
	 * @param contexts the contexts as a vararg
	 * @return the underlying Resource array of the supplied contexts
	 */
	public Resource[] m (Resource... contexts) {
		return contexts;
	}
}
