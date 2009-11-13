;; This software is Copyright (c) Franz, 2009.
;; Franz grants you the rights to distribute
;; and use this software as governed by the terms
;; of the Lisp Lesser GNU Public License
;; (http://opensource.franz.com/preamble.html),
;; known as the LLGPL.

;;
;; WARNING: This agraph3 code is just experimental.
;; com.franz.agraph is the current code, for AG 4.0
;;

;; This lib wraps parts of com.franz.agbase, with
;; AllegroGraph 3.2, but this socket API is not
;; supported with 4.0.
;; See socket-tutorial.clj

;; To use, you need the agraph jar in the classpath.
;; (add-classpath "file:///.../agraph-fje-3.2/agraph-3-2.jar")

(ns com.franz.agraph3
  (:refer-clojure :exclude (name))
  (:use [com.franz util])
  (:import [com.franz.agbase AllegroGraphConnection AllegroGraph
            SPARQLQuery]))

(defmethod close AllegroGraphConnection
  [#^AllegroGraphConnection conn]
  (.disable conn))

(defmethod close AllegroGraph
  [#^AllegroGraph obj]
  (.closeTripleStore obj))

(defn ag-server
  [{:keys [host port]}]
  (let [#^AllegroGraphConnection server (open (AllegroGraphConnection.))]
    (when host (.setHost server host))
    (when port (.setPort server port))
    (.enable server)
    server))

(defn agraph-open
  [#^AllegroGraphConnection server
   {:keys [db-dir repository]}]
  (open (.open server repository db-dir)))

(defn agraph-renew!
  [#^AllegroGraphConnection server
   {:keys [db-dir repository]}]
  (open (.renew server repository db-dir)))

(defn agraph-create!
  [#^AllegroGraphConnection server
   {:keys [db-dir repository]}]
  (open (.create server repository db-dir)))

(defn agraph-replace!
  [#^AllegroGraphConnection server
   {:keys [db-dir repository]}]
  (open (.replace server repository db-dir)))

;; TODO agraph-federate!

(defmacro bean-props
  [obj props]
  `(into {}
         (list ~@(map (fn [[k v]] [k `(. ~obj (~v))])
                      props))))

(defn info
  "returns a map of properties  of the connection"
  [#^AllegroGraphConnection conn]
  (merge (bean-props conn
                     {:chunk-size getChunkSize
                      :command getCommand
                      :debug getDebug
                      :expectedResources getDefaultExpectedResources
                      :host getHost
                      :mode getMode
                      :pollCount getPollCount
                      :pollInterval getPollInterval
                      :port getPort
                      :port2 getPort2
                      :serverKeep getServerKeep
                      :busy isBusy
                      :enabled isEnabled
                      })
         {:serverLevel-0 (.serverLevel conn 0)}
         ))

(defn eval-in-server
  [#^AllegroGraphConnection conn
   #^String cl-expr]
  (seq (.evalInServer conn cl-expr)))

(defn sparql-query
  [ts query]
  (doto (SPARQLQuery.)
    (.setTripleStore ts)
    (.setQuery query)))

(defn select
  [sparql-query]
  (map seq (iterator-seq (.select sparql-query))))
