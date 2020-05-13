(ns daily-log.core
  (:require [clojure.set :refer [rename-keys]]
            [clojure.data.json :as json]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as params]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor.chain :as chain]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [ring.middleware.session.memory :as memory]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as result-set]
            [next.jdbc :as jdbc]
            [hiccup.page :as hiccup]
            [java-time :as t]
            [clj-http.client :as http-client])
  (:import org.apache.commons.codec.binary.Base64
           java.security.KeyFactory
           java.security.spec.RSAPublicKeySpec
           java.security.Signature
           (org.bouncycastle.util BigIntegers))
  (:gen-class))

(extend-protocol result-set/ReadableColumn
  java.sql.Date
  (read-column-by-label ^java.time.LocalDate [^java.sql.Date v _]
    (keyword (.toString v)))
  (read-column-by-index ^java.time.LocalDate [^java.sql.Date v _2 _3]
    (keyword (.toString v))))

(def db {:dbtype "postgres" :dbname "daily_log"
         :stringtype "unspecified" ; HACK to support enums (needed for activity type)
         :host (System/getenv "DAILY_LOG_SERVER_PG_HOST")
         :user (System/getenv "DAILY_LOG_SERVER_PG_USER")
         :password (System/getenv "DAILY_LOG_SERVER_PG_PASSWORD")})

(def ds (jdbc/get-datasource db))

(defn read-json [s]
  (json/read-str s :key-fn keyword))

(defn base64->bytes [s]
  (Base64/decodeBase64 (.getBytes s "UTF-8")))

(defn base64->str [val]
  (String. (base64->bytes val)))

(defn base64->biginteger [val]
  (let [bs (base64->bytes val)]
    (BigIntegers/fromUnsignedByteArray bs)))

(defn str->base64 [s]
  (Base64/encodeBase64URLSafeString (.getBytes s)))

