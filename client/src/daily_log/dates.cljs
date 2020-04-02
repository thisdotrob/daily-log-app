(ns daily-log.dates
  (:require [date-fns :as df]))

(defn- date->js-date [d]
  (-> d name df/parseISO))

(defn- js-date->date [d]
  (-> d
      (df/formatISO (clj->js {:representation "date"}))
      keyword))

(defn date? [k]
  (-> k name df/parseISO df/isValid))

(defn today! []
  (js-date->date (js/Date.)))

(defn add-days [d n]
  (-> d
      date->js-date
      (df/addDays n)
      js-date->date))

(defn ->DD-str [d]
  (-> d
      date->js-date
      (df/format "d")))

(defn ->MMM-str [d]
  (-> d
      date->js-date
      (df/format "MMM")))

(defn ->dow-str [d]
  (-> d
      date->js-date
      (df/format "EEE")))
