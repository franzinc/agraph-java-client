;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.franz.openrdf
  "Clojure wrapper of the Sesame (org.openrdf) Java API. See http://www.openrdf.org/"
  (:refer-clojure :exclude [name with-open])
  (:import [clojure.lang Named]
           [java.net URI]
           [org.openrdf.model ValueFactory Resource Literal Statement]
           [org.openrdf.repository Repository RepositoryConnection RepositoryResult]
           [org.openrdf.model.vocabulary RDF XMLSchema]
           [org.openrdf.model Statement]
           [org.openrdf.model.impl URIImpl LiteralImpl]
           [org.openrdf.query QueryLanguage Query BindingSet Binding TupleQuery]
           [info.aduna.iteration CloseableIteration Iteration])
  (:use [com.franz util]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defmethod print-method URIImpl [o, #^java.io.Writer w]
  ;; Better to print with <> brackets?
  (.write w (str o)))

(defmethod print-method LiteralImpl [o, #^java.io.Writer w]
  ;; Better to print with <> brackets?
  (.write w (str o)))

(defstruct statement :s :p :o)

(defstruct statement4 :s :p :o :context)

(defmulti to-statement "convert some object to a statement map" type)

(let [convert-keys {"s" :s "o" :o "p" :p}]
  (defmethod to-statement BindingSet
    [#^BindingSet bset]
    #^{:type :statement}
    (loop [binds (iterator-seq (.iterator bset))
           result {}]
      (if (seq binds)
        (let [#^Binding b (first binds)]
          (recur (next binds)
                 (assoc result (get convert-keys (.getName b) (.getName b))
                        (.getValue b))))
        result))))

(defmethod to-statement Statement
  [#^Statement obj]
  #^{:type :statement}
  (if (.getContext obj)
    (struct statement4
            (.getSubject obj)
            (.getPredicate obj)
            (.getObject obj)
            (.getContext obj))
    (struct statement
            (.getSubject obj)
            (.getPredicate obj)
            (.getObject obj))))

(defmethod to-statement :statement [obj] obj)

(defmethod to-statement java.util.Map
  [obj]
  (with-meta obj {:type :statement}))

(defmethod to-statement clojure.lang.Sequential
  [obj]
  (with-meta (if (= 3 (count obj))
               (struct statement (obj 0) (obj 1) (obj 2))
               (struct statement4 (obj 0) (obj 1) (obj 2) (obj 3)))
    {:type :statement}))

;(prefer-method to-statement clojure.lang.Sequential clojure.lang.Associative)

(defmethod close Repository
  [#^Repository obj]
  (.shutDown obj))

(defmethod close RepositoryConnection
  [#^RepositoryConnection obj]
  (.close obj))

(defmethod close CloseableIteration
  [#^CloseableIteration obj]
  (.close obj))

(defn iteration-seq
  "Wraps a Sesame Iteration in a Clojure seq.
  Note, CloseableIteration is not needed because closing is handled by with-open2, open, and close."
  [#^Iteration iter]
  (iterator-seq (proxy [java.util.Iterator] []
                  (next [] (.next iter))
                  (hasNext [] (.hasNext iter)))))

(defn repo-connection
  ""
  ([#^Repository repo]
     (.getConnection repo))
  ([#^Repository repo {auto-commit :auto-commit
                       namespaces :namespaces}]
     (let [#^RepositoryConnection rcon (open (.getConnection repo))]
       (doseq [[#^String prefix #^String name] namespaces]
         (.setNamespace rcon prefix name))
       (when-not (nil? auto-commit)
         (.setAutoCommit rcon #^Boolean auto-commit))
       rcon)))

(defn repo-init
  "Warning: the object needs to be closed with (close)."
  [#^Repository repo]
  (.initialize repo)
  repo)

(defn value-factory
  [#^RepositoryConnection rcon]
  (.getValueFactory rcon))

(defn literal
  {:tag Literal}
  ([#^ValueFactory vf value]
     (condp instance? value
       String (.createLiteral vf #^String value)
       (.createLiteral vf value)
       ))
  ([#^ValueFactory vf value arg]
     (.createLiteral vf value arg)))

(defn uri
  {:tag URI}
  ([#^ValueFactory factory uri]
     (.createURI factory uri))
  ([factory namespace local-name]
     (.createURI factory namespace local-name)))

(defn resource-array
  "creates a primitive java array of Resource from the seq"
  ;;{:tag LResource}
  {:inline (fn [contexts] `#^LResource (into-array Resource ~contexts))}
  [resources]
  (into-array Resource resources))

(defn add!
  "Add a statement to a repository.
   Note: the openrdf java api for (.add) also supports adding files; see add-from!"
  ([#^RepositoryConnection rcon
    #^Resource subject
    #^URI predicate
    #^Value object
    ;; TODO: how to pass contexts consistently?
    & contexts]
     (.add rcon subject predicate object (resource-array contexts)))
  ([#^RepositoryConnection rcon, stmt, contexts]
     (if (instance? Statement stmt)
       ;; compiler fails to resolve #^LResource in this one
       (.add rcon #^Statement stmt (into-array Resource contexts))
       (let [stmt (to-statement stmt)]
         (.add rcon (:s stmt) (:p stmt) (:o  stmt)
               (resource-array (if-let [c (:context stmt)] [c] contexts)))))))

(defn add-all!
  "stmts: a seq where each may be a Statement or a (vector subject predicate object)"
  [#^RepositoryConnection rcon,
   stmts & contexts]
  (doseq [st stmts] (add! rcon st contexts)))

(defn add-from!
  ;; Different name from add! to make it less ambiguous.
  "Add statements from a data file.
   See add!
   data: a File, InputStream, or URL.
   contexts: 0 or more Resource objects"
  [#^RepositoryConnection rcon
   data,
   #^String baseURI,
   #^RDFFormat dataFormat,
   & contexts]
  (.add rcon data baseURI dataFormat (resource-array contexts)))

(defn remove!
  [;; clojure compiler bug? error if this is hinted.
   ;; #^RepositoryConnection
   repos-conn
   #^Resource subject
   #^URI predicate
   #^Value object
   & contexts]
  (.remove repos-conn subject predicate object (resource-array contexts)))

(defn clear!
  [#^RepositoryConnection rcon
   & contexts]
  (.clear rcon (resource-array contexts)))

(defn repo-size
  "http://www.openrdf.org/doc/sesame2/2.2/apidocs/org/openrdf/repository/RepositoryConnection.html#size(org.openrdf.model.Resource...)"
  [#^RepositoryConnection repo-con,
   & contexts]
  (. repo-con (size (resource-array contexts))))

(defn prepare-query!
  [#^Query query
   {:keys [dataset bindings max-query-time include-inferred]}]
  (when dataset
    (.setDataset query dataset))
  (when-not (nil? include-inferred)
    (.setIncludeInferred query include-inferred)) 
  (when max-query-time
    (.setMaxQueryTime query max-query-time))
  (doseq [[#^String name val] bindings]
    (.setBinding query name val)))

(defn tuple-query
  "Returns a seq of maps (to-statement).
   Must be called within a with-open2, and this will close the result seq.
  
     qlang:    QueryLanguage.
     baseURI:  optional.
     bindings: optional, map of String names to Value objects.
     prep:     see prepare-query!."
  [;#^RepositoryConnection
   rcon,
   qlang
   query
   {base-uri :base-uri
    dataset :dataset
    bindings :bindings
    max-query-time :max-query-time
    include-inferred :include-inferred
    :as prep}]
  (let [#^TupleQuery q (if base-uri
                         (.prepareTupleQuery #^RepositoryConnection rcon qlang query base-uri)
                         (.prepareTupleQuery #^RepositoryConnection rcon qlang query))]
    (prepare-query! q prep)
    (map to-statement (iteration-seq (open (.evaluate q))))))

(defn query-graph
  "Returns a seq of maps (to-statement).
   Must be called within a with-open2, and this will close the result seq.
  
     qlang:    QueryLanguage.
     baseURI:  optional.
     bindings: optional, map of String names to Value objects.
     prep:     see prepare-query!."
  [;#^RepositoryConnection
   rcon,
   qlang query
   {:keys [base-uri dataset bindings max-query-time include-inferred]
    :as prep}]
  (let [q #^GraphQuery (if base-uri
                          (.prepareGraphQuery #^RepositoryConnection rcon qlang query base-uri)
                          (.prepareGraphQuery #^RepositoryConnection rcon qlang query))]
    (prepare-query! q prep)
    (map to-statement (iteration-seq (open (.evaluate q))))))

(defn query-boolean
  "Returns a boolean.
  
     qlang:    QueryLanguage.
     baseURI:  optional.
     bindings: optional, map of String names to Value objects.
     prep:     see prepare-query!"
  [;#^RepositoryConnection
   rcon
   qlang query
   {:keys [base-uri dataset bindings max-query-time include-inferred]
    :as prep}]
  (let [q #^BooleanQuery (if base-uri
                           (.prepareBooleanQuery #^RepositoryConnection rcon qlang query base-uri)
                           (.prepareBooleanQuery #^RepositoryConnection rcon qlang query))]
    (prepare-query! q prep)
    (.evaluate q)))

(defn get-statements
  "Returns a seq of maps (to-statement).
  Must be called within a with-open2, and this will close the result seq."
  [;#^RepositoryConnection
   rcon
   #^Resource subj
   #^URI pred
   #^Value obj
   {#^Boolean include-inferred :include-inferred
    #^Boolean filter-dups :filter-dups}
   & contexts]
  (let [#^RepositoryResult result
        (.getStatements rcon subj pred obj
                        (if (nil? include-inferred) false include-inferred)
                        (resource-array contexts))]
    (open result)
    (when filter-dups
      (.enableDuplicateFilter result))
    (map to-statement (iteration-seq result))))
