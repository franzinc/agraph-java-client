;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2012 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; To build and install into local maven repo:
;;   export VERSION=4.7
;;   cd .. # agraph-java-client
;;   ant mvn-install
;;   cd clojure
;;   lein jar
;;   lein install
;;

;; Version is parameterized instead of hard-coded in multiple places.
;; VERSION is set by ../makefile.
;; This same algorithm is in ../build.xml
(def agraph-version (or (let [s (System/getenv "VERSION")]
                          (when (seq s) s))
                        (str (.trim (slurp "../.git-branch-name")) "-snapshot")))

(def agraph-git-rev
     ;; better way to do this: "git rev-parse HEAD"
     (try (second (seq (.split (with-open [r (java.io.BufferedReader.
                                              (java.io.FileReader. "../.git/logs/HEAD"))]
                                 (last (line-seq r))) " ")))
          (catch Exception e)))

(defproject com.franz/agraph-clj
  agraph-version
  :description "Clojure client API for Franz AllegroGraph v4"
  :url "http://github.com/franzinc/agraph-java-client"
  :source-path "src"
  :target-dir "dist"
  :extra-classpath-dirs ["tutorial"]
  :repl-init com.franz.agraph.tutorial
  :warn-on-reflection true
  ;; TODO :parent [com.franz/agraph-java-client #=(eval (str agraph-version)) :relative-path "../pom.xml"]
  :manifest {"Implementation-Title" "Clojure client API for Franz AllegroGraph v4"
             "Implementation-Version" #=(eval (str agraph-version))
             "Implementation-Vendor" "Franz, Inc"
             "Git-Rev" #=(eval (str agraph-git-rev))
             "Built-At" #=(eval (.format (java.text.SimpleDateFormat. "yyyy-MM-dd H:mm") (java.util.Date.)))}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.franz/agraph-java-client #=(eval (str agraph-version))]]
  :min-lein-version "1.7.1")
