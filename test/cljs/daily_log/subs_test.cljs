(ns daily-log.subs-test
  (:require [cljs.test :refer (deftest is testing)]
            [daily-log.subs :as sut]))

(deftest visible-dates
  (testing "returns two sequential dates either side of :date-being-edited"
    (let [db {:date-being-edited :2020-04-01}
          event nil]
      (is (= [:2020-03-30 :2020-03-31 :2020-04-01 :2020-04-02 :2020-04-03]
             (sut/visible-dates db [event]))))))

(deftest visible-activity-ids
  (testing "returns empty set when there are no logs for the visible dates"
    (let [logs {}
          visible-dates [:2020-03-31 :2020-04-01 :2020-04-02]]
      (is (= #{}
             (sut/visible-activity-ids [logs visible-dates] nil)))))
  (testing "returns the single activity ID when there is a log for that activity on one of the visible dates"
    (let [logs {:2020-04-01 {:a 0}}
          visible-dates [:2020-03-31 :2020-04-01 :2020-04-02]]
      (is (= #{:a}
             (sut/visible-activity-ids [logs visible-dates] nil)))))
  (testing "returns both activity IDs when there are logs for two activities on separate visible days"
    (let [logs {:2020-04-01 {:a 2}
                :2020-03-31 {:b 10}}
          visible-dates [:2020-03-31 :2020-04-01 :2020-04-02]]
      (is (= #{:a :b}
             (sut/visible-activity-ids [logs visible-dates] nil)))))
  (testing "returns both activity IDs when there are logs for two activities on the same visible day"
    (let [logs {:2020-04-01 {:a 2
                             :b 10}}
          visible-dates [:2020-03-31 :2020-04-01 :2020-04-02]]
      (is (= #{:a :b}
             (sut/visible-activity-ids [logs visible-dates] nil))))))

(deftest activity-name
  (testing "returns the name of the activity when found"
    (let [db {:activities [{:id :a :type :bool :name "Activity A"}
                           {:id :b :type :int :name "Activity B"}]}
          activity-id :a]
      (is (= "Activity A"
             (sut/activity-name db [nil activity-id]))))))

(deftest activity-type
  (testing "returns the type of the activity when found"
    (let [db {:activities [{:id :a :type :bool :name "Activity A"}
                           {:id :b :type :int :name "Activity B"}]}
          activity-id :a]
      (is (= :bool
             (sut/activity-type db [nil activity-id]))))))

(deftest log
  (testing "returns the log's value when found"
    (let [db {:logs {:2020-04-01 {:a 1}}}
          event nil
          activity-id :a
          date :2020-04-01]
      (is (= 1
             (sut/log db [event activity-id date])))))
  (testing "returns 0 when date but not activity is found"
    (let [db {:logs {:2020-04-01 {:a 1}}}
          event nil
          activity-id :b
          date :2020-04-01]
      (is (= 0
             (sut/log db [event activity-id date])))))
  (testing "returns 0 when date is not found"
    (let [db {:logs {:2020-04-01 {:a 1}}}
          event nil
          activity-id :a
          date :2020-04-02]
      (is (= 0
             (sut/log db [event activity-id date]))))))

(deftest has-matches?
  (testing "Returns false if all elements in :search-result have :type :non-match"
    (is (false? (sut/has-matches? [{:type :non-match}])))
    (is (false? (sut/has-matches? [{:type :non-match} {:type :non-match}]))))
  (testing "Returns true if at least one element in :search-result has :type :match"
    (is (true? (sut/has-matches? [{:type :match}])))
    (is (true? (sut/has-matches? [{:type :match} {:type :match}])))
    (is (true? (sut/has-matches? [{:type :match} {:type :non-match}])))))

(deftest search-activities
  (testing "handles empty string val"
    (let [db {:activities [{:name "Played badminton" :id :1 :type :bool}
                           {:name "Life admin" :id :2 :type :bool}
                           {:name "Coffees (cups)" :id :3 :type :int}]}
          search-val ""]
      (is (= []
           (sut/search-activities db [nil search-val])))))
  (testing "returns activities that have a substring match in their :name"
    (let [db {:activities [{:name "Played badminton" :id :1 :type :bool}
                           {:name "Life admin" :id :2 :type :bool}
                           {:name "Coffees (cups)" :id :3 :type :int}]}
          search-val "admin"]
      (is (= #{:1 :2}
             (->> (sut/search-activities db [nil search-val])
                  (map :id)
                  set)))
      (is (= [{:type :non-match
               :val "Played b"}
              {:type :match
               :val "admin"}
              {:type :non-match
               :val "ton"}]
             (->> (sut/search-activities db [nil search-val])
                  (filter #(= :1 (:id %)))
                  first
                  :search-result)))
      (is (= [{:type :non-match
               :val "Life "}
              {:type :match
               :val "admin"}]
             (->> (sut/search-activities db [nil search-val])
                  (filter #(= :2 (:id %)))
                  first
                  :search-result))))))

(deftest search
  (testing "handles zero matches"
    (is (= [{:val "ddd" :type :non-match}]
           (sut/search "ddd" "abc"))))
  (testing "handles a single match"
    (is (= [{:val "abc" :type :match}]
           (sut/search "abc" "abc"))))
  (testing "handles multiple matches"
    (is (= [{:val "abc" :type :match}
            {:val "abc" :type :match}]
           (sut/search "abcabc" "abc"))))
  (testing "handles non-matching chars at the start"
    (is (= [{:val "ddd" :type :non-match}
            {:val "abc" :type :match}]
           (sut/search "dddabc" "abc"))))
  (testing "handles non-matching chars at the end"
    (is (= [{:val "abc" :type :match}
            {:val "ddd" :type :non-match}]
           (sut/search "abcddd" "abc"))))
  (testing "handles non-matching chars at start and end"
    (is (= [{:val "ddd" :type :non-match}
            {:val "abc" :type :match}
            {:val "ddd" :type :non-match}]
           (sut/search "dddabcddd" "abc"))))
  (testing "handles non-matching chars at start and end and in middle"
    (is (= [{:val "ddd" :type :non-match}
            {:val "abc" :type :match}
            {:val "ddd" :type :non-match}
            {:val "abc" :type :match}
            {:val "ddd" :type :non-match}]
           (sut/search "dddabcdddabcddd" "abc"))))
  (testing "searches case insensitive, maintains case in output"
    (is (= [{:val "DDD" :type :non-match}
            {:val "aBc" :type :match}
            {:val "ABc" :type :match}
            {:val "ddD" :type :non-match}]
           (sut/search "DDDaBcABcddD" "abc")))))