(defn jwt->payload [jwt]
  (-> (re-find #".+\.(.+)\." jwt)
      (get 1)
      base64->str
      read-json))

(defn as-unqualified-kebab-maps [rs opts]
  (let [kebab #(clojure.string/replace % #"_" "-")]
    (result-set/as-unqualified-modified-maps rs
                                             (assoc opts :label-fn kebab))))

(defn sql-log->log [m]
  (update m :activity-id (comp keyword str)))

(defn log->sql-log [m]
  (-> m
      (update :activity-id name)
      (update :date name)
      (rename-keys {:activity-id :activity_id})))

(defn sql-activity->activity [m]
  (-> m
      (update :type keyword)
      (update :id (comp keyword str))
      (update :user-id (comp keyword str))))

(defn get-user! [cognito-sub]
  (try
    [(-> (sql/find-by-keys ds
                           :users
                           {:cognito_sub cognito-sub}
                           {:builder-fn as-unqualified-kebab-maps})
         first)
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn insert-user! [cognito-sub]
  (try
    [(-> (sql/insert! ds
                      :users
                      {:cognito_sub cognito-sub}
                      {:builder-fn as-unqualified-kebab-maps}))
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn get-or-insert-user! [cognito-sub]
  (let [get-user-result (get-user! cognito-sub)]
    (if (some some? get-user-result)
      get-user-result
      (insert-user! cognito-sub))))

(defn insert-activity! [user activity]
  (try
    [(-> (sql/insert! ds
                      :activities
                      {:user_id (:id user)
                       :name (:name activity)
                       :type (name (:type activity))}
                      {:builder-fn as-unqualified-kebab-maps})
         sql-activity->activity)
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn get-activities-for-user! [user]
  (try
    [(->> (sql/find-by-keys ds
                            :activities
                            {:user_id (:id user)}
                            {:builder-fn as-unqualified-kebab-maps})
          (map sql-activity->activity))
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn update-log! [log]
  (try
    [(sql/update! ds
                  :logs
                  (select-keys log [:value])
                  (select-keys log [:date :activity_id])
                  {:return-keys true
                   :builder-fn as-unqualified-kebab-maps})
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn insert-log! [log]
  (try
    [(sql/insert! ds
                  :logs
                  log
                  {:builder-fn as-unqualified-kebab-maps})
     nil]
    (catch org.postgresql.util.PSQLException e
      [nil e])))

(defn upsert-log! [log]
  (let [sql-log (log->sql-log log)
        update-result (update-log! sql-log)
        result (if (some some? update-result)
                 update-result
                 (insert-log! sql-log))]
    [(some-> result first sql-log->log)
     (second result)]))

(defn get-logs-for-user! [user]
  (let [qry "SELECT activity_id, date, value
             FROM logs JOIN activities
             ON activity_id = id
             WHERE user_id = ?"]
    (try
      [(->> (sql/query ds [qry (:id user)] {:builder-fn as-unqualified-kebab-maps})
            (map sql-log->log))
       nil]
      (catch org.postgresql.util.PSQLException e
        [nil e]))))

(def insert-activity
  {:name :insert-activity
   :enter
   (fn [{:keys [request user] :as context}]
     (let [edn-params (:edn-params request)
           [result err] (insert-activity! user edn-params)]
       (if (some? err)
         (assoc context :response {:status 500 :body (.getMessage err)})
         (assoc context :response {:status 200 :body result}))))})

(def get-activities
  {:name :get-activities
   :enter
   (fn [{:keys [user] :as context}]
     (let [[result err] (get-activities-for-user! user)]
       (if (some? err)
         (assoc context :response {:status 500 :body (.getMessage err)})
         (assoc context :response {:status 200 :body result}))))})

(def upsert-log
  {:name :upsert-log
   :enter
   (fn [{:keys [request] :as context}]
     (let [edn-params (:edn-params request)
           [result err] (upsert-log! edn-params)]
       (if (some? err)
         (assoc context :response {:status 500 :body (.getMessage err)})
         (assoc context :response {:status 200 :body result}))))})

(def get-logs
  {:name :get-logs
   :enter
   (fn [{:keys [user] :as context}]
     (let [[result err] (get-logs-for-user! user)]
       (if (some? err)
         (assoc context :response {:status 500 :body (.getMessage err)})
         (assoc context :response {:status 200 :body result}))))})

(def token-auth-header
  (str "Basic "
       (str->base64 (str (System/getenv "DAILY_LOG_SERVER_COGNITO_CLIENT_ID")
                      ":"
                      (System/getenv "DAILY_LOG_SERVER_COGNITO_CLIENT_SECRET")))))

(defn jwk->public-key [{:keys [n e] :as jwk}]
  (let [kf (KeyFactory/getInstance "RSA")
        n (base64->biginteger n)
        e (base64->biginteger e)]
    (.generatePublic kf (RSAPublicKeySpec. n e))))

(defn get-public-key! [jwt]
  (let [kid (-> jwt
                (clojure.string/split #"\.")
                first
                base64->str
                read-json
                :kid)
        url (str "https://cognito-idp.us-east-1.amazonaws.com/"
                 (System/getenv "DAILY_LOG_SERVER_COGNITO_USER_POOL_ID")
                 "/.well-known/jwks.json")
        {:keys [body status]} (http-client/get url {:throw-exceptions false})]
    (if (= status 200)
      (let [jwks (->> body
                      read-json
                      :keys
                      (map #(vector (:kid %) %))
                      (into {}))
            jwk (get jwks kid)]
        (if jwk
          (jwk->public-key jwk)
          (println "No matching jwk found" kid)))
      (println "Non-200 from public key endpoint" status))))

(defn signature-verified? [public-key jwt]
  (let [[_ input signature] (->> jwt
                                 (re-find #"(.+\..+)\.(.+)")
                                 (map #(.getBytes % "UTF-8")))]
    (-> (doto (Signature/getInstance "SHA256withRSA" "BC")
          (.initVerify public-key)
          (.update input))
        (.verify (Base64/decodeBase64 signature)))))

(defn correct-claims? [jwt]
  (= [(System/getenv "DAILY_LOG_SERVER_COGNITO_CLIENT_ID")
      (str "https://cognito-idp.us-east-1.amazonaws.com/"
           (System/getenv "DAILY_LOG_SERVER_COGNITO_USER_POOL_ID"))
      "id"]
     (-> (jwt->payload jwt)
         ((juxt :aud :iss :token_use)))))

(defn get-tokens! [code]
  (let [params {:grant_type "authorization_code"
                :code code
                :client_id (System/getenv "DAILY_LOG_SERVER_COGNITO_CLIENT_ID")
                :redirect_uri "http://localhost:8890"}
        {:keys [status body]} (http-client/post "https://auth.spacetrumpet.co.uk/oauth2/token"
                                                {:headers {"Authorization" token-auth-header}
                                                 :throw-exceptions false
                                                 :form-params params})
        {:keys [id_token refresh_token expires_in]} (read-json body)]
    (if (= status 200)
      (if-let [public-key (get-public-key! id_token)]
        (if (correct-claims? id_token)
          (if (signature-verified? public-key id_token)
            {:id-token id_token
             :refresh-token refresh_token
             :expiry (t/plus (t/instant) (t/seconds expires_in))}
            (println "Signature not verified"))
          (println "Incorrect claims"))
        (println "No matching public-key found"))
      (println "Non-200 from token endpoint" status))))

(defn refresh-tokens! [refresh-token]
  (let [params {:grant_type "refresh_token"
                :refresh_token refresh-token
                :client_id (System/getenv "DAILY_LOG_SERVER_COGNITO_CLIENT_ID")}
        {:keys [status body]} (http-client/post "https://auth.spacetrumpet.co.uk/oauth2/token"
                                                {:headers {"Authorization" token-auth-header}
                                                 :throw-exceptions false
                                                 :form-params params})
        {:keys [id_token expires_in]} (json/read-str body :key-fn keyword)]
    (if (= status 200)
      (if-let [public-key (get-public-key! id_token)]
        (if (correct-claims? id_token)
          (if (signature-verified? public-key id_token)
            {:id-token id_token
             :refresh-token refresh-token
             :expiry (t/plus (t/instant) (t/seconds expires_in))}
            (println "Signature not verified"))
          (println "Incorrect claims"))
        (println "No matching public-key found"))
      (println "Non-200 from token endpoint" status))))

(def redirect-to-login-response
  {:status 302
   :headers {"Location" (str "https://auth.spacetrumpet.co.uk/login?client_id="
                             (System/getenv "DAILY_LOG_SERVER_COGNITO_CLIENT_ID")
                             "&response_type=code"
                             "&scope=aws.cognito.signin.user.admin+email+openid+phone+profile"
                             "&redirect_uri=http://localhost:8890")}})

(defn successful-login-response [tokens]
  {:status 302
   :session {:tokens tokens}
   :headers {"Location" "http://localhost:8890/"}})

(def login
  {:name :login
   :enter
   (fn [{{{:keys [tokens]} :session
         {:keys [code]} :query-params}
        :request :as context}]
     (if tokens
       context
       (if-let [tokens (and code (get-tokens! code))]
         (assoc context :response (successful-login-response tokens))
         (assoc context :response redirect-to-login-response))))})

(def authenticate
  {:name :authenticate
   :enter
   (fn [context]
     (let [{:keys [id-token refresh-token expiry]} (get-in context [:request :session :tokens])
           payload (and id-token
                        (jwt->payload id-token))]
       (cond
         (not (:sub payload))
         (-> (assoc context :response {:status 401 :body "No sub"})
             chain/terminate)

         (not (t/before? (t/instant) expiry))
         (if-let [{:keys [id-token] :as tokens} (refresh-tokens! refresh-token)]
           (let [payload (jwt->payload id-token)
                 [user err] (get-or-insert-user! (:sub payload))]
             (if (some? err)
               (-> (assoc context :response {:status 500 :body (.getMessage err)})
                   chain/terminate)
               (-> context
                   (assoc :user user)
                   (assoc-in [:request :session :tokens] tokens))))
           (-> (assoc context :response {:status 401 :body "Couldn't refresh tokens"})
               chain/terminate))

         :else
         (let [[user err] (get-or-insert-user! (:sub payload))]
           (if (some? err)
             (-> (assoc context :response {:status 500 :body (.getMessage err)})
                 chain/terminate)
             (assoc context :user user))))))
   :leave
   (fn [{{session :session} :request :as context}]
     (assoc-in context [:response :session] session))})

(def index
  {:name :index
   :enter
   (fn [context]
     (let [page (hiccup/html5
                 [:head
                  [:link {:href "https://fonts.googleapis.com/icon?family=Material+Icons"
                          :rel "stylesheet"}]
                  [:link {:href "https://cdnjs.cloudflare.com/ajax/libs/materialize/1.0.0/css/materialize.min.css"
                          :rel "stylesheet"
                          :type "text/css"}]
                  [:link {:href "css/style.css"
                          :rel "stylesheet"
                          :type "text/css"}]]
                 [:body.grey.lighten-2 {:style {:height "100%" :overflow "hidden"}}
                  [:div {:id "app"}]
                  [:script {:src "cljs-out/client-main.js"}]])]
       (assoc context :response {:headers {"Content-Type" "text/html"}
                                 :status 200
                                 :body page})))})

(def body-parser (params/body-params))
(def session-interceptor (middlewares/session (merge {:store (memory/memory-store)}
                                                     (when (= "Y" (System/getenv "DAILY_LOG_SERVER_HTTPS_ENABLED"))
                                                       {:cookie-attrs {:secure true}}))))

(def routes
  (route/expand-routes
   #{["/" :get [session-interceptor login authenticate index]]
     ["/activities" :post [body-parser session-interceptor authenticate insert-activity]]
     ["/activities" :get [session-interceptor authenticate get-activities]]
     ["/logs" :post [body-parser session-interceptor authenticate upsert-log]]
     ["/logs" :get [session-interceptor authenticate get-logs]]}))

(def service-map
  {::http/routes routes
   ::http/type   :jetty
   ::http/port   8890
   ::http/secure-headers {:content-security-policy-settings "object-src 'none'; script-src 'self' 'unsafe-eval' 'unsafe-inline'"}
   ::http/resource-path "/public"})

(defn -main []
  (http/start (http/create-server service-map)))
