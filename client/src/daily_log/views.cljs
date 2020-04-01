(ns daily-log.views
  (:require [re-frame.core :as rf]))

(defn table-header []
  (let [col-count 3
        visible-dates @(rf/subscribe [:visible-dates col-count])]
    [:div.row
     [:div.col.s3 "Activity"]
     [:div.col.s3 (nth visible-dates 0)]
     [:div.col.s3 (nth visible-dates 1)]
     [:div.col.s3 (nth visible-dates 2)]]))

(defn activity-cell [activity-id date]
  (let [log @(rf/subscribe [:log activity-id date])]
    [:div.col.s3 (:value log)]))

(defn activity-row [activity-id]
  (let [activity-name @(rf/subscribe [:activity-name activity-id])
        dates @(rf/subscribe [:visible-dates])]
    [:div
     [:div.col.s3 activity-name]
     (for [d dates]
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
