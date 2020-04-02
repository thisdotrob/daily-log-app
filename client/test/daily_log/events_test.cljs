(ns daily-log.events-test
  (:require [cljs.test :refer (deftest is testing)]
            [daily-log.events :as sut]))

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

