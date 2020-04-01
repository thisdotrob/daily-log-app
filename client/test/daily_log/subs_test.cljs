(ns daily-log.subs-test
  (:require [cljs.test :refer (deftest is testing)]
            [daily-log.subs :as sut]))

(deftest visible-dates
  (testing "returns :date-being-edited if col count is 1"
    (let [date-being-edited :2020-04-01
          db {:date-being-edited date-being-edited}
          event nil
          col-count 1]
      (is (= [date-being-edited]
             (sut/visible-dates db [event col-count])))))
  (testing "returns a single sequential date either side of :date-being-edited if col count is 3"
    (let [db {:date-being-edited :2020-04-01}
          event nil
          col-count 3]
      (is (= [:2020-03-31 :2020-04-01 :2020-04-02]
             (sut/visible-dates db [event col-count])))))
  (testing "returns two sequential dates either side of :date-being-edited if col count is 5"
    (let [db {:date-being-edited :2020-04-01}
          event nil
          col-count 5]
      (is (= [:2020-03-30 :2020-03-31 :2020-04-01 :2020-04-02 :2020-04-03]
             (sut/visible-dates db [event col-count]))))))

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
    (let [db {:activity-names {:a "Activity A"
                               :b "Activity B"}}
          event nil
          activity-id :a]
      (is (= "Activity A"
             (sut/activity-name db [event activity-id])))))
  (testing "returns nil when not found"
    (let [db {:activity-names {:a "Activity A"
                               :b "Activity B"}}
          event nil
          activity-id :c]
      (is (nil? (sut/activity-name db [event activity-id]))))))

(deftest activity-type
  (testing "returns the type of the activity when found"
    (let [db {:activity-types {:a :bool
                               :b :int}}
          event nil
          activity-id :a]
      (is (= :bool
             (sut/activity-type db [event activity-id])))))
  (testing "returns nil when not found"
    (let [db {:activity-types {:a :bool
                               :b :int}}
          event nil
          activity-id :c]
      (is (nil? (sut/activity-type db [event activity-id]))))))

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
