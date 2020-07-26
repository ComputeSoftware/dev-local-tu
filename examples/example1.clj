(ns example1
  (:require
    [clojure.test :refer :all]
    [datomic.client.api :as d]
    [dev-local-tu.core :as dev-local-tu]))

(deftest test1
  (with-open [db-env (dev-local-tu/test-env)]
    (let [_ (d/create-database (:client db-env) {:db-name "test"})
          conn (d/connect (:client db-env) {:db-name "test"})
          _ (d/transact conn {:tx-data [{:db/ident       ::name
                                         :db/valueType   :db.type/string
                                         :db/cardinality :db.cardinality/one}]})
          {:keys [tempids]} (d/transact conn {:tx-data [{:db/id "a"
                                                         ::name "hi"}]})]
      (is (= {::name "hi"}
             (d/pull (d/db conn)
                     [::name]
                     (get tempids "a")))))))