(ns daily-log.views-test
  (:require [cljs.test :refer (deftest is testing)]
            [daily-log.views :as sut]))

(deftest log-val->display-str
  (testing "converts 0 to \"No\" when type is :bool"
    (is (= "No"
           (sut/log-val->display-str :bool 0))))
  (testing "converts 1 to \"Yes\" when type is :bool"
    (is (= "Yes"
           (sut/log-val->display-str :bool 1))))
  (testing "converts 5 to \"5\" when type is :int"
    (is (= "5"
           (sut/log-val->display-str :int 5)))))

