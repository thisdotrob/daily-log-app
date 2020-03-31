(ns daily-log.dates
  (:require [date-fns :as df]))

(defn date-ISO-str? [s]
  (-> s df/parseISO df/isValid))

(defn date->ISO-str [d]
  (df/formatISO d (clj->js {:representation "date"})))

(defn today! []
  (-> (js/Date.) date->ISO-str))
