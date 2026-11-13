# java-http

A Clojure wrapper for Java's `java.net.http` HTTP client. Provides a clean, idiomatic Clojure API for making HTTP requests without direct Java interop.

## Overview

This library wraps Java's modern HTTP client (`java.net.http.HttpClient`, introduced in Java 11) with a Clojure-friendly interface. All Java interop is handled internally - users interact only with Clojure data structures like keywords, maps, strings, and integers.

## Project Structure

The library is organized into four namespaces:

- **`java-http.core`** - High-level convenience functions for HTTP methods (get, post, put, etc.) with both sync and async variants
- **`java-http.client`** - Low-level API wrapping Java's HttpClient with send/send-async functions and client building
- **`java-http.request`** - Request building from Clojure maps
- **`java-http.response`** - Response formatting from Java HttpResponse to Clojure maps

## Quick Start

Add the dependency from Clojars to your `deps.edn`:

```clojure
{:deps {io.github.martinverup/java-http {:mvn/version "0.1.0"}}}
```

For local development against a checkout, use `{:deps {io.github.martinverup/java-http {:local/root "./path/to/java-http"}}}` instead.

## Usage

### Core Namespace (Recommended)

Require the core namespace for the easiest API:

```clojure
(require '[java-http.core :as http])
```

#### Simple GET Request

```clojure
(let [response (http/get "https://example.com/posts/1")]
  (println (:status response))  ;; => 200
  (println (:body response)))   ;; => JSON response body
```

#### GET with Query Parameters

```clojure
;; GET with query parameters in the URI
(let [response (http/get "https://example.com/posts?userId=1")]
  (println (:body response)))
```

#### POST Request

```clojure
;; POST with a JSON body
(let [response (http/post "https://example.com/posts"
                        {:body "{\"title\": \"foo\", \"body\": \"bar\", \"userId\": 1}"
                         :headers {:content-type "application/json"}})]
  (println (:status response))  ;; => 201
  (println (:body response)))
```

#### Other HTTP Methods

```clojure
;; PUT
(http/put "https://example.com/resource/1"
          {:body "{\"id\": 1, \"title\": \"updated\"}"
           :headers {:content-type "application/json"}})

;; DELETE
(http/delete "https://example.com/resource/1")

;; HEAD (returns headers only)
(http/head "https://example.com/resource/1")

;; OPTIONS
(http/options "https://example.com/resource")

;; PATCH
(http/patch "https://example.com/resource/1"
            {:body "{\"title\": \"new title\"}"
             :headers {:content-type "application/json"}})

;; TRACE
(http/trace "https://example.com")

;; CONNECT
(http/connect "https://example.com:443")

;; QUERY
(http/query "https://example.com/api/query"
            {:body "{\"query\": \"search term\"}"})
```

### Request Map

All functions accept an optional request map as the second argument. The request map supports the following keys:

| Key | Type | Description |
|-----|------|-------------|
| `:uri` | String | The URL to request (required when using `request` directly) |
| `:method` | Keyword | HTTP method: `:get`, `:post`, `:put`, `:delete`, `:head`, `:options`, `:patch`, `:query`, `:trace`, `:connect` (required when using `request` directly) |
| `:headers` | Map | Request headers as keyword or string to String/Number map |
| `:body` | String, byte[], File, InputStream, or BodyPublisher | Request body content |
| `:timeout` | Integer or Duration | Request timeout in milliseconds |
| `:version` | Keyword | HTTP version: `:HTTP_1_1` or `:HTTP_2` |
| `:expect-continue` | Boolean | Whether to use Expect: 100-Continue header |
| `:body-handler` | Keyword or BodyHandler | Response body handler. Keywords: `:string` (default), `:bytes`, `:input-stream`, `:discarding`, `:file` |
| `:body-handler-file` | Path, File, or String | Required when `:body-handler` is `:file`. The file path to write the response body to |

#### Examples with Request Map

```clojure
;; GET with custom headers
(let [response (http/get "https://example.com/data"
                        {:headers {:authorization "Bearer token123"
                                   :accept "application/json"}})]
  (println (:body response)))

;; POST with body and headers
(let [response (http/post "https://example.com/users"
                        {:body "{\"name\": \"John\"}"
                         :headers {:content-type "application/json"
                                   :accept "application/json"}})]
  (println (:status response)))

;; GET with timeout (milliseconds)
(let [response (http/get "https://example.com/slow-endpoint"
                        {:timeout 5000})]  ;; 5 second timeout
  (println (:body response)))

;; GET with HTTP/2
(let [response (http/get "https://example.com" {:version :HTTP_2})]
  (println (:body response)))

;; GET with Expect: 100-Continue
(let [response (http/get "https://example.com/upload"
                        {:expect-continue true})]
  (println (:status response)))
```

