(ns daily-log.db
  (:require [re-frame.core :as rf]
            [cljs.spec.alpha :as s]
            [daily-log.dates :as d]))

(s/def ::activity-id keyword?)

(s/def ::activity-name string?)

(s/def ::activity-names (s/map-of ::activity-id ::activity-name))

(s/def ::activity-type #{:bool :float :int :percentage})

(s/def ::activity-types (s/map-of ::activity-id ::activity-type))

(s/def ::date d/date?)

(s/def ::date-being-edited ::date)

(s/def ::logs (s/map-of ::date
                        (s/map-of ::activity-id
                                  int?)))

(s/def ::db (s/keys :req-un [::activity-names
                             ::activity-types
                             ::date-being-edited
                             ::logs]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [db]
  (when-not (s/valid? ::db db)
    (throw (ex-info (str "spec check failed: " (s/explain-str ::db db)) {}))))

(def check-spec-interceptor (rf/after check-and-throw))

(def default-db
  {:date-being-edited nil
   :activity-names {}
   :activity-types {}
   :logs {}})
