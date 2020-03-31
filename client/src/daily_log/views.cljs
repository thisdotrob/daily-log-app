(ns daily-log.views
  (:require [re-frame.core :as rf]))

(defn app []
  (let [date-being-edited @(rf/subscribe [:date-being-edited])]
    [:div
     [:span "Hello world"]
     [:br]
     [:span {:on-click #(rf/dispatch [:move-date-being-edited :->])}
      date-being-edited]]))
