(ns daily-log.views
  (:require [re-frame.core :as rf]))

(defn table-header []
  (let [visible-dates @(rf/subscribe [:visible-dates])]
    [:div.row
     [:div.col.s2 "Activity"]
     (for [d visible-dates]
       [:div.col.s2 d])]))

(defn log-val->display-str [type val]
  (cond
    (= :bool type) (if (= 1 val) "Yes" "No")
    (= :percentage type) (str val "%")
    (= :float type) (str (/ val 1000))
    :else (str val)))

(defn activity-cell [activity-id date]
  (let [log @(rf/subscribe [:log activity-id date])
        t @(rf/subscribe [:activity-type activity-id])]
    [:div.col.s2 (log-val->display-str t log)]))

(defn activity-row [activity-id]
  (let [activity-name @(rf/subscribe [:activity-name activity-id])
        visible-dates @(rf/subscribe [:visible-dates])]
    [:div
     [:div.col.s2 activity-name]
     (for [d visible-dates]
       [activity-cell activity-id d])]))

(defn table-body []
  (let [activity-ids @(rf/subscribe [:visible-activity-ids])]
    [:div.row {:style {:overflow "scroll" :height "200px"}}
     (for [id activity-ids]
       [activity-row id])]))

(defn app []
  [:div.container
   [table-header]
   [table-body]])
