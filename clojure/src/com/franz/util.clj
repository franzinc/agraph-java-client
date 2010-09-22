;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.franz.util
  "Utility functions."
  (:refer-clojure :exclude [name with-open])
  (:require [clojure.stacktrace :as st]))

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
          (st/print-cause-trace e))))
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

(defn printlns
  "println each item in a collection"
  [col]
  (doseq [x col]
    (println x)))

(defn read-lines
  "Calls clojure.core/line-seq, but f is a File and (open) is called on the
  reader that is created, so read-lines must be called with a (scope)."
  [#^java.io.File f]
  (line-seq (open (java.io.BufferedReader. (open (java.io.FileReader. f))))))

(defn write-lines
  [#^java.io.File f
   lines]
  (with-open2 [out (java.io.PrintWriter. (java.io.FileWriter. f))]
    (doseq [ln lines]
      (.println #^java.io.PrintWriter out ln))))
