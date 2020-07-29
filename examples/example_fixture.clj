(ns example-fixture
  (:require
    [clojure.test :refer :all]
    [datomic.client.api :as d]
    [dev-local-tu.core :as dev-local-tu]))

(def ^:dynamic *client* nil)

(defn client-fixture
  [f]
  (with-open [db-env (dev-local-tu/test-env)]
    (binding [*client* (:client db-env)]
      (f))))

(use-fixtures :each client-fixture)

(deftest test1
  (let [_ (d/create-database *client* {:db-name "test"})
        conn (d/connect *client* {:db-name "test"})
        _ (d/transact conn {:tx-data [{:db/ident       ::name
                                       :db/valueType   :db.type/string
                                       :db/cardinality :db.cardinality/one}]})
        {:keys [tempids]} (d/transact conn {:tx-data [{:db/id "a"
                                                       ::name "hi"}]})]
    (is (= {::name "hi"}
           (d/pull (d/db conn)
                   [::name]
                   (get tempids "a"))))))