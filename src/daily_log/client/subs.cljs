(ns daily-log.client.subs
  (:require [re-frame.core :as rf]
            [daily-log.client.dates :as d]))

(defn date-being-edited [db _]
  (:date-being-edited db))

(defn visible-dates [db _]
  (map #(d/add-days (:date-being-edited db) %)
       (range -2 3)))

(defn visible-activity-ids [[logs visible-dates] _]
  (->> visible-dates
       (select-keys logs)
       vals
       (mapcat keys)
       set))

(defn activity-name [db [_ activity-id]]
  (->> db
       :activities
       (filter #(= activity-id (:id %)))
       first
       :name))

(defn activity-type [db [_ activity-id]]
  (->> db
      :activities
      (filter #(= activity-id (:id %)))
      first
      :type))

(defn log [db [_ activity-id date]]
  (or (get-in db [:logs date activity-id])
      0))

(defn logs [db _]
  (:logs db))

(defn toasts [db _]
  (:toasts db))

(defn search [search-text search-val]
  (let [p (str "(?i)(.*)(" search-val ")(.*)")
        r (re-pattern p)]
    (loop [s search-text
           result []]
      (let [[_ rest match non-match] (re-matches r s)]
        (cond
          (= "" s) result
          (nil? match) (cons {:type :non-match
                              :val s}
                             result)
          (not= "" non-match) (recur (str rest match)
                                     (cons {:type :non-match
                                            :val non-match}
                                           result))
          :else (recur rest
                       (cons {:type :match
                              :val match}
                             result)))))))

(defn has-matches? [search-result]
  (-> :type
      (map search-result)
      set
      (contains? :match)))

(defn search-activities [{:keys [activities] :as db} [_ search-val]]
  (if (= "" search-val)
    []
    (let [activities (map #(assoc % :search-result (search (:name %)
                                                           search-val))
                          activities)]
      (filter #(has-matches? (:search-result %))
              activities))))

(rf/reg-sub :date-being-edited date-being-edited)
(rf/reg-sub :visible-dates visible-dates)
(rf/reg-sub :visible-activity-ids
            :<- [:logs]
            :<- [:visible-dates]
            visible-activity-ids)
(rf/reg-sub :activity-name activity-name)
(rf/reg-sub :activity-type activity-type)
(rf/reg-sub :log log)
(rf/reg-sub :logs logs)
(rf/reg-sub :toasts toasts)
(rf/reg-sub :search-activities search-activities)
