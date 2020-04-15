(ns ^:figwheel-hooks daily-log.core
  (:require [reagent.dom :as r]
            [re-frame.core :as rf :refer [dispatch dispatch-sync]]
            [devtools.core :as devtools]
            [daily-log.events] ;; These two are only required to make the compiler
            [daily-log.subs]   ;; load them
            [daily-log.views]))

(devtools/install!)       ;; https://github.com/binaryage/cljs-devtools
(enable-console-print!)   ;; so that println writes to `console.log`

(defn render []
  (dispatch-sync [:initialise-db])
  (dispatch [:get-activities])
  (dispatch [:get-logs])
  (r/render [daily-log.views/app]
            (.getElementById js/document "app")))

(defn ^:after-load clear-cache-and-render! []
  (rf/clear-subscription-cache!)
  (render))

(defonce start-up (do (render) true))
