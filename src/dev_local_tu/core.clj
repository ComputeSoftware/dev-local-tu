(ns dev-local-tu.core
  (:require
    [clojure.string :as str]
    [datomic.client.api :as d]
    [datomic.local]
    [clojure.java.io :as io]
    [dev-local-tu.internal.impl :as impl])
  (:import (java.io Closeable File)))

(defn gen-name!
  [prefix]
  (str prefix "-" (impl/rand-str! 7)))

(def default-datomic-dev-local-storage-dir
  (.getAbsolutePath (io/file (System/getProperty "user.home") ".datomic" "data")))

(defn dev-local-directory
  ^File [{:keys [system-name db-name storage-dir]
          :or   {storage-dir default-datomic-dev-local-storage-dir}}]
  (let [file-args (cond-> [storage-dir]
                    system-name (conj system-name)
                    db-name (conj db-name))]
    (apply io/file file-args)))

(defn -new-env-map
  [{:keys [prefix system storage-dir]
    :or   {prefix "dev-local"}}]
  (let [system (or system (gen-name! prefix))
        storage-dir (if (or
                          (= storage-dir :mem)
                          (and storage-dir
                            (str/starts-with? storage-dir "/")))
                      storage-dir
                      (.getAbsolutePath
                        (dev-local-directory
                          (cond-> {}
                            storage-dir
                            (assoc ::storage-dir storage-dir)))))
        client-map {:server-type :datomic-local
                    :system      system
                    :storage-dir storage-dir}]
    {:client      (d/client client-map)
     :client-map  client-map
     :system      system
     :storage-dir storage-dir}))

(defn delete-dev-local-system!
  "Deletes a :dev-local system's data. Always returns true. Throws on failure."
  ([system-name]
   (delete-dev-local-system! system-name default-datomic-dev-local-storage-dir))
  ([system-name storage-dir]
   (let [f (dev-local-directory {:system-name system-name
                                 :storage-dir storage-dir})]
     (when (.exists ^File f)
       (impl/delete-directory! f))
     true)))

(comment
  (-new-env-map {})
  )

(defn release-dbs
  "Releases all DBs."
  [client system]
  (doseq [db-name (d/list-databases client {})]
    ;; https://docs.datomic.com/cloud/datomic-local.html#release-db
    (datomic.local/release-db
      {:system system :db-name db-name})))

(defn cleanup-env!
  "Releases resources used by client and deletes the data directory for the system."
  [{:keys [client system client-map]}]
  (release-dbs client system)
  (when (string? (:storage-dir client-map))
    (delete-dev-local-system! system)))

(defrecord TestEnv [system client]
  Closeable
  (close [env]
    (cleanup-env! env)))

(defn test-env
  "Returns a Closable test environment. Optionally takes a map with the following
   keys.
      :prefix - The prefix for the generated system name.
      :system - Force a specific system name. Will override prefix.
      :storage-dir - The storage directory for data passed to the d/client.
        Defaults to ~/.datomic/data. Pass :mem to use a memory only database.

   The returned test environment has the following keys.
      :client - The generated Datomic client.
      :system - The name of the system, dynamically generated unless ::system was
        specified.
      :client-map - The map passed to the d/client call.

   When closed, the test environment will release the resources used by the client
   and delete the data directory."
  ([] (test-env {}))
  ([env-argm]
   (let [env (-new-env-map env-argm)]
     (map->TestEnv env))))

(comment
  (def e (test-env {:system "test"}))
  (.close e)
  (d/create-database (:client e) {:db-name "test"})
  (d/list-databases (:client e) {})
  (def conn (d/connect (:client e) {:db-name "test"}))
  (d/transact conn {:tx-data [{:db/ident       ::foo
                               :db/valueType   :db.type/string
                               :db/cardinality :db.cardinality/one}]})
  )