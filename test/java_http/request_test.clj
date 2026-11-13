(ns java-http.request-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [java-http.request :as request])
  (:import
   [java.net URI]
   [java.net.http
    HttpClient$Version
    HttpHeaders
    HttpRequest
    HttpRequest$BodyPublisher]
   [java.time Duration]
   [java.util Optional]))

(deftest build-test
  (let [req (request/build {:uri     "https://example.com/path"
                            :method  :get
                            :headers {:content-type "application/json"}
                            :body    "test"
                            :timeout 5000
                            :version :HTTP_2})]
    (is (instance? HttpRequest req))
    (is (= (URI/create "https://example.com/path") (HttpRequest/.uri req)))
    (is (= "GET" (HttpRequest/.method req)))
    (let [headers (-> (HttpRequest/.headers req) HttpHeaders/.map)]
      (is (some? headers))
      (is (= "application/json" (first (get headers "content-type")))))
    (is (= (Duration/ofMillis 5000) (Optional/.get (HttpRequest/.timeout req))))
    (is (= HttpClient$Version/HTTP_2 (Optional/.get (HttpRequest/.version req))))))

(deftest format-headers-test
  (testing "keyword header keys"
    (let [result (#'request/format-headers {:content-type "application/json" :accept "text/plain"})]
      (is (instance? (Class/forName "[Ljava.lang.String;") result))
      (is (= 4 (alength result)))
      (is (= "content-type" (aget result 0)))
      (is (= "application/json" (aget result 1)))
      (is (= "accept" (aget result 2)))
      (is (= "text/plain" (aget result 3)))))

  (testing "string header keys"
    (let [result (#'request/format-headers {"content-type" "application/json" "accept" "text/plain"})]
      (is (instance? (Class/forName "[Ljava.lang.String;") result))
      (is (= 4 (alength result)))
      (is (= "content-type" (aget result 0)))
      (is (= "application/json" (aget result 1)))
      (is (= "accept" (aget result 2)))
      (is (= "text/plain" (aget result 3)))))

  (testing "mixed keyword and string header keys"
    (let [result (#'request/format-headers {:content-type "application/json" "accept" "text/plain"})]
      (is (instance? (Class/forName "[Ljava.lang.String;") result))
      (is (= 4 (alength result))))))

(deftest format-method-test
  (is (= "GET" (#'request/format-method :get)))
  (is (= "POST" (#'request/format-method :post)))
  (is (= "PUT" (#'request/format-method :put)))
  (is (= "DELETE" (#'request/format-method :delete)))
  (is (= "HEAD" (#'request/format-method :head)))
  (is (= "OPTIONS" (#'request/format-method :options)))
  (is (= "PATCH" (#'request/format-method :patch)))
  (is (= "TRACE" (#'request/format-method :trace)))
  (is (= "CONNECT" (#'request/format-method :connect))))

(deftest body-publisher-test
  (is (instance? HttpRequest$BodyPublisher (#'request/->body-publisher "test"))))

(deftest format-timeout-test
  (let [result (#'request/format-timeout 5000)]
    (is (instance? Duration result))
    (is (= 5000 (Duration/.toMillis result)))))

(deftest format-version-test
  (is (= HttpClient$Version/HTTP_1_1
         (#'request/format-version :HTTP_1_1)))
  (is (= HttpClient$Version/HTTP_2
         (#'request/format-version :HTTP_2))))
