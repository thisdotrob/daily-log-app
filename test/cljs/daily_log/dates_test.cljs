(ns daily-log.dates-test
  (:require [cljs.test :refer (deftest is testing)]
            [daily-log.dates :as sut]))

(deftest ->DD-str
  (testing "converts a keyword date into a DD format string"
    (is (= "31"
           (sut/->DD-str :2020-01-31)))
    (is (= "1"
           (sut/->DD-str :2000-02-01)))
    (is (= "13"
           (sut/->DD-str :1999-12-13)))))

(deftest ->MMM-str
  (testing "converts a keyword date into a MMM format string"
    (is (= "Jan"
           (sut/->MMM-str :2020-01-31)))
    (is (= "Feb"
           (sut/->MMM-str :2000-02-01)))
    (is (= "Dec"
           (sut/->MMM-str :1999-12-13)))))

(deftest ->dow-str
  (testing "converts a keyword date into a day-of-week format string"
    (is (= "Wed"
           (sut/->dow-str :2020-04-01)))
    (is (= "Sun"
           (sut/->dow-str :2000-04-02)))
    (is (= "Sat"
           (sut/->dow-str :1999-04-03)))))
