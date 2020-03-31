(ns daily-log.db
  (:require [re-frame.core :as rf]
            [cljs.spec.alpha :as s]
            [daily-log.dates :as d]))


(s/def ::date (s/and string? d/date-ISO-str?))
(s/def ::date-being-edited ::date)

(s/def ::db (s/keys :req-un [::date-being-edited]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [db]
  (when-not (s/valid? ::db db)
    (throw (ex-info (str "spec check failed: " (s/explain-str ::db db)) {}))))

(def check-spec-interceptor (rf/after check-and-throw))
