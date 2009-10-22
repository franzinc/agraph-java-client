;; This software is Copyright (c) Franz, 2009.
;; Franz grants you the rights to distribute
;; and use this software as governed by the terms
;; of the Lisp Lesser GNU Public License
;; (http://opensource.franz.com/preamble.html),
;; known as the LLGPL.

(ns com.franz.test
  "Test utilities."
  (:refer-clojure :exclude (name))
  (:import [java.io File OutputStream FileOutputStream FileWriter
            BufferedReader FileReader PrintStream])
  (:use [clojure.contrib def test-is]
        [com.franz util]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defn run-tests2
  "Runs tests in given namespace with *test-out* bound so it works properly in slime."
  [ns]
  (require ns)
  (binding [*test-out* *out*]
    (run-tests ns)))

(defn with-open2f
  "Calls f within the context of the with-open2 macro."
  [f]
  (with-open2 [] (f)))

(defn read-lines
  [#^File f]
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
