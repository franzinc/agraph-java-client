/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.util;

import java.util.Collection;
import java.util.List;

import test.Util;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Var;

/**
 * Easy access to great Clojure functions.
 */
public class Clj {

    public static Var var(String name) {
        return RT.var(RT.CLOJURE_NS.toString(), name);
    }
    
    /**
     * http://clojuredocs.org/clojure_core/clojure.core/str
     */
    public static String str(Object obj) {
        try {
            return (String) var("str").invoke(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * http://clojuredocs.org/clojure_core/clojure.core/apply
     * http://clojuredocs.org/clojure_core/clojure.core/str
     */
    public static String applyStr(Collection obj) {
        try {
            return (String) var("apply").invoke(var("str"), obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * http://clojuredocs.org/clojure_core/clojure.core/str
     */
    public static String str(Object...obj) {
        try {
            return (String) var("apply").invoke(var("str"), Util.toList(obj));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * http://clojuredocs.org/clojure_core/clojure.core/filter
     */
    public static <Type> List<Type> filter(IFn fn1, Collection<Type> coll) throws Exception {
        return (List<Type>) var("filter").invoke(fn1, coll);
    }
    
    /**
     * http://clojuredocs.org/clojure_core/clojure.core/map
     */
    public static <Type> List<Type> map(IFn fn1, Collection<Type> coll) throws Exception {
        return (List<Type>) var("map").invoke(fn1, coll);
    }
    
    /**
     * http://clojuredocs.org/clojure_core/clojure.core/interpose
     */
    public static List interpose(Object sep, Collection coll) throws Exception {
        return seq( var("interpose").invoke(sep, coll));
    }
    
    /**
     * http://clojuredocs.org/clojure_core/clojure.core/seq
     */
    public static List seq(Object coll) throws Exception {
        return (List) var("seq").invoke(coll);
    }
    
}
