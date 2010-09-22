;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.franz.agraph.stress-conn
  "Stress test connections"
  (:refer-clojure :exclude (name with-open))
  (:use [clojure test]
        [com.franz util openrdf agraph test]
        [com.franz.agraph agtest]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

;; to make sure any other opened things get closed with each test
(use-fixtures :each with-open2f)

(deftest stress-small
  (dotimes [x 400]
    (try (with-agraph-test
           #(do
              (clear! rcon)
              (add! rcon [(uri vf "http://example.org/stress/conn")
                          (uri vf "http://example.org/stress/count")
                          (literal vf x)] nil)
              (when (mod x 3)
                (throw (IllegalArgumentException. "goto")))
              ))
         (catch IllegalArgumentException e)
         (catch Exception e
           (throw (Exception. (str "failed on try: " x) e))))))

(deftest stress-big
  (dotimes [x 300]
    (try (with-agraph-test
           #(dotimes [y 110]
              (try (clear! rcon)
                   (add! rcon [(uri vf "http://example.org/stress/conn")
                               (uri vf "http://example.org/stress/count")
                               (literal vf x)] nil)
                   (when (mod x 20)
                     (throw (IllegalArgumentException. "goto")))
                   (catch IllegalArgumentException e (throw e))
                   (catch Exception e
                     (throw (Exception. (str "failed on try: " x "/" y) e)))
                   )))
         (catch IllegalArgumentException e)
         (catch Exception e
           (throw (Exception. (str "failed on try: " x) e)))
         )))
