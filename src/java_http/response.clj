(ns java-http.response
  "HTTP response formatter for Java's java.net.http.HttpResponse."
  (:refer-clojure :exclude [format])
  (:import
   [java.net.http
    HttpClient$Version
    HttpHeaders
    HttpResponse]
   [java.util Optional]))

(defn- unpack-optional
  "Extracts the value from a Java Optional, returning nil if empty."
  [^Optional optional]
  (Optional/.orElse optional nil))

(defn- format-headers
  "Converts Java HttpHeaders to a Clojure map."
  [^HttpHeaders headers]
  (->> headers
       (HttpHeaders/.map)
       (map (fn [[k v]] [(keyword k) (cond-> v (= 1 (count v)) first)]))
       (into {})))

(defn- format-version
  "Converts HttpClient$Version enum to Clojure keyword.
   Returns :HTTP_1_1 or :HTTP_2."
  [^HttpClient$Version http-version]
  (-> http-version HttpClient$Version/.name keyword))

(defn format
  "Converts a Java HttpResponse to a Clojure map.

  Takes a java.net.http.HttpResponse.
  Extracts all relevant data, converting Java types to Clojure types.

  The returned map contains:
    :body              - String, byte array, InputStream, or File depending on body handler
    :headers           - Map of keyword to String/Vector for response headers
    :status            - Integer, the HTTP status code
    :uri               - String, the final URI after any redirects
    :version           - Keyword, :HTTP_1_1 or :HTTP_2
    :previous-response - The previous response in a redirect chain, or nil
    :ssl-session       - The SSL session used, or nil"
  [^HttpResponse response]
   (let [^HttpHeaders headers (HttpResponse/.headers response)
         ^HttpClient$Version http-version (HttpResponse/.version response)]
     {:body (HttpResponse/.body response)
      :headers (format-headers headers)
      :status (HttpResponse/.statusCode response)
      :uri (str (HttpResponse/.uri response))
      :version (format-version http-version)
      :previous-response (some-> (unpack-optional (HttpResponse/.previousResponse response)) format)
      :ssl-session (unpack-optional (HttpResponse/.sslSession response))}))
