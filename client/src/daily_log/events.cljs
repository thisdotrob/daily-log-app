(ns daily-log.events
  (:require [re-frame.core :as rf]
            [daily-log.dates :as d]
            [daily-log.db :as db]))

(defn initialise-db [{:keys [today]} _]
  (println "event: initialise-db")
  {:db {:date-being-edited today}})

(defn move-date-being-edited [db [_ direction]]
  (println "event: move-date-being-edited," direction)
  db)

(rf/reg-event-fx
 :initialise-db
 [(rf/inject-cofx :today)
  db/check-spec-interceptor]
 initialise-db)

(rf/reg-event-db
 :move-date-being-edited
 [db/check-spec-interceptor]
 move-date-being-edited)

(rf/reg-cofx
 :today
 (fn [cofx _]
   (assoc cofx :today (d/today!))))
