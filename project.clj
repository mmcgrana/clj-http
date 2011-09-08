(defproject org.clojars.maxweber/clj-http "0.1.3-SNAPSHOT"
  :description "A Clojure HTTP library wrapping the Apache HttpComponents client."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.apache.httpcomponents/httpclient "4.1"]
                 [org.apache.httpcomponents/httpcore "4.1"]
                 [commons-codec "1.4"]
                 [commons-io "1.4"]]
  :dev-dependencies [[ring/ring-jetty-adapter "0.3.5"]
                     [ring/ring-devel "0.3.5"]])

