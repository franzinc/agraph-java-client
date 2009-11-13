;; This software is Copyright (c) Franz, 2009.
;; Franz grants you the rights to distribute
;; and use this software as governed by the terms
;; of the Lisp Lesser GNU Public License
;; (http://opensource.franz.com/preamble.html),
;; known as the LLGPL.

(ns com.franz.openrdf
  "Clojure wrapper of the Sesame (org.openrdf) Java API. See http://www.openrdf.org/"
  (:refer-clojure :exclude (name))
  (:import [java.net URI]
           [com.franz.agbase AllegroGraph Triple TriplesIterator
            LiteralNode URINode]
           ;; [org.openrdf.model ValueFactory Resource Literal Statement]
           ;; [org.openrdf.repository Repository RepositoryConnection RepositoryResult]
           ;; [org.openrdf.model.vocabulary RDF XMLSchema]
           ;; [org.openrdf.model Statement]
           ;; [org.openrdf.model.impl URIImpl LiteralImpl]
           ;; [org.openrdf.query QueryLanguage Query BindingSet Binding TupleQuery]
           ;; [info.aduna.iteration CloseableIteration Iteration]
           )
  (:use [clojure.contrib def]
        [com.franz util]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

;; (defmethod print-method URIImpl [o, #^java.io.Writer w]
;;   ;; Better to print with <> brackets?
;;   (.write w (str o)))

;; (defmethod print-method LiteralImpl [o, #^java.io.Writer w]
;;   ;; Better to print with <> brackets?
;;   (.write w (str o)))

(defstruct statement :s :p :o)

(defstruct statement4 :s :p :o :context)

(defmulti to-statement "convert some object to a statement map" type)

(defmethod to-statement Triple
  [#^Triple obj]
  #^{:type :statement}
  (if (.getC obj)
    (struct statement4
            (.getS obj)
            (.getP obj)
            (.getO obj)
            (.getC obj))
    (struct statement
            (.getS obj)
            (.getP obj)
            (.getO obj))))

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

(defmethod close TriplesIterator
  [#^TriplesIterator obj]
  (.close obj))

(defn value-factory
  {:tag AllegroGraph}
  [#^AllegroGraph ts]
  ts)

(defn literal
  {:tag LiteralNode}
  ([#^AllegroGraph vf value]
     (condp instance? value
       String (.createLiteral vf #^String value)
       (.createLiteral vf value)
       ))
  ([#^AllegroGraph vf value arg]
     (.createLiteral vf value arg)))

(defn uri
  {:tag URINode}
  ([#^AllegroGraph factory uri]
     (.createURI factory uri))
  ([factory namespace local-name]
     (.createURI factory namespace local-name)))

(defn add!
  "Add a statement to a repository."
  ([#^AllegroGraph ts
    stmt]
     (let [{:keys [s p o c]} (to-statement stmt)]
       (.add ts s p o c))))

(defn add-all!
  "stmts: a seq where each may be a Statement or a (vector subject predicate object)"
  ([#^AllegroGraph ts
    stmts]
     (doseq [st stmts] (add! ts st)))
  ([#^AllegroGraph ts
    stmts context]
     (doseq [st stmts]
       (add! ts (conj (to-statement st) {:context context})))))

(defn remove!
  [#^AllegroGraph ts
   stmt]
  (let [stmt (to-statement stmt)]
    (.removeStatement ts (:s stmt) (:p stmt) (:o stmt) (:context stmt))))

(defn repo-size
  [#^AllegroGraph ts]
  (.numberOfTriples ts))

(defn get-statements
  "Returns a seq of maps (to-statement).
  Must be called within a scope, and this will close the result seq."
  ([#^AllegroGraph ts
    stmt
    {:keys [#^Boolean include-inferred]}]
     (let [{:keys [s p o c]} (to-statement stmt)
           #^TriplesIterator result
           (.getStatements ts s p o
                           (if (nil? include-inferred) false include-inferred)
                           c)]
       (open result)
       (map to-statement (iterator-seq result))))
  ([#^AllegroGraph ts
    stmt]
     (get-statements ts stmt nil)))
