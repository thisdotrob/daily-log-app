(ns user
  (:require [io.pedestal.http :as http]
            [io.pedestal.test :as test]
            [clojure.tools.namespace.repl :as tools]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [daily-log.core :as daily-log]))

(defonce server (atom nil))

(defn start-dev []
  (reset! server
          (http/start (http/create-server
                       (assoc daily-log/service-map
                              ::http/join? false))))
  nil)

(defn stop-dev []
  (http/stop @server)
  (reset! server nil)
  nil)

(defn test-response-for [verb url body]
  (-> (test/response-for (:io.pedestal.http/service-fn @server)
                         verb url
                         :headers {"Content-Type" "application/edn"}
                         :body (pr-str body))
      (select-keys [:status :body])))

(defn refresh [] (tools/refresh))

(defn test-post-activity [activity-name activity-type]
  (test-response-for :post "/activities" {:name activity-name
                                          :type activity-type}))

(defn test-get-activities []
  (test-response-for :get "/activities" {}))

(defn test-post-log [activity-id date value]
  (test-response-for :post "/logs" {:activity-id activity-id
                                    :date date
                                    :value value}))

(defn test-get-logs []
  (test-response-for :get "/logs" {}))
