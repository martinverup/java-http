(ns java-http.request
  "HTTP request builder for Java's java.net.http.HttpRequest."
  (:require
   [clojure.string :as str])
  (:import
   [java.net URI]
   [java.io File InputStream]
   [java.net.http
    HttpClient$Version
    HttpRequest
    HttpRequest$BodyPublisher
    HttpRequest$BodyPublishers
    HttpRequest$Builder]
   [java.time Duration]))

(defn- format-headers
  "Converts a Clojure map of headers to a String array for Java HttpRequest.

  Takes a map of keyword or string to String/Number and converts it to a flat
  String array in the format [key1 value1 key2 value2 ...]."
  [headers]
  (let [arr (make-array String (* 2 (count headers)))]
    (reduce-kv (fn [i k v]
                 (let [j (int i)
                       kstr (if (keyword? k) (name k) (str k))]
                   (aset arr j kstr)
                   (aset arr (inc j) (str v))
                   (+ j 2)))
               0
               headers)
    arr))

(defn- format-method
  "Converts a Clojure keyword or String to uppercase HTTP method string.

  Accepted keywords: :get, :post, :put, :delete, :head, :options, :patch, :trace, :connect, :query
  These are converted to uppercase strings: GET, POST, PUT, etc."
  [method]
  (str/upper-case (name method)))

(defn- ->body-publisher
  "Creates a BodyPublisher from the request body.

  If body is nil, creates a no-body BodyPublisher.
  If body is a HttpRequest$BodyPublisher, returns it directly.
  If body is an InputStream, creates an InputStream BodyPublisher.
  If body is a File or Path, creates a file BodyPublisher.
  If body is a string, creates a String-based BodyPublisher.
  If body is a byte array, creates a byte array BodyPublisher."
  [body]
  (cond
    (nil? body) (HttpRequest$BodyPublishers/noBody)
    (instance? HttpRequest$BodyPublisher body) body
    (instance? InputStream body) (HttpRequest$BodyPublishers/ofInputStream (fn [] body))
    (instance? File body) (HttpRequest$BodyPublishers/ofFile (File/.toPath body))
    (string? body) (HttpRequest$BodyPublishers/ofString body)
    :else (HttpRequest$BodyPublishers/ofByteArray body)))

(defn- format-timeout
  "Converts a timeout value to java.time.Duration.

  If timeout is an integer, converts it to Duration using Duration/ofMillis.
  If timeout is already a Duration instance, returns it unchanged."
  [timeout]
  (if (int? timeout)
    (Duration/ofMillis timeout)
    timeout))

(defn- format-version
  "Converts a version value to HttpClient$Version.

  If version is already an HttpClient$Version instance, returns it unchanged.
  If version is a keyword, converts it to the corresponding enum value.

  Accepted keywords: :HTTP_1_1, :HTTP_2"
  [version]
  (if (instance? HttpClient$Version version)
    version
    (-> version name str/upper-case (HttpClient$Version/valueOf))))

(defn build
  "Builds and returns a java.net.http.HttpRequest instance.

  Takes a map with the following keys:
    :uri             - String, the URL to request (required)
    :method          - Keyword or string, HTTP method (required)
    :headers         - Map of request headers, keys can be either keyword or string
    :body            - String, byte array, File, Path, InputStream, or HttpRequest$BodyPublisher
    :timeout         - Request timeout in millis (int) or java.time.Duration
    :version         - HTTP version keyword or HttpClient$Version instance
    :expect-continue - Boolean, whether to use Expect: 100-Continue header"
  [{:keys [headers method body timeout uri version expect-continue]}]
  ^HttpRequest (HttpRequest$Builder/.build
                (cond-> (HttpRequest/newBuilder (URI/create uri))
                  headers (HttpRequest$Builder/.headers (format-headers headers))
                  method (HttpRequest$Builder/.method (format-method method) (->body-publisher body))
                  timeout (HttpRequest$Builder/.timeout (format-timeout timeout))
                  version (HttpRequest$Builder/.version (format-version version))
                  expect-continue (HttpRequest$Builder/.expectContinue expect-continue))))
