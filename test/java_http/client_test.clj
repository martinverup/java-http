(ns java-http.client-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [java-http.client :as client])
  (:import
   [java.net.http
    HttpClient
    HttpClient$Redirect
    HttpClient$Version
    HttpResponse$BodyHandler
    HttpResponse$BodyHandlers]
   [java.time Duration]
   [java.util Optional]))

(deftest format-redirect-test
  (testing "keyword to HttpClient$Redirect conversion"
    (is (= HttpClient$Redirect/ALWAYS
           (#'client/format-redirect :always)))
    (is (= HttpClient$Redirect/NEVER
           (#'client/format-redirect :never)))
    (is (= HttpClient$Redirect/NORMAL
           (#'client/format-redirect :normal))))

  (testing "Java enum passthrough"
    (is (= HttpClient$Redirect/ALWAYS
           (#'client/format-redirect HttpClient$Redirect/ALWAYS)))
    (is (= HttpClient$Redirect/NEVER
           (#'client/format-redirect HttpClient$Redirect/NEVER)))
    (is (= HttpClient$Redirect/NORMAL
           (#'client/format-redirect HttpClient$Redirect/NORMAL)))))

(deftest format-redirect-error-test
  (testing "throws on invalid keyword"
    (is (thrown? IllegalArgumentException (#'client/format-redirect :invalid)))))

(deftest format-version-test
  (testing "keyword to HttpClient$Version conversion"
    (is (= HttpClient$Version/HTTP_1_1
           (#'client/format-version :HTTP_1_1)))
    (is (= HttpClient$Version/HTTP_2
           (#'client/format-version :HTTP_2))))

  (testing "Java enum passthrough"
    (is (= HttpClient$Version/HTTP_1_1
           (#'client/format-version HttpClient$Version/HTTP_1_1)))
    (is (= HttpClient$Version/HTTP_2
           (#'client/format-version HttpClient$Version/HTTP_2)))))

(deftest format-version-error-test
  (testing "throws on invalid keyword"
    (is (thrown? IllegalArgumentException (#'client/format-version :invalid)))))

(deftest build-test
  (testing "build with no arguments returns default client"
    (let [c (client/build)]
      (is (instance? HttpClient c))))

  (testing "build with follow-redirects keyword"
    (let [c (client/build :follow-redirects :never)]
      (is (instance? HttpClient c))
      (is (= HttpClient$Redirect/NEVER
           (HttpClient/.followRedirects c)))))

  (testing "build with version keyword"
    (let [c (client/build :version :HTTP_2)]
      (is (instance? HttpClient c))
      (is (= HttpClient$Version/HTTP_2
           (HttpClient/.version c)))))

  (testing "build with connect-timeout"
    (let [c (client/build :connect-timeout 10000)
          timeout-opt (HttpClient/.connectTimeout c)]
      (is (some? timeout-opt))
      (let [timeout (Optional/.get timeout-opt)]
        (is (instance? Duration timeout))
        (is (= 10000 (Duration/.toMillis timeout))))))

  (testing "build with all parameters"
    (let [c (client/build
              :authenticator nil
              :connect-timeout 5000
              :cookie-handler nil
              :executor nil
              :follow-redirects :normal
              :priority 1
              :proxy nil
              :ssl-context nil
              :ssl-parameters nil
              :version :HTTP_1_1)]
      (is (instance? HttpClient c))
      (is (= HttpClient$Version/HTTP_1_1 (HttpClient/.version c)))
      (is (= HttpClient$Redirect/NORMAL (HttpClient/.followRedirects c))))))

(deftest body-handler-test
  (testing "nil defaults to ofString handler"
    (let [handler (#'client/->body-handler nil)]
      (is (instance? HttpResponse$BodyHandler handler))))

  (testing ":string returns BodyHandler"
    (let [handler (#'client/->body-handler :string)]
      (is (instance? HttpResponse$BodyHandler handler))))

  (testing ":bytes returns BodyHandler"
    (let [handler (#'client/->body-handler :bytes)]
      (is (instance? HttpResponse$BodyHandler handler))))

  (testing ":input-stream returns BodyHandler"
    (let [handler (#'client/->body-handler :input-stream)]
      (is (instance? HttpResponse$BodyHandler handler))))

  (testing ":discarding returns BodyHandler"
    (let [handler (#'client/->body-handler :discarding)]
      (is (instance? HttpResponse$BodyHandler handler))))

  (testing "Java BodyHandler instance returns unchanged"
    (let [h (HttpResponse$BodyHandlers/ofString)
          handler (#'client/->body-handler h)]
      (is (identical? h handler))))

  (testing "function returns wrapped BodyHandler"
    (let [f (fn [_] "test")
          handler (#'client/->body-handler f)]
      (is (instance? HttpResponse$BodyHandler handler))
      (is (not (identical? HttpResponse$BodyHandlers/ofString handler)))))
  
    (testing "throws on unknown keyword"
      (is (thrown? IllegalArgumentException (#'client/->body-handler :unknown))))

  (testing ":file with path returns BodyHandler"
    (let [path (java.nio.file.Paths/get "/tmp" (into-array String ["test"]))
          handler (#'client/->body-handler :file path)]
      (is (instance? HttpResponse$BodyHandler handler))))

  (testing ":file with File returns BodyHandler"
    (let [f (java.io.File/createTempFile "java-http" ".tmp")
          handler (#'client/->body-handler :file f)]
      (is (instance? HttpResponse$BodyHandler handler))
      (.delete f)))

  (testing ":file with String path returns BodyHandler"
    (let [f (java.io.File/createTempFile "java-http" ".tmp")
          handler (#'client/->body-handler :file (.getAbsolutePath f))]
      (is (instance? HttpResponse$BodyHandler handler))
      (.delete f)))

  (testing ":file with unsupported type throws"
    (is (thrown? IllegalArgumentException (#'client/->body-handler :file 42)))))
