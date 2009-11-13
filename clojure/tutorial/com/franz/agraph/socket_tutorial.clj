;; This software is Copyright (c) Franz, 2009.
;; Franz grants you the rights to distribute
;; and use this software as governed by the terms
;; of the Lisp Lesser GNU Public License
;; (http://opensource.franz.com/preamble.html),
;; known as the LLGPL.

;; (add-classpath "file:///.../agraph-fje-3.2/agraph-3-2.jar")

(ns com.franz.agraph.socket-tutorial
  (:refer-clojure :exclude (name))
  (:use [com.franz util agraph3]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defonce *connection-params* {:host "localhost"
                              :port 4567
                              :username "test" :password "xyzzy"
                              :db-dir "/tmp/ag32jee/scratch/"
                              ;; :catalog "scratch"
                              :repository "tutorial"})

(def *agraph-java-tutorial-dir* (.getCanonicalPath (java.io.File. "./tutorial/")))

(defn example-sparql-select
  []
  (scope-let [server (ag-server *connection-params*)
              ts (agraph-renew! server *connection-params*)]
    (doto ts
      (.registerNamespace "ex","http://example.org/")
      (.addStatement "!ex:a","!ex:p", "!ex:b")
      (.addStatement "!ex:b","!ex:p", "!ex:c"))
    
    (let [query "SELECT * {?s ?p ?o}"
          sq (sparql-query ts query)]
      (select sq))))

(defn example-sparql-basic-graph-patterns 
  []
  (scope-let [server (ag-server *connection-params*)
              ts (agraph-renew! server *connection-params*)]
    (doto ts
      (.addStatement "<http://example.org/book/book1>"
                     "<http://purl.org/dc/elements/1.1/title>"
                     (.createLiteral ts "SPARQL Tutorial"))
      )
    (printlns (select (sparql-query ts
                                    (str "SELECT ?title WHERE {"
                                         " <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title ."
                                         " }"))))
    
    (let [a (.createBNode ts "_:a")
          b (.createBNode ts "_:b")
          c (.createBNode ts "_:c")]
      (doto ts
        (.registerNamespace "foaf" "http://xmlns.com/foaf/0.1/")

        (.addStatement a "!foaf:name" (.createLiteral ts "Johnny Lee Outlaw"))
        (.addStatement a "!foaf:mbox" "<mailto:jlow@example.com>")
        (.addStatement b "!foaf:name" (.createLiteral ts "Peter Goodguy"))
        (.addStatement b "!foaf:mbox" "<mailto:peter@example.org>")
        (.addStatement c "!foaf:mbox" "<mailto:carol@example.org>")
        )
      (printlns (select (sparql-query ts
                                      (str "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
                                           " SELECT ?name ?mbox"
                                           " WHERE"
                                           "  { ?x foaf:name ?name ."
                                           "    ?x foaf:mbox ?mbox }"))))
      )))

(defn example-kennedy-select
  [query]
  (scope-let [server (ag-server *connection-params*)
              ts (agraph-renew! server (conj *connection-params* {:repository "kennedy"}))]
    (.loadNTriples ts (str *agraph-java-tutorial-dir* "/kennedy.ntriples"))
    (.indexAllTriples ts true)
    (let [query (cond (= 1 query) (str "PREFIX ex: <http://example.org/kennedy/> "
                                       "PREFIX fti: <http://franz.com/ns/allegrograph/2.2/textindex/> "
                                       "SELECT ?fname ?lname WHERE { ?person fti:match 'Arnold' . ?person ex:first-name ?fname . ?person ex:last-name ?lname }")
                      (= 2 query) (str "PREFIX ex: <http://example.org/kennedy/> "
                                       "PREFIX fti: <http://franz.com/ns/allegrograph/2.2/textindex/> "
                                       "SELECT ?person1 ?person2 WHERE { ?person1 fti:match 'John' . ?person2 ex:has-parent ?person1 .}")
                      :else query)
          sq (sparql-query ts query)]
      (select sq))))
