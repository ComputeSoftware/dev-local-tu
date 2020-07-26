(ns dev-local-tu.core
  (:require
    [datomic.client.api :as d]
    [datomic.dev-local]
    [clojure.java.io :as io]
    [dev-local-tu.internal.impl :as impl])
  (:import (java.io Closeable File)))

(defn gen-name!
  [prefix]
  (str prefix "-" (impl/rand-str! 7)))

(def default-datomic-dev-local-storage-dir
  (.getAbsolutePath (io/file (System/getProperty "user.home") ".datomic" "data")))

(defn dev-local-directory
  [{::keys [system-name db-name storage-dir]
    :or    {storage-dir default-datomic-dev-local-storage-dir}}]
  (let [file-args (cond-> [storage-dir system-name]
                          db-name (conj db-name))]
    (apply io/file file-args)))

(defn new-env
  [{::keys [prefix system storage-dir]
    :or    {prefix "dev-local"}}]
  (let [system (or system (gen-name! prefix))
        storage-dir (.getAbsolutePath
                      (dev-local-directory
                        (cond-> {::system-name system}
                                storage-dir
                                (assoc ::storage-dir storage-dir))))
        client-map {:server-type :dev-local
                    :system      system
                    :storage-dir storage-dir}]
    {::client      (d/client client-map)
     ::client-map  client-map
     ::system      system
     ::storage-dir storage-dir}))

(defn delete-dev-local-system!
  "Deletes a :dev-local system's data. Always returns true. Throws on failure."
  ([system-name]
   (delete-dev-local-system! system-name default-datomic-dev-local-storage-dir))
  ([system-name storage-dir]
   (let [f (dev-local-directory {::system-name system-name
                                 ::storage-dir storage-dir})]
     (if (.exists ^File f)
       (do
         (impl/delete-directory! f)
         true)
       true))))

(comment
  (new-env {})
  )

(defn release-dbs
  "Releases all DBs."
  [client system]
  (doseq [db-name (d/list-databases client {})]
    ;; https://docs.datomic.com/cloud/dev-local.html#release-db
    (datomic.dev-local/release-db
      {:system system :db-name db-name})))

(defn cleanup-env!
  "Releases resources used by client and deletes the data directory for the system."
  [{::keys [client system]}]
  (release-dbs client system)
  (delete-dev-local-system! system))

(defrecord TestEnv [system client]
  Closeable
  (close [_]
    (cleanup-env!
      {::client client
       ::system system})))

(defn test-env
  "Returns a Closable test environment. Optionally takes a map with the following
   keys.
      ::prefix - The prefix for the generated system name.
      ::system - Force a specific system name. Will override prefix.
      ::storage-dir - The storage directory for data passed to the d/client.
        Defaults to ~/.datomic/data.

   The returned test environment has the following keys.
      :client - The generated Datomic client.
      :system - The name of the system, dynamically generated unless ::system was
        specified.
      :client-map - The map passed to the d/client call.

   When closed, the test environment will release the resources used by the client
   and delete the data directory."
  ([] (test-env {}))
  ([env-argm]
   (let [{::keys [client client-map system]} (new-env env-argm)]
     (map->TestEnv
       {:system     system
        :client-map client-map
        :client     client}))))

(comment
  (def e (test-env))
  (.close e)
  (d/create-database (:client e) {:db-name "test"})
  )