(ns java-http.core
  "Convenience functions for HTTP requests.

  Provides HTTP convenience functions (get, post, put, delete, etc.) as a higher-level
  API on top of the lower-level client/send function.

  All functions accept a URI and optional request map, and return the
  response as a Clojure map."
  (:refer-clojure :exclude [get])
  (:require
   [java-http.client :as client]))

(defn- get-body-handler
  "Extracts body-handler from request map and returns a BodyHandler."
  [request]
  (client/->body-handler (:body-handler request) (:body-handler-file request)))

(defn request
  "Sends an HTTP request using the default client.

  Takes a request map with the same keys as java-http.request/build.
  The request map can also include:
    :body-handler       - Keyword: :string (default), :bytes, :input-stream, :discarding, :file
                          Or an HttpResponse$BodyHandler instance, or a function.
    :body-handler-file  - Path, File, or String (required when :body-handler is :file)

  This is the underlying function used by all the HTTP convenience functions.

  See java-http.request/build for request map format.
  See java-http.client/send for return value format."
  [request]
  (client/send request (get-body-handler request)))

(defn head
  "Sends a HEAD request to the specified URI.

  Arity 1: (head uri)
  Arity 2: (head uri request-map)

  The request-map is merged with method :head and the given uri.
  Use the second arity to add headers, timeout, etc.

  Returns a response map with keys: :body, :headers, :status, :uri, :version, :previous-response, :ssl-session."
  ([uri request-map]
   (request (merge {:uri uri :method :head} request-map)))
  ([uri]
   (head uri {})))

(defn options
  "Sends an OPTIONS request to the specified URI.

  Arity 1: (options uri)
  Arity 2: (options uri request-map)

  The request-map is merged with method :options and the given uri.
  Use the second arity to add headers, timeout, etc.

  Returns a response map with keys: :body, :headers, :status, :uri, :version, :previous-response, :ssl-session."
  ([uri request-map]
   (request (merge {:uri uri :method :options} request-map)))
  ([uri]
   (options uri {})))

(defn get
  "Sends a GET request to the specified URI.

  Arity 1: (get uri)
  Arity 2: (get uri request-map)

  The request-map is merged with method :get and the given uri.
  Use the second arity to add headers, timeout, body, etc.

  Returns a response map with keys: :body, :headers, :status, :uri, :version, :previous-response, :ssl-session."
  ([uri request-map]
   (request (merge {:uri uri :method :get} request-map)))
  ([uri]
   (get uri {})))

(defn query
  "Sends a QUERY request to the specified URI.

  Arity 1: (query uri)
  Arity 2: (query uri request-map)

  The request-map is merged with method :query and the given uri.
  Use the second arity to add headers, timeout, etc.

  Returns a response map with keys: :body, :headers, :status, :uri, :version, :previous-response, :ssl-session."
  ([uri request-map]
   (request (merge {:uri uri :method :query} request-map)))
  ([uri]
   (query uri {})))

(defn post
  "Sends a POST request to the specified URI.

  Arity 1: (post uri)
  Arity 2: (post uri request-map)

  The request-map is merged with method :post and the given uri.
  Use the second arity to add body, headers, timeout, etc.

  Returns a response map with keys: :body, :headers, :status, :uri, :version, :previous-response, :ssl-session."
  ([uri request-map]
   (request (merge {:uri uri :method :post} request-map)))
  ([uri]
   (post uri {})))

(defn put
  "Sends a PUT request to the specified URI.

  Arity 1: (put uri)
  Arity 2: (put uri request-map)

  The request-map is merged with method :put and the given uri.
  Use the second arity to add body, headers, timeout, etc.

  Returns a response map with keys: :body, :headers, :status, :uri, :version, :previous-response, :ssl-session."
  ([uri request-map]
   (request (merge {:uri uri :method :put} request-map)))
  ([uri]
   (put uri {})))

(defn patch
  "Sends a PATCH request to the specified URI.

  Arity 1: (patch uri)
  Arity 2: (patch uri request-map)

  The request-map is merged with method :patch and the given uri.
  Use the second arity to add body, headers, timeout, etc.

  Returns a response map with keys: :body, :headers, :status, :uri, :version, :previous-response, :ssl-session."
  ([uri request-map]
   (request (merge {:uri uri :method :patch} request-map)))
  ([uri]
   (patch uri {})))

(defn delete
  "Sends a DELETE request to the specified URI.

  Arity 1: (delete uri)
  Arity 2: (delete uri request-map)

  The request-map is merged with method :delete and the given uri.
  Use the second arity to add headers, timeout, body, etc.

  Returns a response map with keys: :body, :headers, :status, :uri, :version, :previous-response, :ssl-session."
  ([uri request-map]
   (request (merge {:uri uri :method :delete} request-map)))
  ([uri]
   (delete uri {})))

(defn trace
  "Sends a TRACE request to the specified URI.

  Arity 1: (trace uri)
  Arity 2: (trace uri request-map)

  The request-map is merged with method :trace and the given uri.
  Use the second arity to add headers, timeout, etc.

  Returns a response map with keys: :body, :headers, :status, :uri, :version, :previous-response, :ssl-session."
  ([uri request-map]
   (request (merge {:uri uri :method :trace} request-map)))
  ([uri]
   (trace uri {})))

