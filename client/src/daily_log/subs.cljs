(ns daily-log.subs
  (:require [re-frame.core :as rf]
            [daily-log.dates :as d]))

(defn visible-dates [db _]
  (map #(d/add-days (:date-being-edited db) %)
       (range -2 3)))

(defn visible-activity-ids [[logs visible-dates] _]
  (->> visible-dates
       (select-keys logs)
       vals
       (mapcat keys)
       set))

(defn activity-name [db [_ activity-id]]
  (get-in db [:activity-names activity-id]))

(defn activity-type [db [_ activity-id]]
  (get-in db [:activity-types activity-id]))

(defn log [db [_ activity-id date]]
  (or (get-in db [:logs date activity-id])
      0))

(defn logs [db _]
  (:logs db))

(rf/reg-sub :visible-dates visible-dates)
(rf/reg-sub :visible-activity-ids
            :<- [:logs]
            :<- [:visible-dates]
            visible-activity-ids)
(rf/reg-sub :activity-name activity-name)
(rf/reg-sub :activity-type activity-type)
(rf/reg-sub :log log)
(rf/reg-sub :logs logs)
