(ns daily-log.dates
  (:require [date-fns :as df]))

(defn date? [k]
  (-> k name df/parseISO df/isValid))

(defn- js-date->date [d]
  (-> d
      (df/formatISO (clj->js {:representation "date"}))
      keyword))

(defn today! []
  (js-date->date (js/Date.)))
