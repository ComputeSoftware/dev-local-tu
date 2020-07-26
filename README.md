# dev-local-tu

Test utility for Datomic dev-local. 

```clojure
dev-local-tu {:mvn/version "0.1.0"}
```

## Rationale 

Datomic dev-local provides a way to create clean environments useful for testing.
It does not provide a methodology for how to use it. 
This library provides an opinionated way to use dev-local in your tests.

## Usage

The primary function exposed by the api is `test-env`. 
This function returns a test environment.
A test environment will contain a Datomic client, created by dev local, that can be cleaned up by simply calling `.close`.
A test environment is typically used within a `with-open`.

```clojure
(require '[dev-local-tu.core :as dev-local-tu]
         '[datomic.client.api :as d])

(with-open [db-env (dev-local-tu/test-env)]
  (let [_ (d/create-database (:client db-env) {:db-name "test"})
        conn (d/connect (:client db-env) {:db-name "test"})
        _ (d/transact conn {:tx-data [{:db/ident       ::name
                                       :db/valueType   :db.type/string
                                       :db/cardinality :db.cardinality/one}]})
        {:keys [tempids]} (d/transact conn {:tx-data [{:db/id "a"
                                                       ::name "hi"}]})]
    (d/pull (d/db conn)
            [::name]
            (get tempids "a"))))
=> #:example1{:name "hi"}
```

If you would prefer to manage the lifecycle, use `new-env` to generate a new, random system and `cleanup-env!` to remove the associated resources.