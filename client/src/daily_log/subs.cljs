(ns daily-log.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :date-being-edited
 (fn [db _] (:date-being-edited db)))

