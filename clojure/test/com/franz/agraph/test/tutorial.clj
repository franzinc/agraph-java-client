;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2012 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.franz.agraph.test.tutorial
  "Tests for AGraph Clojure Tutorial"
  (:use [clojure test])
  (:require [com.franz.agraph.tutorial :as tut]
            [com.franz.agraph.agtest :as agt]))

(deftest run-tutorial
  (binding [tut/*connection-params* (merge tut/*connection-params* (agt/lookup-server-config))]
    (tut/example-all)))
