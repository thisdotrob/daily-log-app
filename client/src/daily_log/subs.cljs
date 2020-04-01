(ns daily-log.subs
  (:require [re-frame.core :as rf]))

(defn visible-dates [db [_ col-count]]
  [:2020-03-31 :2020-04-01 :2020-04-02])

(defn visible-activity-ids [db _]
  [:a :b :c])

(defn activity-name [db [_ activity-id]]
  "Activity X")

(defn log [db [_ activity-id date]]
  {:type :int
   :value 2})

(rf/reg-sub :visible-dates visible-dates)
(rf/reg-sub :visible-activity-ids visible-activity-ids)
(rf/reg-sub :activity-name activity-name)
(rf/reg-sub :log log)
