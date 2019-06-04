(ns metabase.driver.dynamodb
  (:refer-clojure :exclude [second])
  (:require [metabase.driver :as driver]
            [metabase.driver.common :as driver.common]
            [metabase.util
             [date :as du]]
            [metabase.query-processor.store :as qp.store]
            [metabase.driver.dynamodb
             [query-processor :as qp]
             [util :refer [with-dynamodb-client]]]))

(driver/register! :dynamodb)

(defmethod driver/display-name :dynamodb [_]
  "DynamoDB")

(defmethod driver/can-connect? :dynamodb [_ details]
  true)

(defmethod driver/describe-database :dynamodb [_ database]
  (with-dynamodb-client [_ database]
    {:tables (set (for [tname (qp/list-tables)]
                    {:schema nil, :name tname}))}))

(defmethod driver/process-query-in-context :dynamodb [_ qp]
  (fn [{database-id :database, :as query}]
    (with-dynamodb-client [_ (qp.store/database)]
      (qp query))))

(defmethod driver/describe-table :dynamodb [_ database {table-name :name}]
  (with-dynamodb-client [_ database]
    {:schema nil
     :name   table-name
     :fields (set (qp/describe-table table-name))}))

(defmethod driver/mbql->native :dynamodb [_ query]
  (qp/mbql->native query))

(defmethod driver/execute-query :dynamodb [_ query]
  (qp/execute-query query))
