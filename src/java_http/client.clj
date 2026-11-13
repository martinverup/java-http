(ns java-http.client
  "HTTP client wrapper for Java's java.net.http.HttpClient."
  (:refer-clojure :exclude [send])
  (:require
   [clojure.string :as str]
   [java-http.request  :as request]
   [java-http.response :as response])
  (:import
   [java.io File]
   [java.net.http
    HttpClient
    HttpClient$Builder
    HttpClient$Redirect
    HttpClient$Version
    HttpRequest
    HttpResponse
    HttpResponse$BodyHandler
    HttpResponse$BodyHandlers
    HttpResponse$PushPromiseHandler]
   [java.nio.file Path Paths]
   [java.time Duration]
   [java.util.concurrent CompletableFuture]
   [java.util.function Function]))

(defn format-redirect
  "Converts a Clojure keyword or HttpClient$Redirect instance to HttpClient$Redirect enum.

  If passed a keyword, converts it to the corresponding enum value.
  If passed an HttpClient$Redirect instance, returns it unchanged.

  Accepted keywords:
    :always  - Always follow redirects
    :never   - Never follow redirects
    :normal  - Follow redirects according to standard behavior

  Returns the HttpClient$Redirect enum constant.

  Throws IllegalArgumentException if keyword is not recognized."
  [redirect]
  (if (instance? HttpClient$Redirect redirect)
    redirect
    (case redirect
      :always HttpClient$Redirect/ALWAYS
      :never HttpClient$Redirect/NEVER
      :normal HttpClient$Redirect/NORMAL
      (throw (IllegalArgumentException. (str "Unknown redirect policy: " redirect))))))

(defn format-version
  "Converts a Clojure keyword or HttpClient$Version instance to HttpClient$Version enum.

  If passed a keyword, converts it to the corresponding HttpClient$Version enum.
  If passed an HttpClient$Version instance, returns it unchanged.

  Accepted keywords: :HTTP_1_1, :HTTP_2

  Returns the HttpClient$Version enum constant.

  Throws IllegalArgumentException if keyword does not match a valid version."
  [version]
  (if (instance? HttpClient$Version version)
    version
    (try
      (-> version name str/upper-case (HttpClient$Version/valueOf))
      (catch IllegalArgumentException e
        (throw (IllegalArgumentException. (str "Unknown HTTP version: " version) e))))))

(defn- format-timeout
  "Converts a timeout value to java.time.Duration.

  If timeout is an integer, converts it to Duration using Duration/ofMillis.
  If timeout is already a Duration instance, returns it unchanged."
  [timeout]
  (if (int? timeout)
    (Duration/ofMillis timeout)
    timeout))

(defn- ->path
  "Coerces a file location to a java.nio.file.Path.

  Accepts a Path, a java.io.File, or a String path."
  [p]
  (cond
    (instance? Path p) p
    (instance? File p) (.toPath ^File p)
    (string? p) (Paths/get p (make-array String 0))
    :else (throw (IllegalArgumentException.
                  (str "Unsupported file path type: " (class p))))))

(defn ->body-handler
  "Returns an HttpResponse$BodyHandler.

  If a HttpResponse$BodyHandler instance, returns it directly.
  If a keyword, maps to corresponding HttpResponse$BodyHandlers method:
    :string       -> ofString
    :bytes        -> ofByteArray
    :input-stream -> ofInputStream
    :discarding   -> discarding
    :file         -> ofFile (second arg may be a Path, File, or String path)

  Otherwise, returns ofString."
  ([bh]
   (->body-handler bh nil))
  ([bh file-path]
   (cond
     (instance? HttpResponse$BodyHandler bh)
     bh

     (keyword? bh)
     (case bh
       :string       (HttpResponse$BodyHandlers/ofString)
       :bytes        (HttpResponse$BodyHandlers/ofByteArray)
       :input-stream (HttpResponse$BodyHandlers/ofInputStream)
       :discarding   (HttpResponse$BodyHandlers/discarding)
       :file         (HttpResponse$BodyHandlers/ofFile (->path file-path)))

     :else
     (HttpResponse$BodyHandlers/ofString))))

