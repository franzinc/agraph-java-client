;; This software is Copyright (c) Franz, 2009.
;; Franz grants you the rights to distribute
;; and use this software as governed by the terms
;; of the Lisp Lesser GNU Public License
;; (http://opensource.franz.com/preamble.html),
;; known as the LLGPL.

(ns com.franz.agraph.test-tutorial
  "Tests for com.franz.agraph.tutorial and TutorialExamples.java"
  (:refer-clojure :exclude (name))
  (:import [java.io File FileWriter PrintStream]
           [tutorial TutorialExamples])
  (:use [clojure.contrib def test-is]
        [com.franz util openrdf agraph test]
        [com.franz.agraph tutorial agtest]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

;; to make sure any other opened things get closed with each test
(use-fixtures :each with-open2f)

(deftest tutorial-output-clj
  ;;; Captures output from tutorial and compares to saved previous
  ;;; output. If changes to output are good, copy the tmp file to
  ;;; test/tutorial.clj.out, and commit.
  (with-agraph-test
    #(do (clear! rcon)
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
                                  (.getCanonicalPath outf))))))))

(deftest tutorial-output-java
  ;;; Captures output from tutorial and compares to saved previous
  ;;; output. If changes to output are good, copy the tmp file to
  ;;; test/tutorial.clj.out, and commit.
  ;;(clear! rcon)
  (let [outf (File/createTempFile "agraph-tutorial.java-" ".out")]
    (println "Writing tutorial output to: " outf)
    (with-open2 []
      (let [out (PrintStream. outf)]
        (System/setOut out)
        (System/setErr out)
        ;(TutorialExamples/main (into-array String (map str (concat (range 1 16) [19]))))
        (TutorialExamples/main (into-array String (map str (concat (range 1 20) [22]))))
        ;(TutorialExamples/main (into-array String (map str (concat [5]))))
        ))
    (let [prevf (File. "./clojure/test/com/franz/agraph/tutorial.java.out")]
      (is-each = (read-lines prevf) (read-lines outf)
               "line" (str (.getCanonicalPath prevf) " differs from "
                           (.getCanonicalPath outf))))))
