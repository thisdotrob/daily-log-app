(ns daily-log.db
  (:require [re-frame.core :as rf]
            [cljs.spec.alpha :as s]
            [daily-log.dates :as d]))

(s/def ::activity-id keyword?)

(s/def ::activity-name string?)

(s/def ::activity-names (s/map-of ::activity-id ::activity-name))

(s/def ::date d/date?)

(s/def ::date-being-edited ::date)

(s/def ::type #{:bool :float :int :percentage})

(s/def ::value int?)

(s/def ::log (s/keys :req-un [::type ::value]))

(s/def ::logs (s/map-of ::date
                        (s/map-of ::activity-id
                                  ::log)))

(s/def ::db (s/keys :req-un [::activity-names
                             ::date-being-edited
                             ::logs]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [db]
  (when-not (s/valid? ::db db)
    (throw (ex-info (str "spec check failed: " (s/explain-str ::db db)) {}))))

(def check-spec-interceptor (rf/after check-and-throw))

(def default-db
  {:date-being-edited :2020-04-01
   :activity-names {:a "Activity A"
                    :b "Activity B"
                    :c "Activity C"}
   :logs {:2020-03-31 {:a {:type :bool
                           :value 1}
                       :b {:type :float
                           :value 123}
                       :c {:type :int
                           :value 3}}
          :2020-04-01 {:a {:type :bool
                           :value 0}
                       :b {:type :float
                           :value 456}
                       :c {:type :int
                           :value 5}}
          :2020-04-02 {:a {:type :bool
                           :value 0}
                       :b {:type :float
                           :value 0}
                       :c {:type :int
                           :value 0}}}})
