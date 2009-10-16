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
           [tutorial TutorialExamples]
           [org.openrdf.model ValueFactory Resource Literal]
           [org.openrdf.model.vocabulary RDF XMLSchema]
           [org.openrdf.query QueryLanguage]
           [org.openrdf.query.impl DatasetImpl]
           [org.openrdf.rio RDFFormat RDFHandler]
           [org.openrdf.rio.ntriples NTriplesWriter]
           [org.openrdf.rio.rdfxml RDFXMLWriter]
           [org.openrdf.sail.memory MemoryStore]
           [org.openrdf.repository.sail SailRepository]
           )
  (:use [clojure.contrib def test-is]
        [com.franz util openrdf agraph]
        [com.franz.agraph tutorial]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defn agraph-tests
  "Runs tests in this namespace with *test-out* bound so it works properly in slime."
  []
  (binding [*test-out* *out*] (run-tests 'com.franz.agraph.agtest)))

(declare server cat repo rcon vf)

(defn with-agraph-server
  [f]
  (with-agraph [server1 *connection-params*
                cat1 *catalog-id*
                repo1 {:name *catalog-id* :access :renew}
                rcon1]
    (binding [server server1
              cat cat1
              repo repo1
              rcon rcon1
              vf (.getValueFactory repo1)]
      (f))))

(use-fixtures :once with-agraph-server)

;; to make sure any other opened things get closed with each test
(use-fixtures :each (fn with-open2f [f]
                      (with-open2 [] (f))))

(defn run-test
  "run a single test function
  Example: (run-test catalog-scratch-repos)"
  [f]
  (binding [*test-out* *out*]
    (with-agraph-server f)))

(defn read-lines
  [f]
  (line-seq (open (BufferedReader. (open (FileReader. f))))))

(defn test-not-each
  "Intended to be used only from is-each.
  Returns nil if pred is true for every pair in col1 and col2, else return a nice message for is."
  [pred col1 col2 each-name msg]
  (loop [col1 col1
         col2 col2
         i 0]
    (let [a (first col1)
          b (first col2)]
      (cond (and (nil? col1) (nil? col2)) nil
            (pred a b) (recur (next col1) (next col2) (inc i))
            :else [(str each-name "#" i " ") (list a b) msg]))))

(defmacro is-each
  "Test pred on interleaved pairs from col1 and col1, Fail on the first that is not pred."
  ;; because reporting the entire set of pairs is to much info
  [pred col1 col2 each-name msg]
  `(is (not (test-not-each ~pred ~col1 ~col2 ~each-name ~msg))))

;;;; tests

(deftest catalog-scratch
  (is (some #{"scratch"} (map name (catalogs server)))))

(deftest catalog-scratch-repos
  (is nil? (repositories cat)))

(deftest catalog-scratch-repo-clear
  (clear! rcon)
  (is (= 0 (repo-size rcon))))

(deftest tutorial-output-clj
  ;;; Captures output from tutorial and compares to saved previous
  ;;; output. If changes to output are good, copy the tmp file to
  ;;; test/tutorial.clj.out, and commit.
  (clear! rcon)
  (let [outf (File/createTempFile "agraph-tutorial.clj-" ".out")
        server1 server
        cat1 cat
        repo1 repo
        rcon1 rcon]
    (println "Writing tutorial output to: " outf)
    (with-open2 []
      (binding [*out* (open (FileWriter. outf))
                ag-server (fn [url u p] server1)
                open-catalog (fn [c ct] cat1)
                repository (fn [cat name access] repo1)
                repo-init (fn [r] r)
                ;repo-connection (fn [r args] rcon1)
                ]
        (test1)
        (test2)
        (test3)
        (test4)
        (test5)
        (println "tutorial/test6 and higher fail")
        ;;(test-all)
        ))
    (let [prevf (File. "./clojure/test/com/franz/agraph/tutorial.clj.out")]
      (is-each = (read-lines prevf) (read-lines outf)
               "line" (str (.getCanonicalPath prevf) " differs from "
                           (.getCanonicalPath outf))))))

(deftest tutorial-output-java
  ;;; Captures output from tutorial and compares to saved previous
  ;;; output. If changes to output are good, copy the tmp file to
  ;;; test/tutorial.clj.out, and commit.
  (clear! rcon)
  (let [outf (File/createTempFile "agraph-tutorial.java-" ".out")]
    (println "Writing tutorial output to: " outf)
    (with-open2 []
      (let [out (PrintStream. outf)]
        (System/setOut out)
        (System/setErr out)
        ;(TutorialExamples/main (into-array String (map str (concat (range 1 16) [19]))))
        (TutorialExamples/main (into-array String (map str (concat (range 1 5) (range 6 16) [19]))))
        ;(TutorialExamples/main (into-array String (map str (concat [5]))))
        ))
    (let [prevf (File. "./clojure/test/com/franz/agraph/tutorial.java.out")]
      (is-each = (read-lines prevf) (read-lines outf)
               "line" (str (.getCanonicalPath prevf) " differs from "
                           (.getCanonicalPath outf))))))

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
              vf (.getValueFactory mem)]
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
                              vf (.getValueFactory mem)]
                      (tutorial-test5))]
    (is-each = ag-results mem-results "row" nil)))