### Response Map

All functions return a response map with the following keys:

| Key | Type | Description |
|-----|------|-------------|
| `:body` | String, byte[], InputStream, Path | The response body (type depends on body handler) |
| `:headers` | Map | Response headers as keyword to String/Vector map |
| `:status` | Integer | HTTP status code (e.g., 200, 404, 500) |
| `:uri` | String | The final URI after any redirects |
| `:version` | Keyword | HTTP version used: `:HTTP_1_1` or `:HTTP_2` |
| `:previous-response` | Map or nil | Previous response in redirect chain |
| `:ssl-session` | Object or nil | SSL session used for the connection |

#### Response Examples

```clojure
(let [response (http/get "https://example.com/posts/1")]
  ;; Access response fields
  (:status response)   ;; => 200
  (:body response)     ;; => "{\"userId\": 1, ...}"
  (:headers response)  ;; => {:content-type "application/json", ...}
  (:uri response)      ;; => "https://example.com/posts/1"
  (:version response)) ;; => :HTTP_2
```

### Using the request function directly

```clojure
;; Using request directly (requires :uri and :method)
(http/request {:uri "https://example.com" :method :get})

(http/request {:uri "https://example.com" :method :post :body "data"})
```

## Async Requests

All HTTP methods have async variants that return `CompletableFuture`:

```clojure
;; Async GET
(let [future (http/get-async "https://example.com")]
  ;; Block and get the response
  (let [response (.get future)]
    (println (:status response))))

;; Async POST with callback
(let [future (http/post-async "https://example.com/api"
                                 {:body "{\"key\": \"value\"}"
                                  :headers {:content-type "application/json"}})]
  (.thenAccept future (fn [response]
                         (println "Got response:" (:status response)))))

;; Using request-async directly
(let [async-response (http/request-async {:uri "https://example.com" :method :get})]
  (println "Done:" (:status @async-response)))

;; All async convenience functions:
;; get-async, post-async, put-async, delete-async, head-async, options-async
;; patch-async, query-async, trace-async, connect-async
```

## Body Handlers

By default, responses return the body as a String. You can customize this using the `:body-handler` parameter.

### Built-in Body Handlers (Keywords)

| Keyword | Description | Return Type |
|---------|-------------|-------------|
| `:string` | Body as String (default) | String |
| `:bytes` | Body as byte array | byte[] |
| `:input-stream` | Body as InputStream | java.io.InputStream |
| `:discarding` | Discard the body | nil |
| `:file` | Write body to a file | java.nio.file.Path |

### Examples

```clojure
;; Get response body as byte array
(http/get "https://example.com/image.png" {:body-handler :bytes})

;; Get response body as InputStream (useful for large downloads)
(http/get "https://example.com/large-file" {:body-handler :input-stream})

;; Discard the response body (useful for HEAD or status-only requests)
(http/get "https://example.com" {:body-handler :discarding})

;; Save response body directly to a file
;; the path may be a String, java.io.File, or java.nio.file.Path
(http/get "https://example.com/data.json" 
          {:body-handler :file
           :body-handler-file "/tmp/data.json"})
```

### Custom Body Handlers

You can pass a raw Java `HttpResponse$BodyHandler` instance:

```clojure
(import '[java.net.http HttpResponse$BodyHandlers])
(http/get "https://example.com" 
          {:body-handler HttpResponse$BodyHandlers/ofString})
```

## Low-Level API

For direct access to Java's HTTP client API, use the `java-http.client` namespace.

### Building a Custom Client

```clojure
(require '[java-http.client :as client])

;; Build a custom client with specific configuration
(let [my-client (client/build :connect-timeout 10000
                              :follow-redirects :never
                              :version :HTTP_2)]
  ;; Use the custom client with send
  (client/send my-client
               {:uri "https://example.com" :method :get} 
               (client/body-handler :string)))

;; Or use the default client
(client/send {:uri "https://example.com" :method :get} 
             (client/body-handler :string))
```

#### Client Configuration Options

