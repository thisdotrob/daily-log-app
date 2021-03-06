(ns daily-log.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [ajax.edn :as ajax]
            [daily-log.dates :as d]
            [daily-log.db :as db]))

(defn initialise-db [{:keys [today]} _]
  {:db (assoc db/default-db
              :date-being-edited today)})

(defn get-logs [_ _]
  {:http-xhrio {:method :get
                :uri "/logs"
                :format (ajax/edn-request-format)
                :response-format (ajax/edn-response-format)
                :on-success [:get-logs-success]
                :on-failure [:add-toast :error "Failed to fetch logs"]}})

(defn get-activities [_ _]
  {:http-xhrio {:method :get
                :uri "/activities"
                :format (ajax/edn-request-format)
                :response-format (ajax/edn-response-format)
                :on-success [:get-activities-success]
                :on-failure [:add-toast :error "Failed to fetch activities"]}})

(defn post-activity [{:keys [db]} [_ activity-type activity-name]]
  (let [date-being-edited (:date-being-edited db)]
    {:http-xhrio {:method :post
                  :uri "/activities"
                  :params {:name activity-name
                           :type activity-type}
                  :format (ajax/edn-request-format)
                  :response-format (ajax/edn-response-format)
                  :on-success [:post-activity-success]
                  :on-failure [:add-toast :error "Failed to save activity"]}}))

(defn get-activities-success [_ [_ activities]]
  {:dispatch-n (map #(vector :add-activity %) activities)})

(defn post-activity-success [_ [_ activity]]
  {:dispatch-n [[:add-activity activity]
                [:display-activity (:id activity)]]})

(defn add-activity [db [_ activity]]
  (update db :activities conj activity))

(defn add-activities [db [_ activities]]
  (->> (map #(vector nil %) activities)
       (reduce add-activity db)))

(defn display-activity [{db :db} [_ activity-id]]
  {:dispatch [:add-log activity-id (:date-being-edited db) 0]})

(defn add-log [db [_ activity-id date new-val]]
  (assoc-in db [:logs date activity-id] new-val))

(defn get-logs-success [_ [_ logs]]
  {:dispatch-n (map #(vector :add-log
                             (:activity-id %)
                             (:date %)
                             (:value %))
                    logs)})

(defn post-log-success [_ _]
  "NOP to avoid warnings in the console"
  nil)

(defn post-log [_ [_ activity-id date value]]
  {:http-xhrio {:method :post
                :uri "/logs"
                :params {:activity-id activity-id
                         :date date
                         :value value}
                :format (ajax/edn-request-format)
                :response-format (ajax/edn-response-format)
                :on-success [:post-log-success]
                :on-failure [:add-toast :error "Failed to update log"]}})


(defn update-log [_ [_ activity-id date new-val]]
  {:dispatch-n [[:add-log activity-id date new-val]
                [:post-log activity-id date new-val]]})

(defn add-toast [db [_ toast-type toast-content x]]
  (update db :toasts conj {:type toast-type
                           :content toast-content
                           :id (random-uuid)}))

(defn remove-toast [db [_ toast-id]]
  (update db
          :toasts
          (fn [toasts]
            (filter #(not= (:id %) toast-id)
                    toasts))))

(rf/reg-cofx
 :today
 (fn [cofx _]
   (assoc cofx :today (d/today!))))

(rf/reg-event-fx
 :initialise-db
 [(rf/inject-cofx :today)
  db/check-spec-interceptor]
 initialise-db)

(rf/reg-event-fx
 :get-activities
 db/check-spec-interceptor
 get-activities)

(rf/reg-event-fx
 :get-activities-success
 db/check-spec-interceptor
 get-activities-success)

(rf/reg-event-fx
 :post-activity
 db/check-spec-interceptor
 post-activity)

(rf/reg-event-fx
 :post-activity-success
 db/check-spec-interceptor
 post-activity-success)

(rf/reg-event-db
 :add-activity
 db/check-spec-interceptor
 add-activity)

(rf/reg-event-fx
 :display-activity
 db/check-spec-interceptor
 display-activity)

(rf/reg-event-fx
 :get-logs
 db/check-spec-interceptor
 get-logs)

(rf/reg-event-fx
 :get-logs-success
 db/check-spec-interceptor
 get-logs-success)

(rf/reg-event-db
 :add-log
 db/check-spec-interceptor
 add-log)

(rf/reg-event-fx
 :post-log
 db/check-spec-interceptor
 post-log)

(rf/reg-event-fx
 :post-log-success
 db/check-spec-interceptor
 post-log-success)

(rf/reg-event-fx
 :update-log
 db/check-spec-interceptor
 update-log)

(rf/reg-event-db
 :add-toast
 db/check-spec-interceptor
 add-toast)

(rf/reg-event-db
 :remove-toast
 db/check-spec-interceptor
 remove-toast)
