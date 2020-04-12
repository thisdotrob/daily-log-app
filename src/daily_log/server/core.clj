(ns daily-log.server.core
  (:require [clojure.set :refer [rename-keys]]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as params]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as result-set]
            [next.jdbc :as jdbc]))

(extend-protocol result-set/ReadableColumn
  java.sql.Date
  (read-column-by-label ^java.time.LocalDate [^java.sql.Date v _]
    (keyword (.toString v)))
  (read-column-by-index ^java.time.LocalDate [^java.sql.Date v _2 _3]
    (keyword (.toString v))))

(def USER-ID 1) ;; this will eventually come from auth

(def db {:dbtype "postgres" :dbname "daily_log"
         :stringtype "unspecified" ; HACK to support enums (needed for activity type)
         :user (System/getenv "DAILY_LOG_SERVER_PG_USER")
         :password (System/getenv "DAILY_LOG_SERVER_PG_PASSWORD")})

(def ds (jdbc/get-datasource db))

(defn as-unqualified-kebab-maps [rs opts]
  (let [kebab #(clojure.string/replace % #"_" "-")]
    (result-set/as-unqualified-modified-maps rs
                                             (assoc opts :label-fn kebab))))

(defn sql-log->log [m]
  (update m :activity-id (comp keyword str)))

(defn log->sql-log [m]
  (-> m
      (update :activity-id name)
      (update :date name)
      (rename-keys {:activity-id :activity_id})))

(defn sql-activity->activity [m]
  (-> m
      (update :type keyword)
      (update :id (comp keyword str))
      (update :user-id (comp keyword str))))

(defn insert-activity! [activity]
  (try
    [(-> (sql/insert! ds
                      :activities
                      {:user_id USER-ID
                       :name (:name activity)
                       :type (name (:type activity))}
                      {:builder-fn as-unqualified-kebab-maps})
         sql-activity->activity)
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn get-activities-for-user-id! [user-id]
  (try
    [(->> (sql/find-by-keys ds
                            :activities
                            {:user_id user-id}
                            {:builder-fn as-unqualified-kebab-maps})
          (map sql-activity->activity))
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn update-log! [log]
  (try
    [(sql/update! ds
                  :logs
                  (select-keys log [:value])
                  (select-keys log [:date :activity_id])
                  {:return-keys true
                   :builder-fn as-unqualified-kebab-maps})
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn insert-log! [log]
  (try
    [(sql/insert! ds
                  :logs
                  log
                  {:builder-fn as-unqualified-kebab-maps})
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn upsert-log! [log]
  (let [sql-log (log->sql-log log)
        update-result (update-log! sql-log)
        result (if (some some? update-result)
                 update-result
                 (insert-log! sql-log))]
    [(some-> result first sql-log->log)
     (second result)]))

(defn get-logs-for-user-id! [user-id]
  (let [qry "SELECT activity_id, date, value
             FROM logs JOIN activities
             ON activity_id = id
             WHERE user_id = ?"]
    (try
      [(->> (sql/query ds [qry user-id] {:builder-fn as-unqualified-kebab-maps})
            (map sql-log->log))
       nil]
      (catch org.postgresql.util.PSQLException e
        [nil e]))))

(def insert-activity
  {:name :insert-activity
   :enter
   (fn [{:keys [request] :as context}]
     (let [edn-params (:edn-params request)
           [result err] (insert-activity! edn-params)]
       (if (some? err)
         (assoc context :response {:status 500 :body (.getMessage err)})
         (assoc context :response {:status 200 :body result}))))})

(def get-activities
  {:name :get-activities
   :enter
   (fn [context]
     (let [[result err] (get-activities-for-user-id! USER-ID)]
       (if (some? err)
         (assoc context :response {:status 500 :body (.getMessage err)})
         (assoc context :response {:status 200 :body result}))))})

(def upsert-log
  {:name :upsert-log
   :enter
   (fn [{:keys [request] :as context}]
     (let [edn-params (:edn-params request)
           [result err] (upsert-log! edn-params)]
       (if (some? err)
         (assoc context :response {:status 500 :body (.getMessage err)})
         (assoc context :response {:status 200 :body result}))))})

(def get-logs
  {:name :get-logs
   :enter
   (fn [context]
     (let [[result err] (get-logs-for-user-id! USER-ID)]
       (if (some? err)
         (assoc context :response {:status 500 :body (.getMessage err)})
         (assoc context :response {:status 200 :body result}))))})

(def body-parser (params/body-params))

(def routes
  (route/expand-routes
   #{["/activities" :post [body-parser insert-activity]]
     ["/activities" :get [get-activities]]
     ["/logs" :post [body-parser upsert-log]]
     ["/logs" :get [get-logs]]}))

(def service-map
  {::http/routes routes
   ::http/type   :jetty
   ::http/port   8890
   ::http/secure-headers {:content-security-policy-settings "object-src 'none'; script-src 'self' 'unsafe-eval' 'unsafe-inline'"}
   ::http/resource-path "/public"})

(defn start []
  (http/start (http/create-server service-map)))
