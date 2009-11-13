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

(declare server cat repo vf)

(defn with-agraph-test
  [f]
  (scope-let [server1 (ag-server *connection-params*)
              cat1 (ag-catalog server1 (:catalog *connection-params*))
              repo1 (ag-repo-con cat1 (:repository *connection-params*))
              vf1 (value-factory repo1)]
    (repo-size repo1) ;; ensures the connection is really open
    (binding [server server1
              cat cat1
              repo repo1
              vf vf1]
      (f))))

(use-fixtures :once with-agraph-test)

;; to make sure any other opened things get closed with each test
;(use-fixtures :each with-open2f)

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
  (clear! repo)
  (is (= 0 (repo-size repo))))

(deftest tutorial-test2-3
  (clear! repo)
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
    (is (= 0 (repo-size repo)))
    
    (add! repo alice type person)
    (add! repo alice name alicesName)
    (add! repo bob type person)
    (add! repo bob (uri f "http://example.org/ontology/name") bobsName)
    (is (= 4 (repo-size repo)))
    (is-each = statements (set (get-statements repo [nil nil nil] nil))
             "stmt" "xxx")
  
    (remove! repo [bob name bobsName])
    (is (= 3 (repo-size repo)))
    
    (add! repo bob name bobsName)
    (is (= 4 (repo-size repo)))
    
    (is-each = statements
             (set (tuple-query repo QueryLanguage/SPARQL "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}" nil))
             "stmt" "xxx")))

(deftest compare-mem-agraph
  ;; compare Sesame memory store to AGraph
  ;; test that Sesame passes tutorial-test2-3
  (let [mem (repo-init (open (SailRepository. (MemoryStore.))))
        mcon (repo-connection mem)]
    (binding [server nil
              cat nil
              repo mcon
              vf (value-factory mcon)]
      (tutorial-test2-3))))

(defn tutorial-test5
  "return the same results as test5, in a data structure"
  []
  (clear! repo)
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
    (add-all! repo
              [stmt1
               stmt2
               [alice weight (literal f "20.5")]
               [ted weight (literal f "20.5" XMLSchema/FLOAT)]
               [alice favoriteColor red]
               [ted favoriteColor rouge]
               [alice birthdate date]
               [ted birthdate time]])
    (doall (map (fn [x] [x (get-statements repo [nil nil x] nil)])
                [nil fortyTwo fortyTwoUntyped (literal f "20.5" XMLSchema/FLOAT)
                 (literal f "20.5") red rouge]))
    (doall (map (fn [x] [x (tuple-query repo QueryLanguage/SPARQL
                                        (str "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                                             "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = " x ")}") nil)])
                ["42", "\"42\"", "20.5", "\"20.5\"", "\"20.5\"^^xsd:float"
                 "\"Rouge\"@fr", "\"Rouge\"", "\"1984-12-06\"^^xsd:date"]))))

(deftest compare-mem-test5
  ;; compare Sesame memory store to AGraph
  ;; test they get the same results for tutorial-test5
  (let [ag-results (tutorial-test5)
        mem (repo-init (open (SailRepository. (MemoryStore.))))
        mcon (repo-connection mem)
        mem-results (binding [server nil
                              cat nil
                              repo mcon
                              vf (value-factory mcon)]
                      (tutorial-test5))]
    (is-each = ag-results mem-results "row" nil)))

(deftest illegal-sparql
  (is (thrown? org.openrdf.query.QueryEvaluationException
               ;; xsd prefix declaration is missing
               (tuple-query repo QueryLanguage/SPARQL
                            (str "SELECT ?s ?p ?o  "
                                 "WHERE { ?s ?p ?o . "
                                 "FILTER (xsd:int(?o) >= 30) }")
                            nil))))

(deftest test6-baseuri
  ;; testing bug: org.openrdf.rio.RDFParseException: URI "<http://example.org/example/local>" contains illegal character #\< at position 0.
  (clear! repo)
  (let [vcards (new File *agraph-java-tutorial-dir* "/vc-db-1.rdf")
        baseURI "http://example.org/example/local"
        context (uri vf "http://example.org#vcards")]
    (add-from! repo vcards baseURI RDFFormat/RDFXML context)
    (is (= 16 (repo-size repo context)))))

(deftest test16-federation
  (close repo)
  (let [ex "http://www.demo.com/example#"
        repo-args {:namespaces {"ex" ex}}
        ;; create two ordinary stores, and one federated store: 
        red (ag-repo-con cat "redthings" repo-args)
        green (ag-repo-con cat "greenthings" repo-args)
        rainbow (repo-federation server "rainbowthings"
                                 [red green] repo-args)
        rf (value-factory red)
        gf (value-factory green)
        rbf (value-factory rainbow)]
    (clear! red)
    (clear! green)
    ;; add a few triples to the red and green stores:
    (doseq [[c f s o]
            [[red rf "mcintosh" "Apple"]
             [red rf "reddelicious" "Apple"]
             [green gf "pippen" "Apple"]
             [green gf "kermitthefrog" "Frog"]]]
      (add! c (uri f (str ex s)) RDF/TYPE (uri rf (str ex o))))
    ;; query each of the stores; observe that the federated one is the union of the other two:
    (doseq [[kind repo size] [["red" red 2]
                              ["green" green 1]
                              ["federated" rainbow 3]]]
      (is (= size (count (tuple-query repo QueryLanguage/SPARQL
                                      "select ?s where { ?s rdf:type ex:Apple }"
                                      nil)))))))

(deftest con-closing 
  (let [repository (repository repo)]
    (repo-size repo)
    (close repo)
    (let [repo (repo-connection repository)]
      (repo-size repo)
      (close repo)
      ;; close again? (close rcon)
      )))
