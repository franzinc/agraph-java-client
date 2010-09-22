;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;; In the clojure.test framework, "is" is where assertions are made.
  
  ;; Usage, in the REPL:
  (require 'com.franz.agraph.agtest)
  (in-ns 'com.franz.agraph.agtest)
  (agraph-tests)

  ;; Run from shell: 'ant test' or 'lein test'
  )

(ns com.franz.agraph.agtest
  "Tests for com.franz.agraph"
  (:refer-clojure :exclude (name with-open))
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
           [org.openrdf.repository.sail SailRepository])
  (:use [clojure test]
        [com.franz util openrdf agraph test])
  (:require [clojure.string :as str]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defn agraph-tests
  "Runs tests in this namespace with *test-out* bound so it works properly in slime."
  []
  (binding [*test-out* *out*] (run-tests 'com.franz.agraph.agtest)))

(declare server cat repo rcon vf)

(defn lookup-server-config
  []
  (letfn [(unless-blank [s] (when-not (str/blank? s) s))]
    (let [host (or (unless-blank (System/getenv "AGRAPH_HOST"))
                   (unless-blank (System/getProperty "AGRAPH_HOST"))
                   "localhost")]
      {:host host
       :port (if (= host "localhost")
               (let [pf (File. "../../agraph/lisp/agraph.port")]
                 (if (.exists pf)
                   (do (println "Reading agraph.port: " pf)
                       (with-open2 [] (Integer/parseInt (first (read-lines pf)))))
                   10035))
               (or (unless-blank (System/getenv "AGRAPH_PORT"))
                   (unless-blank (System/getProperty "AGRAPH_PORT"))))
       :username (or (System/getenv "AGRAPH_USER") "test")
       :password (or (System/getenv "AGRAPH_PASSWORD") "xyzzy")})))

(def *conn-params* (lookup-server-config))

(defn with-agraph-test
  [f]
  (with-agraph [server1 *conn-params*
                cat1 "java-tutorial"
                repo1 {:name "cljtest" :access :renew}
                rcon1]
    (repo-size rcon1) ;; ensures the connection is really open
    (binding [server server1
              cat cat1
              repo repo1
              rcon rcon1
              vf (value-factory rcon1)]
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
  (is (some #{"java-tutorial"} (catalogs server))))

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
  (let [mem (repo-init (open (SailRepository. (MemoryStore.))))
        mcon (repo-connection mem)]
    (binding [server nil
              cat nil
              repo mem
              rcon mcon
              vf (value-factory mcon)]
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

(comment "broken: different representation of float, need to fix the comparison"
(deftest compare-mem-test5
  ;; compare Sesame memory store to AGraph
  ;; test they get the same results for tutorial-test5
  (let [ag-results (tutorial-test5)
        mem (repo-init (open (SailRepository. (MemoryStore.))))
        mcon (repo-connection mem)
        mem-results (binding [server nil
                              cat nil
                              repo mem
                              rcon mcon
                              vf (value-factory mcon)]
                      (tutorial-test5))]
    ;;(is-each = ag-results mem-results "row" nil)))
    (is= ag-results mem-results)))
)

(deftest illegal-sparql
  (is (thrown? org.openrdf.query.QueryEvaluationException
               ;; xsd prefix declaration is missing
               (tuple-query rcon QueryLanguage/SPARQL
                            (str "SELECT ?s ?p ?o  "
                                 "WHERE { ?s ?p ?o . "
                                 "FILTER (xsd:int(?o) >= 30) }")
                            nil))))

(deftest test6-baseuri
  ;; testing bug: org.openrdf.rio.RDFParseException: URI "<http://example.org/example/local>" contains illegal character #\< at position 0.
  (clear! rcon)
  (let [vcards (File. "../src/tutorial/java-vcards.rdf")
        baseURI "http://example.org/example/local"
        context (-> rcon value-factory (uri "http://example.org#vcards"))]
    (add-from! rcon vcards baseURI RDFFormat/RDFXML context)
    (is (= 16 (repo-size rcon context)))))

(deftest test16-federation
  (close rcon)
  (let [ex "http://www.demo.com/example#"
        rcon-args {:namespaces {"ex" ex}}
        ;; create two ordinary stores, and one federated store: 
        red-con (ag-repo-con cat "redthings" rcon-args)
        green-con (ag-repo-con cat "greenthings" rcon-args)
        rainbow-con (repo-federation server [red-con green-con] rcon-args)
        rf (value-factory red-con)
        gf (value-factory green-con)
        rbf (value-factory rainbow-con)]
    (clear! red-con)
    (clear! green-con)
    ;; add a few triples to the red and green stores:
    (doseq [[c f s o]
            [[red-con rf "mcintosh" "Apple"]
             [red-con rf "reddelicious" "Apple"]
             [green-con gf "pippen" "Apple"]
             [green-con gf "kermitthefrog" "Frog"]]]
      (add! c (uri f (str ex s)) RDF/TYPE (uri rf (str ex o))))
    ;; query each of the stores; observe that the federated one is the union of the other two:
    (doseq [[kind rcon size] [["red" red-con 2]
                              ["green" green-con 1]
                              ["federated" rainbow-con 3]]]
      (is (= size (count (tuple-query rcon QueryLanguage/SPARQL
                                      "select ?s where { ?s rdf:type ex:Apple }"
                                      nil)))))))
