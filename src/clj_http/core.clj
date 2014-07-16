(ns clj-http.core
  "Core HTTP request/response implementation."
  (:import (org.apache.http HttpRequest HttpEntityEnclosingRequest HttpResponse Header StatusLine))
  (:import (org.apache.http.params HttpParams))
  (:import (org.apache.http.util EntityUtils))
  (:import (org.apache.http.entity ByteArrayEntity))
  (:import (org.apache.http.client HttpClient))
  (:import (org.apache.http.client.methods HttpGet HttpHead HttpPut HttpPost HttpDelete))
  (:import (org.apache.http.client.params CookiePolicy ClientPNames))
  (:import (org.apache.http.impl DefaultConnectionReuseStrategy))
  (:import (org.apache.http.impl.client DefaultConnectionKeepAliveStrategy DefaultHttpClient))
  (:import (org.apache.http.conn ClientConnectionManager))
  (:import (org.apache.http.impl.conn SingleClientConnManager)))

(defn- parse-headers [^HttpResponse http-resp]
  (into {} (map (fn [^Header h] [(.toLowerCase (.getName h)) (.getValue h)])
                (iterator-seq (.headerIterator http-resp)))))

(def ^:dynamic *conn-pool-ctx*
  {:connection-header   "close"
   :reuse-strategy      (DefaultConnectionReuseStrategy.)
   :keep-alive-strategy (DefaultConnectionKeepAliveStrategy.)})

(defn- mk-http-client
  "Make HttpClient Instance according to the *conn-pool-ctx* spec"
  [{manager             :manager
    params              :params
    reuse-strategy      :reuse-strategy
    keep-alive-strategy :keep-alive-strategy}]
  (doto (DefaultHttpClient. manager params)
    (.setReuseStrategy reuse-strategy)
    (.setKeepAliveStrategy keep-alive-strategy)))

(defn- afterExecute
  "Shutdown the connection manager if required"
  [^ClientConnectionManager conn-mgr]
  (if (isa? conn-mgr SingleClientConnManager)
    (.shutdown conn-mgr)))

(defn- setCookyPolicy [^HttpParams params]
  (.setParameter params ClientPNames/COOKIE_POLICY CookiePolicy/BROWSER_COMPATIBILITY))

(defn request
  "Executes the HTTP request corresponding to the given Ring request map and
   returns the Ring response map corresponding to the resulting HTTP response.

   Note that where Ring uses InputStreams for the request and response bodies,
   the clj-http uses ByteArrays for the bodies."
  [{:keys [request-method scheme server-name server-port uri query-string
           headers content-type character-encoding body]}]
  (let [^HttpClient http-client (mk-http-client *conn-pool-ctx*)
        [connection-header-v] (-> (select-keys *conn-pool-ctx* [:connection-header]) vals reverse)]
    (try
      (-> http-client
        (.getParams)
        (setCookyPolicy))
      (let [http-url (str scheme "://" server-name
                          (if server-port (str ":" server-port))
                          uri
                          (if query-string (str "?" query-string)))
              ^HttpRequest
              http-req (case request-method
                         :get    (HttpGet. http-url)
                         :head   (HttpHead. http-url)
                         :put    (HttpPut. http-url)
                         :post   (HttpPost. http-url)
                         :delete (HttpDelete. http-url))]
        (if (and content-type character-encoding)
          (.addHeader http-req "Content-Type"
                      (str content-type "; charset=" character-encoding)))
        (if (and content-type (not character-encoding))
          (.addHeader http-req "Content-Type" content-type))
        (.addHeader http-req "Connection" connection-header-v)
        (doseq [[header-n header-v] headers]
          (.addHeader http-req header-n header-v))
        (if body
          (let [http-body (ByteArrayEntity. body)]
            (.setEntity ^HttpEntityEnclosingRequest http-req http-body)))
        (let [^HttpResponse http-resp (.execute http-client http-req)
              http-entity (.getEntity http-resp)
              resp {:status (.getStatusCode ^StatusLine (.getStatusLine http-resp))
                    :headers (parse-headers http-resp)
                    :body (if http-entity (EntityUtils/toByteArray http-entity))}]
          (afterExecute (.getConnectionManager http-client))
          resp)))))
