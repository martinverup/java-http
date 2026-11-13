(ns java-http.response-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [java-http.response :as response])
  (:import
   [java.net.http HttpClient$Version]
   [java.util Optional]))

(deftest unpack-optional-test
  (testing "unpack-optional with present value"
    (let [optional (Optional/of "test-value")]
      (is (= "test-value" (#'response/unpack-optional optional)))))

  (testing "unpack-optional with empty value"
    (let [optional (Optional/ofNullable nil)]
      (is (nil? (#'response/unpack-optional optional)))))

  (testing "unpack-optional with another present value"
    (let [optional (Optional/of 42)]
      (is (= 42 (#'response/unpack-optional optional))))))

(deftest format-version-test
  (testing "format-version with HTTP_1_1"
    (is (= :HTTP_1_1 (#'response/format-version HttpClient$Version/HTTP_1_1))))

  (testing "format-version with HTTP_2"
    (is (= :HTTP_2 (#'response/format-version HttpClient$Version/HTTP_2)))))
