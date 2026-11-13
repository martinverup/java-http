(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'io.github.martinverup/java-http)
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))

(def version "0.1.0")

(defn jar
  [{:keys [version] :or {version version}}]
  (let [jar-file (format "target/%s-%s.jar" (name lib) version)]
    (b/write-pom {:target "target" :lib lib :version version
                  :basis basis
                  :scm {:url "https://github.com/martinverup/java-http"
                        :connection "scm:git:git://github.com/martinverup/java-http.git"
                        :developerConnection "scm:git:ssh://git@github.com/martinverup/java-http.git"
                        :tag version}
                  :pom-data [[:url "https://github.com/martinverup/java-http"]
                             [:licenses
                              [:license
                               [:name "MIT License"]
                               [:url "https://opensource.org/licenses/MIT"]]]]})
    (b/copy-dir {:src-dirs ["src"] :target-dir class-dir})
    (b/copy-file {:src "target/pom.xml" :target (str class-dir "/META-INF/pom.xml")})
    (b/jar {:class-dir class-dir :jar-file jar-file})
    jar-file))

(defn install
  [{:keys [version] :or {version version}}]
  (let [jar-file (jar {:version version})]
    (b/install {:basis basis
                :lib lib
                :version version
                :jar-file jar-file
                :class-dir class-dir})))
