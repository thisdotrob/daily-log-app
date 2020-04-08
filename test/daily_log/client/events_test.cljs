(ns daily-log.events-test
  (:require [cljs.test :refer (deftest is testing)]
            [daily-log.client.events :as sut]))

(deftest add-activity
  (let [id :ee58d735-4c66-4565-9113-e50872a5a2ed
        date-being-edited :2020-04-02
        activity-type :bool
        activity-name "Activity X"
        cofx {:db {:date-being-edited date-being-edited}
              :id id}]
    (testing "It adds a log for the date in focus to ensure the new activity is in the displayed date range"
      (is (= 0
             (-> (sut/add-activity cofx [nil activity-type activity-name])
                 :db
                 :logs
                 date-being-edited
                 id))))
    (testing "It adds an entry in :activity-types"
      (is (= activity-type
             (-> (sut/add-activity cofx [nil activity-type activity-name])
                 :db
                 :activity-types
                 id))))
    (testing "It adds an entry in :activity-names"
      (is (= activity-name
             (-> (sut/add-activity cofx [nil activity-type activity-name])
                 :db
                 :activity-names
                 id))))))

(deftest inc-log
  (let [date :2020-04-03
        activity-id :ee58d735-4c66-4565-9113-e50872a5a2ed
        bool-activity-id :6906a4a9-149b-4a4d-9507-8772501ebbb5
        db {:activity-types {activity-id :int
                             bool-activity-id :bool}
            :logs {date {activity-id 0
                         bool-activity-id 0}}}]
    (testing "It increments the value of an non :bool log"
      (is (= 1
             (-> (sut/inc-log db [nil activity-id date])
                 :logs
                 date
                 activity-id)))
      (is (= 2
             (-> (sut/inc-log db [nil activity-id date])
                 (sut/inc-log [nil activity-id date])
                 :logs
                 date
                 activity-id))))
    (testing "It doesn't increment the value of a :bool log by more than 1"
      (is (= 1
             (-> (sut/inc-log db [nil bool-activity-id date])
                 :logs
                 date
                 bool-activity-id)))
      (is (= 1
             (-> (sut/inc-log db [nil bool-activity-id date])
                 (sut/inc-log [nil bool-activity-id date])
                 :logs
                 date
                 bool-activity-id))))))

(deftest reset-log
  (let [date :2020-04-03
        activity-id :ee58d735-4c66-4565-9113-e50872a5a2ed
        db {:logs {date {activity-id 20}}}]
    (testing "It resets the value of the log to zero"
      (is (zero? (-> (sut/reset-log db [nil activity-id date])
                     :logs
                     date
                     activity-id))))))
