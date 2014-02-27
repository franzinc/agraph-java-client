/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import static test.Util.get;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryResult;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryResult;

import com.franz.util.Closer;

/**
 * A simple implementation of Statement used for testing.
 */
@SuppressWarnings("serial")
public class Stmt implements Statement {
    private static final String XSD_FLOAT = "http://www.w3.org/2001/XMLSchema#float";
    private static final String XSD_LONG = "http://www.w3.org/2001/XMLSchema#long";
    private static final String XSD_DATETIME = "http://www.w3.org/2001/XMLSchema#dateTime";
    private static final String XSD_DOUBLE = "http://www.w3.org/2001/XMLSchema#double";

    private final Resource s;
    private final URI p;
    private final Value o;
    private final Resource c;
    
    public Stmt(Resource s, URI p, Value o) {
        this(s, p, o, null);
    }
    
    public Stmt(Statement s) {
        this(s.getSubject(), s.getPredicate(), s.getObject(), s.getContext());
    }
    
    static Value value(BindingSet s, String bindingName) {
        Binding b = s.getBinding(bindingName);
        return b == null ? null : b.getValue();
    }
    
    public static Set<Stmt> stmts(Stmt... stmts) {
        HashSet<Stmt> set = new HashSet<Stmt>(Arrays.asList(stmts));
        set.remove(null);
        return set;
    }

    public static Stmt spog(BindingSet s, String... SPOGnames) {
        return new Stmt((Resource)value(s, get(SPOGnames, 0, "s")),
                        (URI)value(s, get(SPOGnames, 1, "p")),
                        (Value)value(s, get(SPOGnames, 2, "o")),
                        (Resource)value(s, get(SPOGnames, 3, "g")));
    }
   
    public static Set<Stmt> statementSet(RepositoryResult<Statement> results) throws Exception {
        try {
            Set<Stmt> ret = new HashSet<Stmt>();
            while (results.hasNext()) {
                ret.add(new Stmt(results.next()));
            }
            return ret;
        } finally {
        	Closer.Close(results);
        }
    }
    
    public static Set<Stmt> stmtsSP(Collection c) throws Exception {
        Set<Stmt> ret = new HashSet<Stmt>();
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            Statement s = (Statement) iter.next();
            ret.add(new Stmt(s.getSubject(), s.getPredicate(), null, null));
        }
        return ret;
    }
    
    public static Set<Stmt> statementSet(QueryResult<Statement> results) throws Exception {
        try {
            Set<Stmt> ret = new HashSet<Stmt>();
            while (results.hasNext()) {
                ret.add(new Stmt(results.next()));
            }
            return ret;
        } finally {
        	Closer.Close(results);
        }
    }
    
    public static Set<Stmt> statementSet(TupleQueryResult result, String... SPOGnames) throws Exception {
        try {
            Set<Stmt> ret = new HashSet<Stmt>();
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                ret.add(Stmt.spog(bindingSet, SPOGnames));
            }
            return ret;
        } finally {
        	Closer.Close(result);
        }
    }

    public static Collection<Stmt> dropSubjects(Collection<Stmt> c) {
        Collection<Stmt> r = new ArrayList<Stmt>(c.size());
        for (Stmt s : c) {
            r.add(new Stmt(null, s.getPredicate(), s.getObject(), s.getContext()));
        }
        return r;
    }
    
    public static Collection<Stmt> dropSubjects(Stmt...c) {
        Collection<Stmt> r = new ArrayList<Stmt>(c.length);
        for (Stmt s : c) {
            r.add(new Stmt(null, s.getPredicate(), s.getObject(), s.getContext()));
        }
        return r;
    }

    public Stmt(Resource s, URI p, Value o, Resource c) {
        this.s = s;
        this.p = p;
        this.o = o;
        this.c = c;
    }
    
    public Resource getSubject() {
        return s;
    }
    public URI getPredicate() {
        return p;
    }
    public Value getObject() {
        return o;
    }
    public Resource getContext() {
        return c;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (false == obj instanceof Statement) {
            return false;
        }
        Statement o = (Statement) obj;
        return eq(getSubject(), o.getSubject()) &&
        eq(getPredicate(), o.getPredicate()) &&
        eq(getObject(), o.getObject()) &&
        eq(getContext(), o.getContext());
    }

    @Override
    public String toString() {
        return "[" + getSubject() + " " +
        getPredicate() + " " +
        getObject() +
        (getContext() == null ? "" : " " + getContext()) +
        "]";
    }
    
    String str(Value o) {
        return o==null? null : "[" + o.getClass() + o + "]";
    }
    
    /**
     * null-safe equals
     */
    static boolean eq(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else if (a.equals(b)) {
            return true;
        } else if (a instanceof Literal && b instanceof Literal) {
            Literal al = (Literal) a;
            Literal bl = (Literal) b;
            if (eq(al.getDatatype(), bl.getDatatype())) {
                if (al.getDatatype() == null) {
                    return false;
                }
                if (eq(al.getDatatype().toString(), XSD_FLOAT)) {
                    //System.out.println("eq float: " + al.floatValue() + " " + bl.floatValue() + " " + Math.abs(al.floatValue() - bl.floatValue()));
                    return Math.abs(al.floatValue() - bl.floatValue()) < 0.001;
                }
                if (eq(al.getDatatype().toString(), XSD_DATETIME)) {
                    //System.out.println("eq dt: " + al.calendarValue().toGregorianCalendar().getTimeInMillis() + " " + bl.calendarValue().toGregorianCalendar().getTimeInMillis());
                    return eq(al.calendarValue().toGregorianCalendar(), bl.calendarValue().toGregorianCalendar());
                }
                if (eq(al.getDatatype().toString(), XSD_LONG)) {
                    //System.out.println("eq long: " + al.longValue() + " " + bl.longValue());
                    return al.longValue() == bl.longValue();
                }
                if (eq(al.getDatatype().toString(), XSD_DOUBLE)) {
                	//System.out.println("eq long: " + al.longValue() + " " + bl.longValue());
                	return al.doubleValue() == bl.doubleValue();
                }
            }
        }
        return false;
    }
    
}
