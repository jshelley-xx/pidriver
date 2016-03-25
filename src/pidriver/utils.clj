(ns pidriver.utils
  (:require [clojure.java.io :refer :all])
  (:require [clj-http.client :as client])
  (:require [cheshire.core :refer :all])
  (:require [clojure.string :refer [join]])
  (:require [cemerick.url :refer (url url-encode)])
  )

(def category "pidriver.utils")
(def LOG (org.apache.logging.log4j.LogManager/getLogger category))


(defn get-forex-home []
  (System/getProperty "forex.home"))

(defn multisplitter [a & xs] (mapv class (into [a] xs)))

(defn pp-to-s [o]
  (let [w (java.io.StringWriter.)] (clojure.pprint/pprint o w)(.toString w)))


(def settings (atom {}))

(defn- load-settings [cat]
  (try
    (read-string (slurp cat))
  (catch Exception e {})))

(defn- save-settings [cat m]
  (make-parents cat)
  (spit cat (with-out-str (pr m))))


(defn reset-settings []
  (reset! settings {}))


(defn get-setting [cat l]
  (if (not (get @settings cat))
    (swap! settings assoc cat (load-settings cat)))
  (get-in @settings (into [cat] l) nil))

(defn set-setting [cat l v]
  (swap! settings assoc-in (into [cat] l) v)
  (save-settings cat (get @settings cat)))

(defn set-all-settings [cat l]
  (doall (map #(swap! settings assoc-in (into [cat] (first %)) (second %)) l))
  (save-settings cat (get @settings cat)))

(defn get-settings-cat [cat]
  (str (get-forex-home) "/data/config/" cat))

(defn get-app-settings-cat [cat]
  (str (get-forex-home) "/config/" cat ))


; looks up data data/config (working) folder, and returns
;from the more permanent config folder if not found
(defn get-setting-fallback-to-app-setting [cat]
  (fn get-it
    ([l1]
      (get-it l1 l1))
    ([l1 l2]
    (.debug LOG (str "fallbac: " l1 " : " l2))
    (let [s (get-setting (get-settings-cat cat) l1)]
      (if (some? s)
      s
      (get-setting (get-app-settings-cat cat) l2))))))


(defn get-json
  ([url-to-use]
    (.debug LOG (str "fetching: " url-to-use ))
    (let [
      response (client/get url-to-use {})
      status (:status response)
      body (:body response)
      ]
      (.debug LOG (str "\treceived " (count body) " byte response"))
      (if (= 200 status)
          (do
            (.trace LOG (str "\tresponse " body))
            (parse-string body))
          (do
            (.warn LOG (str "non-200 status: " status ", response: " (:body response)))
            (parse-string "{}")
            ))))
  ([s f]
    (f (get-json s))))



(defn post-json
  ([url-to-use j]
    (.debug LOG (str "posting: " url-to-use ))
    (let [
      response (client/post url-to-use {:form-params j
                                        :content-type :json})
      status (:status response)
      body (:body response)
      ]
      (.debug LOG (str "\treceived " (count body) " byte response"))
      (if (= 200 status)
          (do
            (.trace LOG (str "\tresponse " body))
            (parse-string body))
          (do
            (.warn LOG (str "non-200 status: " status ", response: " (:body response)))
            (parse-string "{}")
            ))))
  ([s j f]
    (f (post-json s j))))
