;; This software is Copyright (c) Franz, 2009.
;; Franz grants you the rights to distribute
;; and use this software as governed by the terms
;; of the Lisp Lesser GNU Public License
;; (http://opensource.franz.com/preamble.html),
;; known as the LLGPL.

;; This lib wraps parts of com.franz.agbase, with
;; AllegroGraph 3.2, but this socket API is not
;; supported with 4.0.
;; See socket-tutorial.clj

;; To use, you need the agraph jar in the classpath.
;; (add-classpath "file:///.../agraph-fje-3.2/agraph-3-2.jar")

(ns franz.agraph3.socket
  (:import [com.franz.agbase AllegroGraphConnection AllegroGraph
            SPARQLQuery]))

(defmethod close AllegroGraphConnection
  [#^AllegroGraphConnection conn]
  (.disable conn))

(defmethod close AllegroGraph
  [#^AllegroGraph obj]
  (.closeTripleStore obj))

(defn connect
  ([] (connect nil))
  ([#^Integer port]
     (let [conn (AllegroGraphConnection.)]
       (when port (.setPort conn port))
       (.enable conn)
       conn)))

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

(defmacro with-agraph
  "ts-sym and init are optional.
Example: (with-agraph [conn {:port 4126}] (info conn))
Example: (with-agraph [conn {:port 4126}, ts (.open conn ts-name ds-dir)] (info conn))"
  [[conn-sym {port :port}
    ts-sym init]
   & body]
  `(with-openm [~conn-sym (connect ~port)
                ~@(when ts-sym [ts-sym init])]
     ~@body))

(defn create-triple-store!
  [#^AllegroGraphConnection conn
   #^String named
   #^String ds-dir]
  (let [ts (.create conn name ds-dir)]
    ts))

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
