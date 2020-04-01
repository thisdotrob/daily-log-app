(ns daily-log.subs-test
  (:require [cljs.test :refer (deftest is)]
            [daily-log.subs :as sut]))

(deftest log
  (is (= nil (sut/log {} [nil :a :2020-04-01]))))
