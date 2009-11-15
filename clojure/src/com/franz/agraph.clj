;; This software is Copyright (c) Franz, 2009.
;; Franz grants you the rights to distribute
;; and use this software as governed by the terms
;; of the Lisp Lesser GNU Public License
;; (http://opensource.franz.com/preamble.html),
;; known as the LLGPL.

(ns com.franz.agraph
  "Clojure client API to Franz AllegroGraph 4.0.
 This API wraps the agraph-java-client API, which is an extension of the Sesame org.openrdf API.
 Communication with the server is through HTTP REST using JSON.
 Uses the Franz Clojure wrapper of Sesame in com/franz/openrdf.clj."
  (:refer-clojure :exclude (name))
  (:import [com.knowledgereefsystems.agsail AllegroSail]
           [java.io File]
           [java.net URI]
           [org.openrdf.model ValueFactory Resource Literal Statement]
           [org.openrdf.repository Repository RepositoryConnection]
           [org.openrdf.model.vocabulary RDF XMLSchema]
           [org.openrdf.query QueryLanguage BindingSet Binding])
  (:use [clojure.contrib def]
        [com.franz util openrdf]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defn ag-repository
  [{:keys [host port db-dir repository]}]
  (repo-init (open (org.openrdf.repository.sail.SailRepository.
                    (AllegroSail. host port false repository
                                  (File. db-dir) -1 -1 false false)))))

(defn repository
  "access-verb must be a keyword from the set of access-verbs."
  ([#^Repository rcon]
     (.getRepository rcon)))

(defn agraph-repoconn
  ([{:keys [host port db-dir repository] :as connection-params}]
     (repo-connection (repo-init (ag-repository connection-params))))
  ([{:keys [host port db-dir repository] :as connection-params}
    rcon-args]
     (repo-connection (repo-init (ag-repository connection-params)) rcon-args)))

;; (defn repo-federation
;;   "rcons: may be of type AGRepository or AGRepositoryConnection"
;;   [#^AGServer server repo-name rcons rcon-args]
;;   (-> (.createFederation server repo-name
;;                          (into-array AGRepository (map #(cond (instance? AGRepository %) %
;;                                                               (nil? %) nil
;;                                                               :else (.getRepository #^AGRepositoryConnection %))
;;                                                        rcons)))
;;       open repo-init (repo-connection rcon-args)))

;; (defn add-from-server!
;;   ;; Different name from add-from! to make it less ambiguous.
;;   ;; This is an AllegroGraph extension to the openrdf api.
;;   "Add statements from a data file on the server.
;;    See add-from!.
  
;;      data:     a File, InputStream, or URL.
;;      contexts: 0 or more Resource objects"
;;   [#^AGRepositoryConnection repos-conn
;;    data
;;    #^String baseURI
;;    #^RDFFormat dataFormat
;;    & contexts]
;;   (.add repos-conn data baseURI dataFormat true (resource-array contexts)))
