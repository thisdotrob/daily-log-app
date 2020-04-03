(ns daily-log.events
  (:require [re-frame.core :as rf]
            [daily-log.dates :as d]
            [daily-log.db :as db]))

(defn initialise-db [{:keys [today]} _]
  {:db (assoc db/default-db
              :date-being-edited today)})

(defn add-activity [{:keys [db id]} [_ activity-type activity-name]]
  (let [date-being-edited (:date-being-edited db)]
    {:db (-> db
             (assoc-in [:logs date-being-edited id] 0)
             (assoc-in [:activity-types id] activity-type)
             (assoc-in [:activity-names id] activity-name))}))

(defn inc-log [db [_ date activity-id n]]
  (let [activity-type (get-in db [:activity-types activity-id])]
    (if (= :bool activity-type)
      (assoc-in db [:logs date activity-id] 1)
      (update-in db [:logs date activity-id] (partial + n)))))

(defn dec-log [db [_ date activity-id n]]
  (println "dec-log" n)
  (update-in db [:logs date activity-id] #(let [new-val (- % n)]
                                            (if (< new-val 0)
                                              0
                                              new-val))))

(rf/reg-cofx
 :today
 (fn [cofx _]
   (assoc cofx :today (d/today!))))

(rf/reg-cofx
 :random-id
 (fn [cofx _]
   (assoc cofx :id (-> (random-uuid) str keyword))))

(rf/reg-event-fx
 :initialise-db
 [(rf/inject-cofx :today)
  db/check-spec-interceptor]
 initialise-db)

(rf/reg-event-fx
 :add-activity
 [(rf/inject-cofx :random-id)
  db/check-spec-interceptor]
 add-activity)

(rf/reg-event-db
 :inc-log
 db/check-spec-interceptor
 inc-log)

(rf/reg-event-db
 :dec-log
 db/check-spec-interceptor
 dec-log)
