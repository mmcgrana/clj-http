(defproject clj-http "0.1.4"
  :description
    "A Clojure HTTP library wrapping the Apache HttpComponents client."
  :dependencies
    [[org.clojure/clojure "1.3.0"]
     [org.apache.httpcomponents/httpclient "4.0.3"]
     [commons-codec "1.4"]
     [commons-io "1.4"]]
  :dev-dependencies
    [[swank-clojure "1.3.2"]
     [ring/ring-jetty-adapter "0.3.11"]
     [ring/ring-devel "0.3.11"]
     [robert/hooke "1.1.2"]]
  :test-selectors {:default  #(not (:integration %))
                   :integration :integration
                   :all (constantly true)}
  :repositories {"releases"  "http://192.168.0.7:8080/archiva/repository/internal/"
                 "snapshots" "http://192.168.0.7:8080/archiva/repository/snapshots/"})
