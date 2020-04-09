(ns daily-log.client.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [ajax.edn :as ajax]
            [daily-log.client.dates :as d]
            [daily-log.client.db :as db]))

(defn initialise-db [{:keys [today]} _]
  {:db (assoc db/default-db
              :date-being-edited today)})

(defn get-logs [_ _]
  {:http-xhrio {:method :get
                :uri "/logs"
                :format (ajax/edn-request-format)
                :response-format (ajax/edn-response-format)
                :on-success [:add-logs]
                :on-failure [:add-toast :error "Failed to fetch logs"]}})

(defn get-activities [_ _]
  {:http-xhrio {:method :get
                :uri "/activities"
                :format (ajax/edn-request-format)
                :response-format (ajax/edn-response-format)
                :on-success [:add-activities]
                :on-failure [:add-toast :error "Failed to fetch activities"]}})

(defn post-activity [{:keys [db]} [_ activity-type activity-name]]
  (let [date-being-edited (:date-being-edited db)]
    {:http-xhrio {:method :post
                  :uri "/activities"
                  :params {:name activity-name
                           :type activity-type}
                  :format (ajax/edn-request-format)
                  :response-format (ajax/edn-response-format)
                  :on-success [:add-activity]
                  :on-failure [:add-toast :error "Failed to save activity"]}}))

(defn add-activity [{:keys [date-being-edited] :as db}
                    [_ {id :id :as activity}]]
  (-> db
      (assoc-in [:logs date-being-edited id] 0)
      (assoc-in [:activity-types id] (:type activity))
      (assoc-in [:activity-names id] (:name activity))))

(defn add-activities [db [_ activities]]
  (->> (map #(vector nil %) activities)
       (reduce add-activity db)))

(defn add-log [db [_ activity-id date new-val]]
  (assoc-in db [:logs date activity-id] new-val))

(defn add-logs [db [_ logs]]
  (->> (map #(vector nil (:activity-id %) (:date %) (:value %))
            logs)
       (reduce add-log db)))

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

(defn add-toast [db [_ toast-type toast-content]]
  (update db :toasts conj {:toast-type toast-type
                           :toast-content toast-content
                           :toast-id (random-uuid)}))

(defn remove-toast [db [_ toast-id]]
  (update db
          :toasts
          (fn [toasts]
            (filter #(not= (:toast-id %) toast-id)
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
 :post-activity
 db/check-spec-interceptor
 post-activity)

(rf/reg-event-db
 :add-activity
 db/check-spec-interceptor
 add-activity)

(rf/reg-event-db
 :add-activities
 db/check-spec-interceptor
 add-activities)

(rf/reg-event-fx
 :get-logs
 db/check-spec-interceptor
 get-logs)

(rf/reg-event-db
 :add-log
 db/check-spec-interceptor
 add-log)

(rf/reg-event-db
 :add-logs
 db/check-spec-interceptor
 add-logs)

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
