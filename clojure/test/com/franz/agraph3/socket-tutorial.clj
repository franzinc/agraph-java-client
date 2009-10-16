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

;; (add-classpath "file:///.../agraph-fje-3.2/agraph-3-2.jar")

(ns com.franz.agraph3.socket-tutorial
  (:use [com.franz.agraph3.socket])
  (:import [com.franz.agbase AllegroGraphConnection AllegroGraph
            SPARQLQuery]))

(defn sparql-select
  [conn ds-dir]
  (with-openm [ts (.renew conn "sparqlselect" ds-dir)]
    (doto ts
      (.registerNamespace "ex","http://example.org/")
      (.addStatement "!ex:a","!ex:p", "!ex:b")
      (.addStatement "!ex:b","!ex:p", "!ex:c"))
    
    (let [query "SELECT * {?s ?p ?o}"
          sq (sparql-query ts query)]
      (select sq))))

(defn sample-sparql-basic-graph-patterns 
  [conn ds-dir]
  (with-openm [ts (.renew conn "sparql" ds-dir)]
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

(defn sample-kennedy-select
  [conn ds-dir data-dir query]
  (with-openm [ts (.renew conn "kennedy" ds-dir)]
    (.loadNTriples ts (str data-dir "/kennedy.ntriples"))
    (.indexAllTriples ts true)
    (let [query (cond (= 1 query) "PREFIX ex: <http://example.org/kennedy/> PREFIX fti: <http://franz.com/ns/allegrograph/2.2/textindex/> SELECT ?fname ?lname WHERE { ?person fti:match 'Arnold' . ?person ex:first-name ?fname . ?person ex:last-name ?lname }"
                      (= 2 query) "PREFIX ex: <http://example.org/kennedy/> PREFIX fti: <http://franz.com/ns/allegrograph/2.2/textindex/> SELECT ?person1 ?person2 WHERE { ?person1 fti:match 'John' . ?person2 ex:has-parent ?person1 .}"
                      :else query)
          sq (sparql-query ts query)]
      (select sq))))