| Key | Type | Description |
|-----|------|-------------|
| `:authenticator` | java.net.Authenticator | Custom authenticator |
| `:connect-timeout` | Integer or Duration | Connection timeout in millis |
| `:cookie-handler` | java.net.CookieHandler | Cookie handler |
| `:executor` | java.util.concurrent.Executor | Custom executor for async requests |
| `:follow-redirects` | Keyword | Redirect policy: `:always`, `:never`, `:normal` |
| `:priority` | Integer | Client priority |
| `:proxy` | java.net.Proxy | Proxy configuration |
| `:ssl-context` | javax.net.ssl.SSLContext | SSL context |
| `:ssl-parameters` | javax.net.ssl.SSLParameters | SSL parameters |
| `:version` | Keyword | HTTP version: `:HTTP_1_1`, `:HTTP_2` |

### Body Handler Helper

The `body-handler` function creates `HttpResponse$BodyHandler` instances:

```clojure
;; Keyword shortcuts
(client/body-handler :string)    ;; -> HttpResponse$BodyHandlers/ofString
(client/body-handler :bytes)     ;; -> HttpResponse$BodyHandlers/ofByteArray
(client/body-handler :input-stream) ;; -> HttpResponse$BodyHandlers/ofInputStream
(client/body-handler :discarding) ;; -> HttpResponse$BodyHandlers/discarding
(client/body-handler :file path)  ;; -> HttpResponse$BodyHandlers/ofFile(path)

;; Pass through existing handlers
(client/body-handler HttpResponse$BodyHandlers/ofString)
```

### Push Promise Handling (HTTP/2)

For HTTP/2 server push, you can provide a push-promise-handler with async requests:

```clojure
(import '[java.net.http HttpResponse$BodyHandlers])

;; Handle push promises
(let [my-client (client/build :version :HTTP_2)
      handler (fn [initiating-request push-request accept-fn]
                (println "Received push promise for:" (.uri push-request))
                ;; Accept the push promise by calling accept-fn with a BodyHandler
                (accept-fn HttpResponse$BodyHandlers/ofString))
      future (client/send-async my-client 
                 {:uri "https://example.com" :method :get}
                 (client/body-handler :string)
                 handler)]
  ;; future completes with the main response
  (.thenAccept future (fn [response] (println "Main response:" (:status response)))))
```

The push promise handler receives:
- `initiating-request` - the original `HttpRequest` that triggered the push
- `push-request` - the pushed `HttpRequest` from the server
- `accept-fn` - a function `(fn [body-handler] ...)` that returns a `CompletableFuture<HttpResponse>` to accept the push promise

## Error Handling

All functions may throw the following exceptions:

- `java.io.IOException` - If an I/O error occurs during the request
- `java.net.ConnectException` - If connection cannot be established
- `java.util.concurrent.TimeoutException` - If connection times out
- `java.lang.IllegalArgumentException` - If request is invalid

### Error Handling Example

```clojure
(try
  (http/get "https://example.com")
  (catch java.net.ConnectException e
    (println "Connection failed:" (.getMessage e)))
  (catch java.io.IOException e
    (println "IO error:" (.getMessage e))))
```

## Headers

Headers are specified as a Clojure map with keyword or string keys:

```clojure
;; Single value headers
{:content-type "application/json"
 :accept "application/json"
 :authorization "Bearer token123"}

;; Multiple headers
{:content-type "application/json"
 :accept "application/json"
 :user-agent "my-app/1.0"}
```

The response headers are returned in the same format:

```clojure
(let [response (http/get "https://example.com")]
  (get-in response [:headers :content-type]))  ;; => "application/json"
```

## Timeouts

Specify timeouts in milliseconds as an integer:

```clojure
;; 5 second timeout
(http/get "https://example.com/slow" {:timeout 5000})

;; Using java.time.Duration
(import '[java.time Duration])
(http/get "https://example.com/slow" {:timeout (Duration/ofSeconds 5)})
```

## Arity

All HTTP convenience functions support two arities:

1. **Single argument (URI only):**
   ```clojure
   (http/get "https://example.com")
   ```

2. **Two arguments (URI + request map):**
   ```clojure
   (http/get "https://example.com" {:headers {:accept "application/json"}})
   ```

All async functions also support the same two arities:

```clojure
;; Single argument
(http/get-async "https://example.com")

;; Two arguments
(http/post-async "https://example.com/api" {:body "data" :headers {...}})
```

## HTTP Versions

Specify the HTTP version using keywords:

```clojure
;; Use HTTP/1.1
(http/get "https://example.com" {:version :HTTP_1_1})

;; Use HTTP/2 (default)
(http/get "https://example.com" {:version :HTTP_2})
```

## License

Distributed under the MIT License. See LICENSE for details.
