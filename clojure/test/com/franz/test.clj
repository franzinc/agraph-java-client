;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.franz.test
  "Test utilities."
  (:refer-clojure :exclude (name with-open))
  (:import [java.io File OutputStream FileOutputStream FileWriter
            BufferedReader FileReader PrintStream])
  (:use [clojure test]
        [com.franz util]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defn with-open2f
  "Calls f within the context of the with-open2 macro."
  [f]
  (with-open2 [] (f)))

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

(defn run-tests-exit
  "Run all tests in all given namespaces; print results; exit.
  Calls System/exit with 0 for success or -1 for errors and failures.
  For use in ant, use fork=true and failonerror=true.
  Note, ant fork=false in some cases prevents test output from printing.

  (This is a copy of clojure.test/run-tests-exit, patch submitted.)"
  [& namespaces]
  (apply require :reload-all namespaces)
  (System/exit (int (if (successful? (apply run-tests namespaces)) 0 -1))))
