(ns daily-log.client.db
  (:require [re-frame.core :as rf]
            [cljs.spec.alpha :as s]
            [daily-log.client.dates :as d]))

(s/def ::activity-id keyword?)

(s/def ::activity-name string?)

(s/def ::activity-names (s/map-of ::activity-id ::activity-name))

(s/def ::activity-type #{:bool :int})

(s/def ::activity-types (s/map-of ::activity-id ::activity-type))

(s/def ::date d/date?)

(s/def ::date-being-edited ::date)

(s/def ::logs (s/map-of ::date
                        (s/map-of ::activity-id
                                  int?)))

(s/def ::toast-id uuid?)

(s/def ::toast-content string?)

(s/def ::toast-type #{:info :error})

(s/def ::toasts (s/coll-of (s/keys ::req-un [::toast-type ::toast-content ::toast-id])))

(s/def ::db (s/keys :req-un [::activity-names
                             ::activity-types
                             ::date-being-edited
                             ::logs
                             ::toasts]))

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
   :logs {}
   :toasts [{:toast-type :error
             :toast-id (random-uuid)
             :toast-content "An error toast"}
            {:toast-type :info
             :toast-id (random-uuid)
             :toast-content "An info toast"}]})
