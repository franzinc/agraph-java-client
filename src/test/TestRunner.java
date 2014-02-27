/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static test.Util.logTimeStamped;

/**
 * Different from the JUnitCore runner, this class prints test exceptions as they happen.
 */
public class TestRunner {

    private final static Logger log = LoggerFactory.getLogger(TestRunner.class);

    /**
     * @param args Suite class name to run
     */
    public static void main(String[] args) throws Exception {
        Class suiteClass = Class.forName(args[0]);
        SuiteClasses suite = (SuiteClasses) suiteClass.getAnnotation(SuiteClasses.class);
        IncludeCategory includeCat = (IncludeCategory) suiteClass.getAnnotation(IncludeCategory.class);
        Class<? extends Annotation> include = includeCat == null ? null : (Class) includeCat.value();
        ExcludeCategory excludeCat = (ExcludeCategory) suiteClass.getAnnotation(ExcludeCategory.class);
        Class<? extends Annotation> exclude = excludeCat == null ? null : (Class) excludeCat.value();
        List<String> failures = new ArrayList<String>();
        for (Class testClass : suite.value()) {
            List<Method> testMethods = testMethods(testClass, include, exclude);
            if (testMethods.isEmpty()) {
                continue;
            }
            invokeAll(methodsAnnotated(testClass, BeforeClass.class), false, testClass);
            try {
            	Method setTestName;
            	try {
            		setTestName = testClass.getMethod("setTestName", String.class);
            	} catch (NoSuchMethodException e) {
            		setTestName = null;
            	}
                for (Method m : testMethods) {
                	String fullname = testClass.getName() + "." + m.getName();
                    long start = System.currentTimeMillis();
                    System.out.flush();
                    System.err.flush();
                    System.out.println();
		    logTimeStamped("Testcase: " + fullname);
                    Object test = testClass.newInstance();
                	if (setTestName != null) {
                		setTestName.invoke(test, m.getName());
                	}
                    try {
                        invokeAll(methodsAnnotated(testClass, Before.class), false, test);
                        m.invoke(test);
                        logTimeStamped("SUCCESS Testcase: " + fullname + " took " + (System.currentTimeMillis() - start) + " ms");
                    } catch (Error e) {
                        failures.add(fullname);
                        System.out.flush();
                        logTimeStamped("ERROR Testcase: " + fullname + " took " + (System.currentTimeMillis() - start) + " ms");
                        throw e;
                    } catch (Throwable e) {
                        failures.add(fullname);
                        System.out.flush();
                        if (e instanceof InvocationTargetException) {
                            e = e.getCause();
                        }
                        e.printStackTrace(System.err);
                        logTimeStamped("FAIL Testcase: " + fullname + " took " + (System.currentTimeMillis() - start) + " ms");

			if(System.getProperty("dump-agraph-log-on-test-failure", "false").equals("true")) {
			    Util.DumpAgraphLog();
			}

			if(System.getProperty("test-failure-is-error", "false").equals("true")) {
			    System.exit(-1);
			}

			
                    } finally {
                		System.out.flush();
                    	invokeAllIgnoreExceptions(Util.reverse( methodsAnnotated(testClass, After.class)), false, test);
                		System.out.flush();
                        System.err.flush();
                        Thread.sleep(2);
                    }
                }
            } finally {
        		System.out.flush();
                invokeAllIgnoreExceptions(Util.reverse( methodsAnnotated(testClass, AfterClass.class) ), false, testClass);
            }
        }
        if ( ! failures.isEmpty()) {
        	System.err.println("Failures: " + failures);
        }
        System.exit(failures.isEmpty() ? 0 : -1);
    }

    static List<Method> testMethods(Class c,
            Class<? extends Annotation> include,
            Class<? extends Annotation> exclude) {
        List<Method> r = new ArrayList<Method>();
        for (Method m : methodsAnnotated(c, Test.class)) {
            Category cat = m.getAnnotation(Category.class);
            Collection<Class<?>> cats = cat==null ? new HashSet<Class<?>>() : new HashSet<Class<?>>(Arrays.asList(cat.value()));
            if ((include != null && ! isAssignableFromAny(include, cats)
                    || (exclude != null && isAssignableFromAny(exclude, cats)))) {
            } else {
                r.add(m);
            }
        }
        return r;
    }
    
    static boolean isAssignableFromAny(Class c, Collection<Class<?>> cats) {
        if (c == null) {
            return true;
        }
        if (cats == null) {
            return true;
        }
        for (Class clazz : cats) {
            if (c.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * List of super class first, self class last.
     */
    static List<Class> getSupers(Class cl) {
        List<Class> r = new ArrayList<Class>();
        while (cl != null) {
        	r.add(0, cl);
        	cl = cl.getSuperclass();
        }
    	return r;
    }
    
    static List<Method> methodsAnnotated(Class clazz,
            Class<? extends Annotation> annotationClass) {
        List<Method> r = new ArrayList<Method>();
        List<Class> classes = getSupers(clazz);
    	for (Class c : classes) {
            Method[] methods = c.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
            	Method m = methods[i];
            	Annotation anno = m.getAnnotation(annotationClass);
            	if (anno != null) {
            		r.add(m);
            	}
            }
        }
        return r;
    }
    
    static List<Method> methodsAnnotatedCategory(Class c, Class category) {
        List<Method> r = new ArrayList<Method>();
        for (Method m : c.getMethods()) {
            Category anno = (Category) m.getAnnotation(Category.class);
            if (anno != null) {
                for (Class<?> cat : anno.value()) {
                    if (cat.equals(category)) {
                        r.add(m);
                    }
                }
            }
        }
        return r;
    }
    
    static List<Object> invokeAll(List<Method> methods,
            boolean logExceptions, Object obj, Object...args) throws Exception {
        List<Object> r = new ArrayList<Object>();
        for (Method m : methods) {
            r.add( m.invoke(obj, args) );
        }
        return r;
    }

    static List<Object> invokeAllIgnoreExceptions(List<Method> methods,
            boolean logExceptions, Object obj, Object...args) throws Exception {
        List<Object> r = new ArrayList<Object>();
        for (Method m : methods) {
        	try {
                    r.add( m.invoke(obj, args) );
        	} catch (Exception e) {
                    log.warn("ignoring", e);
        	}
        }
        return r;
    }

}
