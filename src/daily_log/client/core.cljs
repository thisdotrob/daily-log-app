(ns ^:figwheel-hooks daily-log.client.core
  (:require [reagent.dom :as r]
            [re-frame.core :as rf :refer [dispatch dispatch-sync]]
            [devtools.core :as devtools]
            [daily-log.client.events] ;; These two are only required to make the compiler
            [daily-log.client.subs]   ;; load them
            [daily-log.client.views]))

(devtools/install!)       ;; https://github.com/binaryage/cljs-devtools
(enable-console-print!)   ;; so that println writes to `console.log`

(defn render []
  (dispatch-sync [:initialise-db])
  (dispatch [:get-activities])
  (dispatch [:get-logs])
  (r/render [daily-log.client.views/app]
            (.getElementById js/document "app")))

(defn ^:after-load clear-cache-and-render! []
  (rf/clear-subscription-cache!)
  (render))

(defonce start-up (do (render) true))
