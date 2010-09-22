;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008-2010 Franz Inc.
;; All rights reserved. This program and the accompanying materials
;; are made available under the terms of the Eclipse Public License v1.0
;; which accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.franz.agraph
  "Clojure client API to Franz AllegroGraph 4.0.
 This API wraps the agraph-java-client API, which is an extension of the Sesame org.openrdf API.
 Communication with the server is through HTTP REST using JSON.
 Uses the Franz Clojure wrapper of Sesame in com/franz/openrdf.clj."
  (:refer-clojure :exclude (name))
  (:import [clojure.lang Named]
           [com.franz.agraph.repository
            AGCatalog AGQueryLanguage AGRepository
            AGRepositoryConnection AGServer AGValueFactory]
           [org.openrdf.model ValueFactory Resource Literal Statement URI]
           [org.openrdf.repository Repository RepositoryConnection]
           [org.openrdf.model.vocabulary RDF XMLSchema]
           [org.openrdf.query QueryLanguage BindingSet Binding])
  (:use [com.franz util openrdf]))

(alter-meta! *ns* assoc :author "Franz Inc <www.franz.com>, Mike Hinchey <mhinchey@franz.com>")

(defmulti name
  "Shadows clojure.core/name to make it an extensible method."
  type)

(defmethod name :default [x] (.getName x))

;; same as the clojure.core fn name
(defmethod name clojure.lang.Named [#^clojure.lang.Named x] (clojure.core/name x))

(defmethod name AGRepository [#^AGRepository x] (.getRepositoryID x))

(defmethod name AGCatalog [#^AGCatalog x] (.getCatalogName x))

(defmethod close AGCatalog [#^AGCatalog obj])

;; (defn connect-agraph
;;   "returns a connection to AllegroGraph server, for the HTTP REST API"
;;   {:tag AGServer}
;;   ([host port] (connect host port nil nil))
;;   ([host port username password]
;;      (AGServer. host port)))

(defn catalogs
  "Returns a seq of AGCatalogs objects."
  [#^AGServer server]
  (seq (.listCatalogs server)))

(defn open-catalog
  "Returns an AGCatalog."
  {:tag AGCatalog}
  [#^AGServer server name]
  (.getCatalog server name))

(defn repositories
  "Returns a seq of AGRepository objects."
  [#^AGCatalog catalog]
  (seq (.listRepositories catalog)))

;; (def #^{:private true} -access-verbs
;;      {:renew AGRepository/RENEW
;;       :create AllegroRepository/CREATE
;;       :open AllegroRepository/OPEN
;;       :access AllegroRepository/ACCESS})
;; (def access-verbs (set (keys -access-verbs)))

(def lang-prolog (AGQueryLanguage/PROLOG))

(defn repository
  "access-verb must be a keyword from the set of access-verbs."
  ([#^AGCatalog catalog name access-verb]
     (open (.createRepository catalog #^String name
                              ;; TODO: (-access-verbs access-verb)
                              )))
  ;; TODO: this may be confusing since it doesn't open a repository,
  ;; only gets a reference.
  ([#^Repository rcon]
     (.getRepository rcon)))

(defn ag-repo-con
  ([#^AGCatalog catalog repo-name]
     (repo-connection (repo-init (repository catalog repo-name nil))))
  ([#^AGCatalog catalog repo-name rcon-args]
     (repo-connection (repo-init (repository catalog repo-name nil)) rcon-args)))

(defn repo-federation
  "rcons: may be of type AGRepository or AGRepositoryConnection"
  [#^AGServer server rcons rcon-args]
  (-> (.federate server
                 (into-array AGRepository (map #(cond (instance? AGRepository %) %
                                                      (nil? %) nil
                                                      :else (.getRepository #^AGRepositoryConnection %))
                                                rcons)))
      open repo-init (repo-connection rcon-args)))

(defn ag-server
  [url username password]
  (AGServer. url username password))

(defn with-agraph-fn
  "catalog, repository, and repository-connection are optional: they are only
   opened if specified in the arguments.
  
     access:    a keyword from the set of 'access-verbs.
     my-fn:     a function of 4 args [conn cat repos repos-conn]
                catalog and rcon will be closed when this block exits.
     rcon-args: if nil, no rcon will be created. Arguments passed to
                franz.openrdf/repo-connection."
  [[{:keys [host port username password]}
    catalog-name
    {:keys [name access]}
    rcon-args]
   my-fn]
  (with-open2 []
    (let [conn (ag-server (str "http://" host ":" port) username password)]
      (if catalog-name
        (let [cat (open-catalog conn catalog-name)]
          (with-open2 [repo (when name
                              (repo-init (repository cat name access)))
                       rcon (when rcon-args
                              (repo-connection repo rcon-args))]
            (my-fn conn cat repo rcon)))
        (my-fn conn nil nil nil)))))

(defmacro with-agraph
  "catalog, repository, and repository-connection are optional - they are only opened if specified in the args.
access: a keyword from the set of 'access-verbs.
catalog and repository-connection will be closed when this block exits.
rcon-args: optional, args passed to franz.openrdf/repo-connection.
Example: (with-agraph [conn {:host \"localhost\" :port 8080
                             :username name :password pw}
                       catalog \"scratch\"
                       repo {:name \"agraph_test\" :access :renew}
                       rcon {:auto-commit true :namespaces {\"ns-prefix\" \"ns\"}}]
            (println conn))"
  [[conn-sym conn-args
    cat-sym cat-name
    repo-sym {repos-name :name repos-access :access :as repo-args}
    rcon-sym rcon-args]
   & body]
  `(with-agraph-fn [~conn-args ~cat-name ~repo-args
                    ;; rcon-args should be nil if no rcon-sym is wanted
                    ~(or rcon-args (when rcon-sym {}))]
     (fn [~conn-sym ~(or cat-sym '_) ~(or repo-sym '_) ~(or rcon-sym '_)]
       ~@body)))

(defn add-from-server!
  ;; Different name from add-from! to make it less ambiguous.
  ;; This is an AllegroGraph extension to the openrdf api.
  "Add statements from a data file on the server.
   See add-from!.
  
     data:     a File, InputStream, or URL.
     contexts: 0 or more Resource objects"
  [#^AGRepositoryConnection repos-conn
   data
   #^String baseURI
   #^RDFFormat dataFormat
   & contexts]
  (.add repos-conn data baseURI dataFormat (resource-array contexts)))
