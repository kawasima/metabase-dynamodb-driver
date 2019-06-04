(ns metabase.driver.dynamodb.query-processor
  (:require [clojure.string :as str]
            [metabase.mbql
             [schema :as mbql.s]
             [util :as mbql.u]]
            [metabase.models.field :refer [Field]]
            [metabase.query-processor
             [interface :as i]
             [store :as qp.store]]
            [metabase.driver.dynamodb.util :refer [*dynamodb-client*]])
  (:import metabase.models.field.FieldInstance))

(def ^:dynamic ^:private *query* nil)

(defn list-tables []
  (-> (.listTables *dynamodb-client*)
      (.getTableNames)))

(defn- dynamodb-type->base-type [attr-type]
  (case attr-type
    "N"      :type/Decimal
    "S"      :type/Text
    "BOOL"   :type/Boolean
    :type/*))

(defn describe-table [table]
  (let [table-desc (-> (.describeTable *dynamodb-client* table)
                       (.getTable))]
    (println "describe-table" table-desc)
    (for [attribute-def (.getAttributeDefinitions table-desc)]
      {:name      (.getAttributeName attribute-def)
       :database-type (.getAttributeType attribute-def)
       :base-type (dynamodb-type->base-type (.getAttributeType attribute-def))})) )

;;
;;
;;
(defmulti ^:private ->rvalue
  "Format this `Field` or value for use as the right hand value of an expression, e.g. by adding `$` to a `Field`'s
  name"
  {:arglists '([x])}
  mbql.u/dispatch-by-clause-name-or-class)

(defmulti ^:private ->lvalue
  "Return an escaped name that can be used as the name of a given Field."
  {:arglists '([field])}
  mbql.u/dispatch-by-clause-name-or-class)

(defmulti ^:private ->initial-rvalue
  "Return the rvalue that should be used in the *initial* projection for this `Field`."
  {:arglists '([field])}
  mbql.u/dispatch-by-clause-name-or-class)


(defn- field->name
  "Return a single string name for FIELD. For nested fields, this creates a combined qualified name."
  ^String [^FieldInstance field, ^String separator]
  (if-let [parent-id (:parent_id field)]
    (str/join separator [(field->name (qp.store/field parent-id) separator)
                         (:name field)])
    (:name field)))

(defmethod ->lvalue         (class Field) [this] (field->name this "___"))
(defmethod ->initial-rvalue (class Field) [this] (str \$ (field->name this ".")))
(defmethod ->rvalue         (class Field) [this] (str \$ (->lvalue this)))

(defmethod ->lvalue         :field-id [[_ field-id]] (->lvalue         (qp.store/field field-id)))
(defmethod ->initial-rvalue :field-id [[_ field-id]] (->initial-rvalue (qp.store/field field-id)))
(defmethod ->rvalue         :field-id [[_ field-id]] (->rvalue         (qp.store/field field-id)))

(defn- handle-fields [{:keys [fields]} pipeline-ctx]
  (println "handle-fields" fields)
  (if-not (seq fields)
    pipeline-ctx
    (let [new-projections (for [field fields]
                            [(->lvalue field) (->rvalue field)])]
      (-> pipeline-ctx
          (assoc :projections (map (comp keyword first) new-projections))
          (update :query conj (into {} new-projections))))))

(defn mbql->native [{{source-table-id :source-table} :query, :as query}]
  (let [{source-table-name :name} (qp.store/table source-table-id)]
    (binding [*query* query]
      (println "mbql->native:" query)
      {:projections nil
       :query       (reduce (fn [pipeline-ctx f]
                              (f (:query query) pipeline-ctx))
                            {:projections [], :query []}
                            [handle-fields])
       :collection  nil
       :mbql?       true})))

(defn execute-query
  [{{:keys [collection query mbql? projections]} :native}]
  (println "execute-query:"  query)
  {:rows []})
