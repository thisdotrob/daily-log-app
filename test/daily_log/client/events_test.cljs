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
            :on-success [:add-logs]
            :on-failure [:add-toast :error "Failed to fetch logs"]}
           (-> (sut/get-logs {} [nil])
               :http-xhrio)))))

(deftest get-activities
  (testing "includes the expected :http-xhrio effect in the effects map"
    (is (= {:method :get
            :uri "/activities"
            :response-format (ajax/edn-response-format)
            :format (ajax/edn-request-format)
            :on-success [:add-activities]
            :on-failure [:add-toast :error "Failed to fetch activities"]}
           (-> (sut/get-activities {} [nil])
               :http-xhrio)))))

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
              :on-success [:add-activity]
              :on-failure [:add-toast :error "Failed to save activity"]}
             (-> (sut/post-activity cofx [nil activity-type activity-name])
                 :http-xhrio))))))

(deftest add-activity
  (let [date-being-edited :2020-04-02
        activity-type :bool
        activity-name "Activity X"
        db {:date-being-edited date-being-edited}
        id :100
        activity {:id id
                  :type activity-type
                  :name activity-name}]
    (testing "It adds a log for the date in focus to ensure the new activity is in the displayed date range"
      (is (= 0
             (-> (sut/add-activity db [nil activity])
                 :logs
                 date-being-edited
                 id))))
    (testing "It adds an entry in :activity-types"
      (is (= activity-type
             (-> (sut/add-activity db [nil activity])
                 :activity-types
                 id))))
    (testing "It adds an entry in :activity-names"
      (is (= activity-name
             (-> (sut/add-activity db [nil activity])
                 :activity-names
                 id))))))

(deftest add-activities
  (let [date-being-edited :2020-04-02
        activity-type-a :bool
        activity-type-b :int
        activity-name-a "Activity A"
        activity-name-b "Activity B"
        activity-id-a :100
        activity-id-b :101
        db {:date-being-edited date-being-edited}
        activities [{:id activity-id-a
                     :type activity-type-a
                     :name activity-name-a}
                    {:id activity-id-b
                     :type activity-type-b
                     :name activity-name-b}]]
    (testing "It adds logs for the date in focus to ensure the new activities are in the displayed date range"
      (is (= [0 0]
             (-> (sut/add-activities db [nil activities])
                 :logs
                 date-being-edited
                 ((juxt activity-id-a activity-id-b))))))
    (testing "It adds entries in :activity-types"
      (is (= [activity-type-a activity-type-b]
             (-> (sut/add-activities db [nil activities])
                 :activity-types
                 ((juxt activity-id-a activity-id-b))))))
    (testing "It adds entries in :activity-names"
      (is (= [activity-name-a activity-name-b]
             (-> (sut/add-activities db [nil activities])
                 :activity-names
                 ((juxt activity-id-a activity-id-b))))))))

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
