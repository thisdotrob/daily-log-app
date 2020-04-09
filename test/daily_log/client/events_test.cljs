(ns daily-log.events-test
  (:require [cljs.test :refer (deftest is testing)]
            [ajax.edn :as ajax]
            [daily-log.client.events :as sut]))

(deftest get-logs
  (testing "includes the expected :http-xhrio effect in the effects map"
    (is (= {:method :get
            :uri "/logs"
            :response-format (ajax/edn-response-format)
            :format (ajax/edn-request-format)
            :on-success [:get-logs-success]
            :on-failure [:add-toast :error "Failed to fetch logs"]}
           (-> (sut/get-logs {} [nil])
               :http-xhrio)))))

(deftest get-logs-success
  (testing "dispatches :add-log for each log"
    (let [logs [{:activity-id :1 :date :2020-04-09 :value 1}
                {:activity-id :2 :date :2020-04-10 :value 2}]]
      (is (= {:dispatch-n [[:add-log :1 :2020-04-09 1]
                           [:add-log :2 :2020-04-10 2]]}
             (sut/get-logs-success nil [nil logs]))))))

(deftest get-activities
  (testing "includes the expected :http-xhrio effect in the effects map"
    (is (= {:method :get
            :uri "/activities"
            :response-format (ajax/edn-response-format)
            :format (ajax/edn-request-format)
            :on-success [:get-activities-success]
            :on-failure [:add-toast :error "Failed to fetch activities"]}
           (-> (sut/get-activities {} [nil])
               :http-xhrio)))))

(deftest get-activities-success
  (testing "dispatches :add-activity for each activity"
    (let [activities [:activity-a :activity-b]]
      (is (= {:dispatch-n [[:add-activity :activity-a]
                           [:add-activity :activity-b]]}
             (sut/get-activities-success nil [nil activities]))))))

(deftest post-activity
  (testing "includes the expected :http-xhrio effect in the effects map"
    (let [date-being-edited :2020-04-02
          activity-type :bool
          activity-name "Activity |"
          cofx {:db {:date-being-edited date-being-edited}}]
      (is (= {:method :post
              :uri "/activities"
              :params {:name activity-name
                       :type activity-type}
              :response-format (ajax/edn-response-format)
              :format (ajax/edn-request-format)
              :on-success [:post-activity-success]
              :on-failure [:add-toast :error "Failed to save activity"]}
             (-> (sut/post-activity cofx [nil activity-type activity-name])
                 :http-xhrio))))))

(deftest post-activity-success
  (testing "dispatches :add-activity and :display-display activity"
    (let [activity {:id :1 :type :int :name "Activity X"}]
      (is (= {:dispatch-n [[:add-activity activity]
                           [:display-activity activity]]}
             (sut/post-activity-success nil [nil activity]))))))

(deftest add-activity
  (let [db {:activities [{:name "existing toast"
                          :type :bool
                          :id :1}]}
        activity {:id :2
                  :type :bool
                  :name "Activity to add"}
        updated-activities (->> (sut/add-activity db [nil activity])
                                :activities)]
    (testing "appends the activity to the list in the db"
      (is (= 2 (count updated-activities)))
      (is (= #{:1 :2}
             (-> (map :id updated-activities)
                 set))))))

(deftest add-activities
  (let [db {:activities [{:name "existing toast"
                          :type :bool
                          :id :1}]}
        new-activities [{:id :2
                         :type :bool
                         :name "Activity to add 1"}
                        {:id :3
                         :type :int
                         :name "Activity to add 2"}]
        updated-activities (->> (sut/add-activities db [nil new-activities])
                                :activities)]
    (testing "appends the activity to the list in the db"
      (is (= 3 (count updated-activities)))
      (is (= #{:1 :2 :3}
             (-> (map :id updated-activities)
                 set))))))

(deftest display-activity
  (testing "It adds a log for the date in focus to ensure the new activity is in the displayed date range"
    (let [date-being-edited :2020-04-09
          activity-id :1
          db {:date-being-edited date-being-edited}]
      (is (= {:dispatch [:add-log activity-id date-being-edited 0]}
             (sut/display-activity db [nil activity-id]))))))

(deftest add-log
  (testing "It updates existing logs"
    (let [date :2020-04-08
          activity-id :111
          db {:logs {date {activity-id 0}}}
          new-val 1]
      (is (= new-val
             (-> (sut/add-log db [nil activity-id date new-val])
                 :logs
                 date
                 activity-id)))))
  (testing "It inserts new logs"
    (let [date :2020-04-08
          activity-id :111
          db {:logs {}}
          new-val 3]
      (is (= new-val
             (-> (sut/add-log db [nil activity-id date new-val])
                 :logs
                 date
                 activity-id))))))

(deftest post-log
  (testing "includes the expected :http-xhrio effect in the effects map"
    (let [date :2020-04-08
          activity-id :111
          db {}
          value 3]
      (is (= {:method :post
              :uri "/logs"
              :params {:activity-id activity-id
                       :date date
                       :value value}
              :format (ajax/edn-request-format)
              :response-format (ajax/edn-response-format)
              :on-success [:post-log-success]
              :on-failure [:add-toast :error "Failed to update log"]}
             (-> (sut/post-log db [nil activity-id date value])
                 :http-xhrio))))))

(deftest add-toast
  (let [existing-toast-id (random-uuid)
        db {:toasts [{:content "existing toast"
                      :type :info
                      :id existing-toast-id}]}
        toast-content "message"
        toast-type :error
        updated-toasts (->> (sut/add-toast db [nil toast-type toast-content])
                            :toasts)]
    (testing "appends the toast to the list in the db"
      (is (= 2 (count updated-toasts))))
    (let [new-toast (->> updated-toasts
                         (filter #(not= existing-toast-id (:id %)))
                         first)]
      (testing "gives the new toast an id"
        (is (uuid? (:id new-toast))))
      (testing "sets the correct type and content on the new toast"
        (is (= toast-content
               (:content new-toast)))
        (is (= toast-type
               (:type new-toast)))))))

(deftest remove-toast
  (let [toast-to-rm-id (random-uuid)
        toast-to-keep-id (random-uuid)
        db {:toasts [{:id toast-to-keep-id
                      :type :info
                      :content "this toast should remain"}
                     {:id toast-to-rm-id
                      :type :error
                      :content "this toast should be removed"}]}]
    (is (= 1
           (->> (sut/remove-toast db [nil toast-to-rm-id])
                :toasts
                count)))
    (is (= toast-to-keep-id
           (->> (sut/remove-toast db [nil toast-to-rm-id])
                :toasts
                first
                :id)))))
