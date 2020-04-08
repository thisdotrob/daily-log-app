(ns daily-log.server.core
  (:require [io.pedestal.http :as http]
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
         :user "daily_log_server" :password "barbwirebracket"})

(def ds (jdbc/get-datasource db))

(defn as-unqualified-kebab-maps [rs opts]
  (let [kebab #(clojure.string/replace % #"_" "-")]
    (result-set/as-unqualified-modified-maps rs
                                             (assoc opts :label-fn kebab))))

(defn insert-activity! [activity]
  (try
    [(sql/insert! ds
                  :activities
                  {:user_id USER-ID
                   :name (:name activity)
                   :type (name (:type activity))}
                  {:builder-fn as-unqualified-kebab-maps})
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn get-activities-for-user-id! [user-id]
  (try
    [(map #(update % :type keyword)
          (sql/find-by-keys ds
                            :activities
                            {:user_id user-id}
                            {:builder-fn as-unqualified-kebab-maps}))
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn update-log! [{:keys [value date activity-id] :as log}]
  (try
    [(sql/update! ds
                  :logs
                  {:value value}
                  {:date  (name date)
                   :activity_id activity-id}
                  {:return-keys true
                   :builder-fn as-unqualified-kebab-maps})
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn insert-log! [{:keys [value date activity-id] :as log}]
  (try
    (println ">>>")
    [(sql/insert! ds
                  :logs
                  {:activity_id activity-id
                   :date (name date)
                   :value value}
                  {:builder-fn as-unqualified-kebab-maps})
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn upsert-log! [{:keys [value date activity-id] :as log}]
  (let [update-result (update-log! log)]
    (if (nil? (first update-result))
      (insert-log! log)
      update-result)))

(defn get-logs-for-user-id! [user-id]
  (let [qry "SELECT activity_id, date, value
             FROM logs JOIN activities
             ON activity_id = id
             WHERE user_id = ?"]
    (try
      [(sql/query ds [qry user-id] {:builder-fn as-unqualified-kebab-maps})
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