(defn build
  "Builds and returns an HttpClient instance with the specified configuration.

  Accepts keyword arguments corresponding to HttpClient.Builder methods.
  All parameters are optional. Omitted parameters use Java's defaults.

  Parameters:
    :authenticator     - java.net.Authenticator instance
    :connect-timeout   - Connection timeout in millis (int) or Duration
    :cookie-handler    - java.net.CookieHandler instance
    :executor          - java.util.concurrent.Executor instance
    :follow-redirects  - Redirect policy keyword: :always, :never, :normal
    :priority          - Integer priority for the client
    :proxy             - java.net.Proxy instance
    :ssl-context       - javax.net.ssl.SSLContext instance
    :ssl-parameters    - javax.net.ssl.SSLParameters instance
    :version           - HTTP version keyword: :HTTP_1_1, :HTTP_2

  Returns a java.net.http.HttpClient instance."
  [& {:keys [authenticator
             connect-timeout
             cookie-handler
             executor
             follow-redirects
             priority
             proxy
             ssl-context
             ssl-parameters
             version]}]
  ^HttpClient (HttpClient$Builder/.build
               (cond-> (HttpClient/newBuilder)
                 authenticator (HttpClient$Builder/.authenticator authenticator)
                 connect-timeout (HttpClient$Builder/.connectTimeout (format-timeout connect-timeout))
                 cookie-handler (HttpClient$Builder/.cookieHandler cookie-handler)
                 executor (HttpClient$Builder/.executor executor)
                 follow-redirects (HttpClient$Builder/.followRedirects (format-redirect follow-redirects))
                 priority (HttpClient$Builder/.priority priority)
                 proxy (HttpClient$Builder/.proxy proxy)
                 ssl-context (HttpClient$Builder/.sslContext ssl-context)
                 ssl-parameters (HttpClient$Builder/.sslParameters ssl-parameters)
                 version (HttpClient$Builder/.version (format-version version)))))

(def default-client
  "A pre-built HttpClient instance with default configuration.

  Equivalent to calling build with no arguments.
  Uses Java's default settings for all parameters.

  Reuse this instance for multiple requests to avoid overhead."
  ^HttpClient (build))

(defn send
  "Sends an HTTP request synchronously and returns the response as a Clojure map.

  Matches Java signature: HttpClient.send(HttpRequest, BodyHandler)

  Arity 2: (send request body-handler)
  Arity 3: (send client request body-handler)

  request is a map with keys: uri, method, headers, body, timeout, version.
  body-handler is an HttpResponse$BodyHandler (use body-handler helper for keywords).

  Returns a map with keys: body, headers, status, uri, version, previous-response, ssl-session.

  Throws IOException on network errors, TimeoutException on timeouts."
  ([request body-handler]
   (send default-client request body-handler))
  ([^HttpClient client request ^HttpResponse$BodyHandler body-handler]
   (let [^HttpRequest req (request/build request)
         ^HttpResponse response (HttpClient/.send client req body-handler)]
     (response/format response))))

(defn send-async
  "Sends an HTTP request asynchronously and returns a CompletableFuture.

  Matches Java signatures:
    HttpClient.sendAsync(HttpRequest, BodyHandler)
    HttpClient.sendAsync(HttpRequest, BodyHandler, PushPromiseHandler)

  Arity 2: (send-async request body-handler)
  Arity 3: (send-async client request body-handler)
  Arity 4: (send-async client request body-handler push-promise-handler)

  request is a map with keys: uri, method, headers, body, timeout, version.
  body-handler is an HttpResponse$BodyHandler.
  push-promise-handler is an optional function: (fn [initiating-request push-request accept-fn] ...)
    where accept-fn is a Function<BodyHandler, CompletableFuture<HttpResponse>>.

  Returns a CompletableFuture that will complete with the response map.

  Throws IOException on network errors, TimeoutException on timeouts."
  ([request body-handler]
   (send-async default-client request body-handler nil))
  ([^HttpClient client request body-handler]
   (send-async client request ^HttpResponse$BodyHandler body-handler nil))
  ([^HttpClient client request ^HttpResponse$BodyHandler body-handler ^HttpResponse$PushPromiseHandler push-promise-handler]
   (let [^HttpRequest req (request/build request)
         ^CompletableFuture java-future (if push-promise-handler
                                          (HttpClient/.sendAsync client req body-handler push-promise-handler)
                                          (HttpClient/.sendAsync client req body-handler))]
     (CompletableFuture/.thenApply
      java-future
      (reify Function
        (apply [_this response]
          (response/format response)))))))
