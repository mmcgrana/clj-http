(ns clj-http.core-test
  (:use clojure.test)
  (:require [clojure.pprint :as pp])
  (:require [clj-http.core :as core])
  (:require [clj-http.util :as util])
  (:require [ring.adapter.jetty :as ring]))

(defn handler [req]
  (pp/pprint req)
  (println) (println)
  (condp = [(:request-method req) (:uri req)]
    [:get "/get"]
      {:status 200 :body "get"}
    [:head "/head"]
      {:status 200}
    [:get "/content-type"]
      {:status 200 :body (:content-type req)}
    [:get "/header"]
      {:status 200 :body (get-in req [:headers "x-my-header"])}
    [:get "/response-header"]
      {:status 200 :body "check header" :headers {"X-My-Header" "header-val"}}
    [:post "/post"]
      {:status 200 :body (slurp (:body req))}
    [:get "/redirect"]
      {:status 301 :body "redirecting" :headers {"Location" "/get"}}
    [:get "/error"]
      {:status 500 :body "o noes"}))

(defn run-server
  []
  (defonce server
    (future (ring/run-jetty handler {:port 18080}))))

(def base-req
  {:scheme "http"
   :server-name "localhost"
   :server-port 18080})

(defn request [req]
  (core/request (merge base-req req)))

(defn slurp-body [req]
  (slurp (:body req)))

(deftest ^{:integration true} makes-get-request
  (run-server)
  (let [resp (request {:request-method :get :uri "/get"})]
    (is (= 200 (:status resp)))
    (is (= "get" (slurp-body resp)))))

(deftest ^{:integration true} makes-head-request
  (run-server)
  (let [resp (request {:request-method :head :uri "/head"})]
    (is (= 200 (:status resp)))
    (is (nil? (:body resp)))))

(deftest ^{:integration true} sets-content-type-with-charset
  (run-server)
  (let [resp (request {:request-method :get :uri "/content-type"
                       :content-type "text/plain" :character-encoding "UTF-8"})]
    (is (= "text/plain; charset=UTF-8" (slurp-body resp)))))

(deftest ^{:integration true} sets-content-type-without-charset
  (run-server)
  (let [resp (request {:request-method :get :uri "/content-type"
                       :content-type "text/plain"})]
    (is (= "text/plain" (slurp-body resp)))))

(deftest ^{:integration true} sets-arbitrary-headers
  (run-server)
  (let [resp (request {:request-method :get :uri "/header"
                       :headers {"X-My-Header" "header-val"}})]
    (is (= "header-val" (slurp-body resp)))))

(deftest ^{:integration true} returns-lowercase-headers
  (run-server)
  (let [resp (request {:request-method :get :uri "/response-header"})]
    (is (= "header-val" (get-in resp [:headers "x-my-header"])))))

(deftest ^{:integration true} sends-and-returns-byte-array-body
  (run-server)
  (let [resp (request {:request-method :post :uri "/post"
                       :body (util/utf8-bytes "contents")})]
    (is (= 200 (:status resp)))
    (is (= "contents" (slurp-body resp)))))

(deftest ^{:integration true} returns-arbitrary-headers
  (run-server)
  (let [resp (request {:request-method :get :uri "/get"})]
    (is (string? (get-in resp [:headers "date"])))))

(deftest ^{:integration true} returns-redirect-status
  (run-server)
  (let [resp (request {:request-method :get :uri "/redirect"})]
    (is (= 301 (:status resp)))))

(deftest ^{:integration true} returns-status-on-exceptional-responses
  (run-server)
  (let [resp (request {:request-method :get :uri "/error"})]
    (is (= 500 (:status resp)))))
