;; This software is Copyright (c) Franz, 2009.
;; Franz grants you the rights to distribute
;; and use this software as governed by the terms
;; of the Lisp Lesser GNU Public License
;; (http://opensource.franz.com/preamble.html),
;; known as the LLGPL.

(comment
  ;; In the clojure test-is framework, IS is where assertions are made.

  ;; Usage, in the REPL:
  (require 'com.franz.agraph.agtest)
  (in-ns 'com.franz.agraph.agtest)
  (agraph-tests)

  ;; Run from shell: agtests.sh
  )

(ns com.franz.agraph.agtest
  "Tests for com.franz.agraph"
  (:refer-clojure :exclude (name))
  (:import [java.io File OutputStream FileOutputStream FileWriter
            BufferedReader FileReader PrintStream]
           [com.franz.agraph.repository
            AGCatalog AGQueryLanguage AGRepository
            AGRepositoryConnection AGServer AGValueFactory]
           [org.openrdf.model ValueFactory Resource Literal]
           [org.openrdf.model.vocabulary RDF XMLSchema]
           [org.openrdf.query QueryLanguage]
           [org.openrdf.query.impl DatasetImpl]
           [org.openrdf.rio RDFFormat RDFHandler]
           [org.openrdf.rio.ntriples NTriplesWriter]
           [org.openrdf.rio.rdfxml RDFXMLWriter]
           [org.openrdf.sail.memory MemoryStore]
           [org.openrdf.repository RepositoryConnection]
           [org.openrdf.repository.sail SailRepository]
           )
  (:use [clojure.contrib def test-is]
        [com.franz util openrdf agraph test]
        [com.franz.agraph tutorial]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defn agraph-tests
  "Runs tests in this namespace with *test-out* bound so it works properly in slime."
  []
  (binding [*test-out* *out*] (run-tests 'com.franz.agraph.agtest)))

(declare server cat repo rcon vf)

(defn with-agraph-test
  [f]
  (with-agraph [server1 *connection-params*
                cat1 *catalog-id*
                repo1 {:name *catalog-id* :access :renew}
                rcon1]
    (repo-size rcon1) ;; ensures the connection is really open
    (binding [server server1
              cat cat1
              repo repo1
              rcon rcon1
              vf (value-factory repo1)]
      (f))))

(use-fixtures :once with-agraph-test)

;; to make sure any other opened things get closed with each test
(use-fixtures :each with-open2f)

(defn run-test
  "run a single test function
  Example: (run-test catalog-scratch-repos)"
  [f]
  (binding [*test-out* *out*]
    (with-agraph-test f)))

;;;; tests

(deftest catalog-scratch
  (is (some #{"scratch"} (map name (catalogs server)))))

(deftest catalog-scratch-repos
  (is nil? (repositories cat)))

(deftest catalog-scratch-repo-clear
  (clear! rcon)
  (is (= 0 (repo-size rcon))))

(deftest tutorial-test2-3
  (clear! rcon)
  (let [f vf
        ;; create some resources and literals to make statements out of
        alice (uri f "http://example.org/people/alice")
        bob (uri f "http://example.org/people/bob")
        name (uri f "http://example.org/ontology/name")
        person (uri f "http://example.org/ontology/Person")
        bobsName (literal f "Bob")
        alicesName (literal f "Alice")
        type RDF/TYPE
        statements (set (map to-statement [[alice type person]
                                           [alice name alicesName]
                                           [bob type person]
                                           [bob name bobsName]]))]
    (is (= 0 (repo-size rcon)))
    
    (add! rcon alice type person)
    (add! rcon alice name alicesName)
    (add! rcon bob type person)
    (add! rcon bob (uri f "http://example.org/ontology/name") bobsName)
    (is (= 4 (repo-size rcon)))
    (is (= statements
           (set (get-statements rcon nil nil nil false nil))))
    
    (remove! rcon bob name bobsName)
    (is (= 3 (repo-size rcon)))
    
    (add! rcon bob name bobsName)
    (is (= 4 (repo-size rcon)))
    
    (is (= statements
           (set (tuple-query rcon QueryLanguage/SPARQL "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}" nil))))
    ))

(deftest compare-mem-agraph
  ;; compare Sesame memory store to AGraph
  ;; test that Sesame passes tutorial-test2-3
  (let [mem (repo-init (open (SailRepository. (MemoryStore.))))]
    (binding [server nil
              cat nil
              repo mem
              rcon (repo-connection mem)
              vf (value-factory mem)]
      (tutorial-test2-3))))

(defn tutorial-test5
  "return the same results as test5, in a data structure"
  []
  (clear! rcon)
  (let [f vf
        exns "http://example.org/people/"
        alice (uri f "http://example.org/people/alice")
        age (uri f exns "age")
        weight (uri f exns, "weight")
        favoriteColor (uri f exns "favoriteColor")
        birthdate (uri f exns "birthdate")
        ted (uri f exns "Ted")
        red (literal f "Red")
        rouge (literal f "Rouge" "fr")
        fortyTwo (literal f "42" XMLSchema/INT)
        fortyTwoInteger (literal f"42", XMLSchema/LONG)
        fortyTwoUntyped (literal f "42")
        date (literal f "1984-12-06" XMLSchema/DATE)
        time (literal f "1984-12-06T09:00:00" XMLSchema/DATETIME)
        stmt1 (.createStatement f alice age fortyTwo)
        stmt2 (.createStatement f ted age fortyTwoUntyped)]
    (add-all! rcon
              [stmt1
               stmt2
               [alice weight (literal f "20.5")]
               [ted weight (literal f "20.5" XMLSchema/FLOAT)]
               [alice favoriteColor red]
               [ted favoriteColor rouge]
               [alice birthdate date]
               [ted birthdate time]])
    (doall (map (fn [x] [x (get-statements rcon nil nil x nil)])
                [nil fortyTwo fortyTwoUntyped (literal f "20.5" XMLSchema/FLOAT)
                 (literal f "20.5") red rouge]))
    (doall (map (fn [x] [x (tuple-query rcon QueryLanguage/SPARQL
                                        (str "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                                             "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = " x ")}") nil)])
                ["42", "\"42\"", "20.5", "\"20.5\"", "\"20.5\"^^xsd:float"
                 "\"Rouge\"@fr", "\"Rouge\"", "\"1984-12-06\"^^xsd:date"]))))

(deftest compare-mem-test5
  ;; compare Sesame memory store to AGraph
  ;; test they get the same results for tutorial-test5
  (let [ag-results (tutorial-test5)
        mem (repo-init (open (SailRepository. (MemoryStore.))))
        mem-results (binding [server nil
                              cat nil
                              repo mem
                              rcon (repo-connection mem)
                              vf (value-factory mem)]
                      (tutorial-test5))]
    (is-each = ag-results mem-results "row" nil)))