(defn connect
  "Sends a CONNECT request to the specified URI.

  Arity 1: (connect uri)
  Arity 2: (connect uri request-map)

  The request-map is merged with method :connect and the given uri.
  Use the second arity to add headers, timeout, etc.

  Returns a response map with keys: :body, :headers, :status, :uri, :version, :previous-response, :ssl-session."
  ([uri request-map]
   (request (merge {:uri uri :method :connect} request-map)))
  ([uri]
   (connect uri {})))

;; Async convenience functions - return CompletableFuture

(defn request-async
  "Sends an HTTP request asynchronously using the default client.

  Takes a request map with the same keys as request.
  The request map can also include:
    :body-handler       - Keyword: :string (default), :bytes, :input-stream, :discarding, :file
                          Or an HttpResponse$BodyHandler instance, or a function.
    :body-handler-file  - Path, File, or String (required when :body-handler is :file)

  Returns a CompletableFuture that will complete with the response map.

  See java-http.request/build for request map format.
  See java-http.client/send-async for return value."
  [request]
  (client/send-async request (get-body-handler request)))

(defn head-async
  "Sends a HEAD request asynchronously to the specified URI.

  Arity 1: (head-async uri)
  Arity 2: (head-async uri request-map)

  The request-map is merged with method :head and the given uri.
  Use the second arity to add headers, timeout, etc.

  Returns a CompletableFuture that will complete with the response map."
  ([uri request-map]
   (request-async (merge {:uri uri :method :head} request-map)))
  ([uri]
   (head-async uri {})))

(defn options-async
  "Sends an OPTIONS request asynchronously to the specified URI.

  Arity 1: (options-async uri)
  Arity 2: (options-async uri request-map)

  The request-map is merged with method :options and the given uri.
  Use the second arity to add headers, timeout, etc.

  Returns a CompletableFuture that will complete with the response map."
  ([uri request-map]
   (request-async (merge {:uri uri :method :options} request-map)))
  ([uri]
   (options-async uri {})))

(defn get-async
  "Sends a GET request asynchronously to the specified URI.

  Arity 1: (get-async uri)
  Arity 2: (get-async uri request-map)

  The request-map is merged with method :get and the given uri.
  Use the second arity to add headers, timeout, body, etc.

  Returns a CompletableFuture that will complete with the response map."
  ([uri request-map]
   (request-async (merge {:uri uri :method :get} request-map)))
  ([uri]
   (get-async uri {})))

(defn query-async
  "Sends a QUERY request asynchronously to the specified URI.

  Arity 1: (query-async uri)
  Arity 2: (query-async uri request-map)

  The request-map is merged with method :query and the given uri.
  Use the second arity to add headers, timeout, etc.

  Returns a CompletableFuture that will complete with the response map."
  ([uri request-map]
   (request-async (merge {:uri uri :method :query} request-map)))
  ([uri]
   (query-async uri {})))

(defn post-async
  "Sends a POST request asynchronously to the specified URI.

  Arity 1: (post-async uri)
  Arity 2: (post-async uri request-map)

  The request-map is merged with method :post and the given uri.
  Use the second arity to add body, headers, timeout, etc.

  Returns a CompletableFuture that will complete with the response map."
  ([uri request-map]
   (request-async (merge {:uri uri :method :post} request-map)))
  ([uri]
   (post-async uri {})))

(defn put-async
  "Sends a PUT request asynchronously to the specified URI.

  Arity 1: (put-async uri)
  Arity 2: (put-async uri request-map)

  The request-map is merged with method :put and the given uri.
  Use the second arity to add body, headers, timeout, etc.

  Returns a CompletableFuture that will complete with the response map."
  ([uri request-map]
   (request-async (merge {:uri uri :method :put} request-map)))
  ([uri]
   (put-async uri {})))

(defn patch-async
  "Sends a PATCH request asynchronously to the specified URI.

  Arity 1: (patch-async uri)
  Arity 2: (patch-async uri request-map)

  The request-map is merged with method :patch and the given uri.
  Use the second arity to add body, headers, timeout, etc.

  Returns a CompletableFuture that will complete with the response map."
  ([uri request-map]
   (request-async (merge {:uri uri :method :patch} request-map)))
  ([uri]
   (patch-async uri {})))

(defn delete-async
  "Sends a DELETE request asynchronously to the specified URI.

  Arity 1: (delete-async uri)
  Arity 2: (delete-async uri request-map)

  The request-map is merged with method :delete and the given uri.
  Use the second arity to add headers, timeout, body, etc.

  Returns a CompletableFuture that will complete with the response map."
  ([uri request-map]
   (request-async (merge {:uri uri :method :delete} request-map)))
  ([uri]
   (delete-async uri {})))

(defn trace-async
  "Sends a TRACE request asynchronously to the specified URI.

  Arity 1: (trace-async uri)
  Arity 2: (trace-async uri request-map)

  The request-map is merged with method :trace and the given uri.
  Use the second arity to add headers, timeout, etc.

  Returns a CompletableFuture that will complete with the response map."
  ([uri request-map]
   (request-async (merge {:uri uri :method :trace} request-map)))
  ([uri]
   (trace-async uri {})))

(defn connect-async
  "Sends a CONNECT request asynchronously to the specified URI.

  Arity 1: (connect-async uri)
  Arity 2: (connect-async uri request-map)

  The request-map is merged with method :connect and the given uri.
  Use the second arity to add headers, timeout, etc.

  Returns a CompletableFuture that will complete with the response map."
  ([uri request-map]
   (request-async (merge {:uri uri :method :connect} request-map)))
  ([uri]
   (connect-async uri {})))
