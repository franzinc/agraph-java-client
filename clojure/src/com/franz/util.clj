;; This software is Copyright (c) Franz, 2009.
;; Franz grants you the rights to distribute
;; and use this software as governed by the terms
;; of the Lisp Lesser GNU Public License
;; (http://opensource.franz.com/preamble.html),
;; known as the LLGPL.

(ns com.franz.util
  "Utility functions."
  (:use [clojure.contrib stacktrace]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defmulti close
  ;; TODO: rename to close!
  "Used by with-closeable in a finally block to close objects.
Methods are defined for java.io.Closeable and a default for (.close) by reflection.
May be extended for differently named close methods."
  type)

(defmethod close :default
  [obj]
  (when obj (.close obj)))

(defmethod close java.io.Closeable
  [#^java.io.Closeable obj]
  (.close obj))

(declare *with-open-stack*)

(defn close-all
  "Not intended to be used other than by with-open2.
  Calls close on all objects in open-stack, catches and prints any exceptions."
  [open-stack]
  (when (seq open-stack)
    (try
      (close (first open-stack))
      (catch Throwable e
        (binding [*out* *err*]
          (print "Ignoring exception from close: " e)
          (print-cause-trace e))))
    (recur (next open-stack))))

(defn open
  "Register obj to be closed before the enclosing with-open exits.
   Must be called within the context of with-open.
   Returns the same obj."
  [obj]
  (when (and obj (not (some #{obj} *with-open-stack*)))
    (set! *with-open-stack* (conj *with-open-stack* obj)))
  obj)

(defmacro with-open2
  "Similar to clojure.core/with-open, but also closes objects for which (open) was called within the body.

  Only a single try/finally is used. The bindings are wrapped by a call to open.
  In the finally, close-all is called, closing all opened objects in reverse order.

  All exceptions thrown by close methods will be caught and printed to System/err.
  For different behavior, use a binding on close to catch exceptions.

  Except for the different behavior of catching exceptions from close, this can replace with-open.

  Example: (with-open [f (FileReader. x)] ... )
  Example: (with-open [] (... (open (FileReader. x))))"
  [bindings & body]
  `(binding [*with-open-stack* ()]
     (let ~(into []
                 (mapcat (fn [[b v]]
                           (if (symbol? b)
                             [b `(open ~v)]
                             (throw
                               (IllegalArgumentException.
                                 (str "with-open2: binding must be a symbol: " b)))))
                         (partition 2 bindings)))
       (try
         ~@body
         (finally
           (close-all *with-open-stack*))))))

(defmacro scope-let
  "Establishes a with-open2 scope and a plain let form."
  [let-bindings & body]
  `(with-open2 []
     (let ~let-bindings
       ~@body)))

(defmacro scope1
  "Establishes a with-open2 scope only if there is not already one open."
  [& body]
  `(let [scope1# (fn [] ~@body)]
     (if (.isBound (var *with-open-stack*))
       (scope1#)
       (with-open2 []
         (scope1#)))))

(defmacro scope1-let
  "Establishes a scope1 (a scope if there isn't one already) and a plain let form."
  [let-bindings & body]
  `(scope1 (let ~let-bindings
             ~@body)))

(defn printlns
  "println each item in a collection"
  [col]
  (doseq [x col]
    (println x)))

(defn read-lines
  [#^java.io.File f]
  (line-seq (open (java.io.BufferedReader. (open (java.io.FileReader. f))))))

()