(ns daily-log.views
  (:require [re-frame.core :as rf]))

(defn log-val->display-str [type val]
  (cond
    (= :bool type) (if (= 1 val) "Yes" "No")
    (= :percentage type) (str val "%")
    (= :float type) (str (/ val 1000))
    :else (str val)))

(defn activity-cell [activity-id date]
  (let [log @(rf/subscribe [:log activity-id date])
        t @(rf/subscribe [:activity-type activity-id])]
    [:div.col.s2.center-align (log-val->display-str t log)]))

(defn activity-row [activity-id]
  (let [activity-name @(rf/subscribe [:activity-name activity-id])
        visible-dates @(rf/subscribe [:visible-dates])]
    [:div.row
     [:div.col.s2 activity-name]
     (for [d visible-dates]
       [activity-cell activity-id d])]))

(defn app []
  (let [visible-dates @(rf/subscribe [:visible-dates])
        activity-ids @(rf/subscribe [:visible-activity-ids])]
    [:div.container.grey.lighten-3.z-depth-1 {:style {:margin-top "10px"}}
     [:div.row {:style {:margin-bottom 0}}
      [:div.row.teal.lighten-2 {:style {:margin-bottom 0}}
       [:div.col.s2 "Activity"]
       (for [d visible-dates]
         [:div.col.s2.center-align d])]]
     [:div.row {:style {:padding-top "10px"}}
      (for [id activity-ids]
        [activity-row id])]
     [:div.row
      [:div.col.input-field.s8
        [:i.material-icons.prefix "playlist_add"]
        [:input.validate {:id "new_activity_name" :type "text"}]
        [:label {:for "new_activity_name"} "Activity Name"]]
      [:div.col.input-field.s4
       [:select {:default-value :prompt}
        [:option {:value :prompt :disabled ""} "Choose a type..."]
        [:option {:value :bool} "Yes/No"]
        [:option {:value :int} "Whole number"]
        [:option {:value :float} "Decimal number"]
        [:option {:value :percentage} "Percentage"]]
       [:label "Activity Type"]]]]))
