;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;; Tutorial code for using Franz AllegroGraph with Clojure.
  
  ;; Usage:

  ;; In the source distribution of agraph-java-client,
  ;; in the clojure directory...
  ;;
  ;; Next, download all dependencies:
  ;; > lein deps
  ;;
  ;; Start the repl:
  ;; > lein repl
  ;;
  ;; Or, start the repl for Emacs/Slime:
  ;; > lein swank
  ;;

  ;; 
  ;; Or use .project and .classpath with Eclipse/CounterClockWise
  ;; See http://code.google.com/p/counterclockwise/
  
  ;; Load this file:
  (require 'com.franz.agraph.tutorial)

  ;; Change to this namespace:
  (in-ns 'com.franz.agraph.tutorial)
  
  ;; You may want to set your own connection params:
  (def *connection-params* {:host "localhost" :port 8080
                            :username "test" :password "xyzzy"
                            :catalog "java-catalog"
                            :repository "cljtutorial"})

  ;; Optional, for convenience in the REPL:
  (use '[clojure stacktrace])
  (use '[clojure.contrib repl-utils trace])
  
  ;; Execute the examples: test1-test16
  (test1)
  (test-all)
  )

(ns com.franz.agraph.tutorial
  "Tutorial code for using Franz AllegroGraph from the Clojure language.
  Follows the Python and Java tutorial code."
  (:refer-clojure :exclude (name))
  (:import [com.franz.agraph.repository
            AGCatalog AGQueryLanguage AGRepository
            AGRepositoryConnection AGServer AGValueFactory]
           [org.openrdf.model ValueFactory Resource Literal]
           [org.openrdf.model.vocabulary RDF XMLSchema]
           [org.openrdf.query QueryLanguage]
           [org.openrdf.query.impl DatasetImpl]
           [org.openrdf.rio RDFFormat RDFHandler]
           [org.openrdf.rio.ntriples NTriplesWriter]
           [org.openrdf.rio.rdfxml RDFXMLWriter]
           [java.io File OutputStream FileOutputStream])
  (:use [com.franz util openrdf agraph]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

;; Clojure equivalents of the Python and Java tests are below.

;; Note, you can also try the Java tutorial in the Clojure REPL.
;; However, in Slime/swank-clojure, the System.out prints to the
;; *inferior-lisp* buffer not the Slime REPL, so it may be easier to
;; use that instead of slime.
;; (import 'tutorial.TutorialExamples)
;; (TutorialExamples/test1)

(defonce *connection-params* {:host "localhost" :port 10035
                              :username "test" :password "xyzzy"
                              :catalog "java-catalog"
                              :repository "javatutorial"})

(def *agraph-java-tutorial-dir* (or (System/getProperty "com.franz.agraph.tutorial.dir")
                                    (.getCanonicalPath (java.io.File. "../src/tutorial/"))))

(defn test1
  "lists catalogs and more info about the scratch catalog."
  []
  (with-agraph [server *connection-params*]
    (println "Available catalogs:" (catalogs server))
    (let [catalog (open-catalog server (:catalog *connection-params*))]
      (println "Available repositories in catalog" (name catalog) ":"
               (repositories catalog))
      (let [myRepository (repo-init (repository catalog (:repository *connection-params*) :renew))]
        (let [rcon (repo-connection myRepository)]
          (println "Repository" (name myRepository) "is up! It contains"
                   (repo-size rcon) "statements."))))))

(defn test2
  "demonstrates adding and removing triples."
  []
  (with-agraph [con *connection-params*
                cat (:catalog *connection-params*)
                repo {:name (:repository *connection-params*) :access :renew}
                rcon]
    (let [f (.getValueFactory repo)
          ;; create some resources and literals to make statements out of
          alice (uri f "http://example.org/people/alice")
          bob (uri f "http://example.org/people/bob")
          name (uri f "http://example.org/ontology/name")
          person (uri f "http://example.org/ontology/Person")
          bobsName (literal f "Bob")
          alicesName (literal f "Alice")]
      (println "Triple count before inserts:" (repo-size rcon))
      (printlns (get-statements rcon nil nil nil nil))
      
      (add! rcon alice RDF/TYPE person)
      (add! rcon alice name alicesName)
      (add! rcon bob RDF/TYPE person)
      (add! rcon bob (uri f "http://example.org/ontology/name") bobsName)
      (println "Triple count after adding:" (repo-size rcon))
      
      (remove! rcon bob name bobsName)
      (println "Triple count after removing:" (repo-size rcon))
      
      ;; add it back
      (add! rcon bob name bobsName)
      repo)))

(defn test3
  "demonstrates a SPARQL query using the data from test2"
  []
  (with-open2 [rcon (repo-connection (test2))]
    (printlns (tuple-query rcon QueryLanguage/SPARQL "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}"
                           nil))))

(defn test4
  ""
  []
  (let [repo (test2)]
    (with-open2 [rcon (repo-connection repo)]
      (let [alice (uri (.getValueFactory repo) "http://example.org/people/alice")]
        (printlns (get-statements rcon alice nil nil nil))))))

(defn test5
  "Typed Literals"
  []
  (with-agraph [con *connection-params*
                cat (:catalog *connection-params*)
                repo {:name (:repository *connection-params*) :access :renew}
                rcon]
    (clear! rcon)
    (let [f (.getValueFactory rcon)
          exns "http://example.org/people/"
          alice (uri f "http://example.org/people/alice")
          age (uri f exns "age")
          weight (uri f exns, "weight")
          favoriteColor (uri f exns "favoriteColor")
          birthdate (uri f exns "birthdate")
          ted (uri f exns "Ted")
          red (literal f "Red")
          rouge (literal f "Rouge" "fr")
          fortyTwoInt (literal f "42" XMLSchema/INT)
          fortyTwoLong (literal f "42" XMLSchema/LONG)
          fortyTwoUntyped (literal f "42")
          date (literal f "1984-12-06" XMLSchema/DATE)
          time (literal f "1984-12-06T09:00:00" XMLSchema/DATETIME)
          stmt1 (.createStatement f alice age fortyTwoInt)
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
      
      (doseq [obj [nil fortyTwoInt fortyTwoLong fortyTwoUntyped
                   (literal f "20.5" XMLSchema/FLOAT)
                   (literal f "20.5") red rouge]]
        (println "Retrieve triples matching" obj ".")
        (printlns (get-statements rcon nil nil obj nil)))
      
      (doseq [obj ["42", "\"42\"", "20.5", "\"20.5\"", "\"20.5\"^^xsd:float"
                   "\"Rouge\"@fr", "\"Rouge\"", "\"1984-12-06\"^^xsd:date"]]
        (println "Query triples matching" obj ".")
        (printlns (tuple-query rcon QueryLanguage/SPARQL
                               (str "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = " obj ")}") nil)))
      )))

(defn test6
  []
  (with-agraph [con *connection-params*
                cat (:catalog *connection-params*)
                repo {:name (:repository *connection-params*) :access :renew}
                rcon {:namespaces {"vcd" "http://www.w3.org/2001/vcard-rdf/3.0#"}}]
    (clear! rcon)
    (let [vcards (new File *agraph-java-tutorial-dir* "/java-vcards.rdf")
          kennedy (new File *agraph-java-tutorial-dir* "/java-kennedy.ntriples")
          baseURI "http://example.org/example/local"
          context (-> repo .getValueFactory (uri "http://example.org#vcards"))]
      (add-from! rcon vcards baseURI RDFFormat/RDFXML context)
      (add-from! rcon kennedy baseURI RDFFormat/NTRIPLES nil)
      (println "After loading, repository contains " (repo-size rcon context)
               " vcard triples in context '" context "'\n    and   "
               (repo-size rcon nil) " kennedy triples in context 'nil'.")
      repo)))

(defn test7
  []
  (with-open2 [rcon (repo-connection (test6))]
    (println "Match all and print subjects and contexts:")
    (printlns (get-statements rcon nil nil nil nil))
    
    (println "Same thing with SPARQL query:")
    (printlns (tuple-query rcon QueryLanguage/SPARQL
                           "SELECT DISTINCT ?s ?c WHERE {graph ?c {?s ?p ?o .} }" nil))))

(defn test8
  "Writing RDF or NTriples to a file"
  [& [write-to-file?]]
  (with-open2 [rcon (repo-connection (test6))]
    (let [contexts (resource-array [(uri (.getValueFactory rcon) "http://example.org#vcards")])]
      (let [output (if write-to-file? (new FileOutputStream "/tmp/temp.nt") *out*)
            writer (new NTriplesWriter output)]
        (println "Writing NTriples to" output)
        (.export rcon writer contexts))
      
      (let [output (if write-to-file? (new FileOutputStream "/tmp/temp.rdf") *out*)
            writer (new RDFXMLWriter output)]
        (println "Writing RDFXML to" output)
        (.export rcon writer contexts)))))

(defn test9
  "Writing the result of a statements match to a file."
  []
  (with-open2 [rcon (repo-connection (test6))]
    (.exportStatements rcon nil RDF/TYPE nil false (new RDFXMLWriter *out*) (resource-array nil))))

(defn test10
  "Datasets and multiple contexts"
  []
  (with-agraph [con *connection-params*
                cat (:catalog *connection-params*)
                repo {:name (:repository *connection-params*) :access :renew}
                rcon]
    (let [f (.getValueFactory repo)
          exns "http://example.org/people/"
          alice (uri f exns "alice")
          bob (uri f exns "bob")
          ted (uri f exns "ted")
          name (uri f "http://example.org/ontology/name")
          person (uri f "http://example.org/ontology/Person")
          alicesName (literal f "Alice")
          bobsName (literal f "Bob")
          tedsName (literal f "Ted")
          context1 (uri f exns "cxt1")
          context2 (uri f exns "cxt2")]
      (add-all! rcon [[alice RDF/TYPE person context1]
                      [alice name alicesName context1]
                      [bob RDF/TYPE person context2]
                      [bob name bobsName context2]
                      [ted RDF/TYPE person]
                      [ted name tedsName]])
      (println "All triples in all contexts:")
      (printlns (get-statements rcon nil nil nil nil))
      (println "Triples in contexts 1 or 2:")
      (printlns (get-statements rcon nil nil nil nil context1 context2))
      (println "Triples in contexts nil or 2:")
      (printlns (get-statements rcon nil nil nil nil nil context2))
	    
      (println "Query over contexts 1 and 2:")
      (printlns (tuple-query rcon QueryLanguage/SPARQL
                             "SELECT ?s ?p ?o ?c WHERE { GRAPH ?c {?s ?p ?o . } }"
                             {:dataset (doto (new DatasetImpl)
                                         (.addNamedGraph context1)
                                         (.addNamedGraph context1))}))
      
      (println "Query over the null context:")
      (printlns (tuple-query rcon QueryLanguage/SPARQL
                             "SELECT ?s ?p ?o WHERE {?s ?p ?o . }"
                             {:dataset (doto (new DatasetImpl)
                                         (.addDefaultGraph nil))})))))

(defn test11
  "Namespaces"
  []
  (with-agraph [con *connection-params*
                cat (:catalog *connection-params*)
                repo {:name (:repository *connection-params*) :access :renew}
                rcon]
    (let [f (.getValueFactory repo)
          exns "http://example.org/people/"
          alice (uri f exns "alice")
          person (uri f exns "Person")]
      (add! rcon alice RDF/TYPE person)
      (.setNamespace rcon "ex" exns)
      ;; conn.removeNamespace("ex")
      (printlns (tuple-query rcon QueryLanguage/SPARQL
                             "SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER ((?p = rdf:type) && (?o = ex:Person) ) }" nil)))))

(defn test12
  "Text search"
  []
  (with-agraph [con *connection-params*
                cat (:catalog *connection-params*)
                repo {:name (:repository *connection-params*) :access :renew}
                rcon]
    (let [f (.getValueFactory repo)
          exns "http://example.org/people/"]
      (.createFreetextIndex repo "index1" (to-array (list (str exns "fullname"))))
      (let [alice (uri f exns "alice1")
            persontype (uri f exns "Person")
            fullname (uri f exns "fullname")
            alicename (literal f "Alice B. Toklas")
            book (uri f exns "book1")
            booktype (uri f exns "Book")
            booktitle (uri f exns "title")
            wonderland (literal f "Alice in Wonderland")]
        (clear! rcon (resource-array nil))
        (add-all! rcon [[alice RDF/TYPE persontype]
                        [alice fullname alicename]
                        [book RDF/TYPE booktype]
                        [book booktitle wonderland]])
        ;; .indexTriples repo true)
	    (.setNamespace rcon "ex" exns)
	    ;; (.setNamespace rcon 'fti', "http://franz.com/ns/allegrograph/2.2/textindex/") ; is already built-in
        
        (let [queryString "SELECT ?s ?p ?o WHERE { ?s ?p ?o . ?s fti:match 'Alice' . }"]
                                        ; :query "SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER regex(?o, "Ali") }"
          (printlns (take 5 (tuple-query rcon QueryLanguage/SPARQL queryString nil))))))))

(defn test13
  "Ask, Construct, and Describe queries"
  []
  (with-open2 [rcon (repo-connection (test2)
                                     {:namespaces {"ex" "http://example.org/people/"
                                                   "ont" "http://example.org/ontology/"}})]
    (printlns (tuple-query rcon QueryLanguage/SPARQL
                           "select ?s ?p ?o where { ?s ?p ?o}" nil))
    
    (println "Boolean result:"
             (query-boolean rcon QueryLanguage/SPARQL
                            "ask { ?s ont:name \"Alice\" }" nil))
    
    (print "Construct result: ")
    (doall (map #(print % " ")
                (query-graph rcon QueryLanguage/SPARQL
                             "construct {?s ?p ?o} where { ?s ?p ?o . filter (?o = \"Alice\") } " nil)))
    (println)
    
    (println "Describe result: ")
    (printlns (query-graph rcon QueryLanguage/SPARQL
                           "describe ?s where { ?s ?p ?o . filter (?o = \"Alice\") }" nil))))

(defn test14
  "Parametric Queries"
  []
  (let [repo (test2)]
    (with-open2 [rcon (repo-connection repo)]
      (let [f (.getValueFactory repo)
            alice (uri f "http://example.org/people/alice1")
            bob (uri f "http://example.org/people/bob")]
        (println "Facts about Alice:")
        (printlns (tuple-query rcon QueryLanguage/SPARQL
                               "select ?s ?p ?o where { ?s ?p ?o}"
                               {:binding ["s" alice]}))
        (println "Facts about Bob:")
        (printlns (tuple-query rcon QueryLanguage/SPARQL
                               "select ?s ?p ?o where { ?s ?p ?o}"
                               {:binding ["s" bob]}))))))

(defn test16
  "Federated triple stores."
  []
  (with-agraph [server *connection-params*
                cat (:catalog *connection-params*)]
    (let [ex "http://www.demo.com/example#"
          rcon-args {:namespaces {"ex" ex}}]
      ;; create two ordinary stores, and one federated store: 
      (let [red-rep (repo-init (repository cat "redthings" :renew))
            red-con (repo-connection red-rep rcon-args)
            green-rep (repo-init (repository cat "greenthings" :renew))
            green-con (repo-connection green-rep rcon-args)
            rainbow-rep (repo-init (repo-federation server
                                                    red-rep green-rep))
            rainbow-con (repo-connection rainbow-rep rcon-args)
            rf (value-factory red-con)
            gf (value-factory green-con)
            rbf (value-factory rainbow-con)]
        (clear! red-con)
        (clear! green-con)
        ;; add a few triples to the red and green stores:
        (doseq [[c f s o] [[red-con rf "mcintosh" "Apple"]
                           [red-con rf "reddelicious" "Apple"]
                           [green-con gf "pippen" "Apple"]
                           [green-con gf "kermitthefrog" "Frog"]
                           ]]
          (add! c (uri f (str ex s)) RDF/TYPE (uri rf (str ex o))))
        ;; query each of the stores; observe that the federated one is the union of the other two:
        (doseq [[kind rcon] [["red" red-con]
                             ["green" green-con]
                             ["federated" rainbow-con]]]
          (println)
          (println kind "apples:")
          (printlns (tuple-query rcon QueryLanguage/SPARQL
                                 "select ?s where { ?s rdf:type ex:Apple }"
                                 nil)))))))

;; (defn test17
;;   "Prolog queries"
;;   []
;;   (with-open2 [rcon (repo-connection (test6))]
;;     (.deleteEnvironment rcon "kennedys") ;; start fresh
;;     (.setEnvironment rcon "kennedys")
;;     (.setNamespace rcon "kdy" "http://www.franz.com/simple#")
;;     (.setRuleLanguage rcon lang-prolog)
;;     (.addRules rcon "
;;        (<-- (female ?x) ;; IF
;;           (q ?x !kdy:sex !kdy:female))
;;        (<-- (male ?x) ;; IF
;;           (q ?x !kdy:sex !kdy:male))
;;        ")
;;     ;; This causes a failure(correctly):
;;     ;; conn.deleteRule('male')
;;     (with-results [row (prepare-query rcon {:type :tuple :lang lang-prolog} "
;;       (select (?person ?name)
;;               (q ?person !rdf:type !kdy:person)
;;               (male ?person)
;;               (q ?person !kdy:first-name ?name)
;;               )
;;       ")]
;;       (println row))))

(defn test26
  "Queries per second."
  []
  (with-open2 [rcon (repo-connection (test6))]
    (let [reps 10
          vf (.getValueFactory rcon)
          ;;TEMPORARY
          context (uri vf "http://example.org#vcards")
          ;; END TEMPORARY
        
          contexts (resource-array [context])
          ajax (literal vf "AFC Ajax")]
      ;; (let [begin (System/currentTimeMillis)
      ;;       count (last (map (fn [_] (count (cursor-all (statements-cursor rcon nil nil ajax false context nil))))
      ;;                        (repeat reps nil)))
      ;;       end (System/currentTimeMillis)]
      ;;   (printf "Did %s %s-row matches in %s.%s seconds."
      ;;           reps count (int (/ (- end begin) 1000)) (mod (- end begin) 1000))
      ;;   (println))
      
      (let [begin (System/currentTimeMillis)
            count (last (map (fn [_] (with-open2 []
                                       (count (get-statements rcon nil nil nil false nil))))
                             (repeat reps nil)))
            end (System/currentTimeMillis)]
        (printf "Did %s %s-row matches in %s.%s seconds."
                reps count (int (/ (- end begin) 1000)) (mod (- end begin) 1000))
        (println))

      (doseq [size [1 5 10 100]]
        (let [queryString (str "select ?x ?y ?z where {?x ?y ?z} limit " size)]
          (let [begin (System/currentTimeMillis)
                count (last (map (fn [_] (with-open2 []
                                           (count (tuple-query rcon QueryLanguage/SPARQL queryString nil))))
                                 (repeat reps nil)))
                end (System/currentTimeMillis)]
            (printf "Did %s %s-row matches in %s.%s seconds."
                    reps count (int (/ (- end begin) 1000)) (mod (- end begin) 1000))
            (println)))))))

(defn test-all
  []
  (test1)
  (test2)
  (test3)
  (test4)
  (test5)
  (test6)
  (test7)
  (test8)
  (test9)
  (test10)
  (test11)
  (test12)
  (test13)
  (test14)
  (test16)
  (test26)
  )
