(ns mooncake.browser.journey-test
  (:require [midje.sweet :refer :all]
            [clj-webdriver.taxi :as wd]
            [clojure.test :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [mooncake.test.test-helpers.db :as dbh]
            [mooncake.handler :as h]
            [mooncake.integration.kerodon :as kero]
            [mooncake.activity :as a]
            [mooncake.config :as config]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

(def localhost "localhost:5439")

(defn wait-for-selector [selector]
  (try
    (wd/wait-until #(not (empty? (wd/css-finder selector))) 5000)
    (catch Exception e
      (prn (str ">>>>>>>>>> Selector could not be found: " selector))
      (prn "==========  PAGE SOURCE ==========")
      (prn (wd/page-source))
      (prn "==========  END PAGE SOURCE ==========")
      (throw e))))

(def test-port 5439)

(def mooncake-feed-body ".func--feed-page")

(def stub-config-map
  {:port          5439
   :host          "localhost"
   :base-url      "http://localhost:5439"
   :secure        "false"
   :client-id     "fake stonecutter client id"
   :client-secret "fake stonecutter client secret"
   :auth-url      "fake stonecutter auth url"
   :mongo-uri     "mongodb://localhost:27017/mooncake-dev"
   :stub-user     "MRS STUBBY"})

(def test-store
  (atom (dbh/create-in-memory-store)))

(defn start-server []
  (let [activity-sources (a/load-activity-sources-from-resource "test-activity-sources.yml")
        app-routes (h/create-site-app stub-config-map @test-store activity-sources)]
    (loop [server (run-jetty app-routes {:port test-port :host "localhost" :join? false})]
      (if (.isStarted server)
        server
        (recur server)))))

(defn stop-server [server]
  (.stop server))

(defn start-browser []
  (wd/set-driver! {:browser :firefox}))

(defn stop-browser []
  (wd/quit))

(def server (atom {}))

(defn count-activity-items []
  (count (wd/elements ".func--activity-item")))

(defn wait-and-count [count]
  (try (wd/wait-until #(= count (count-activity-items)) 5000)
       (catch Exception e
         (throw e))))

(def screenshot-directory "test/mooncake/browser/screenshots")
(def screenshot-number (atom 0))
(defn screenshot [filename]
  (log/info (str "Screenshot: " filename))
  (wd/take-screenshot :file (str screenshot-directory "/"
                                 (format "%02d" (swap! screenshot-number + 1))
                                 "_" filename ".png")))

(defn clear-screenshots []
  (doall (->> (io/file screenshot-directory)
              file-seq
              (filter #(re-matches #".*\.png$" (.getName %)))
              (map io/delete-file))))

(against-background
  [(before :contents (do (reset! server (start-server))
                         (kero/create-dummy-activities @test-store (* 2 config/activities-per-page))
                         (clear-screenshots)
                         (start-browser)))
   (after :contents (do
                      (stop-browser)
                      (stop-server @server)))]

  (try
    (facts "about feedpage" :browser
           (wd/to (str localhost "/d-cent-sign-in"))
           (wd/current-url) => (contains (str localhost "/"))
           (wait-for-selector mooncake-feed-body)
           (screenshot "feed_page")
           (count-activity-items) => config/activities-per-page

           (fact "pagination buttons are removed when javascript is enabled"
                 (wd/css-finder ".func--newer-activities__link") => empty?
                 (wd/css-finder ".func--older-activities__link") => empty?)

           (fact "more activities are loaded when page is scrolled to the bottom"
                 (let [page-length (:height (wd/element-size "body"))]
                   (wd/execute-script (str "scroll(0, " page-length ");"))
                   (screenshot "loading_more_activites_spinner")
                   (wait-and-count (* 2 config/activities-per-page))
                   (count-activity-items) => (* 2 config/activities-per-page)))
           (fact "new activities are loaded when new activities exist and update is triggered"
                 (wd/exists? ".show-new-activities__link") => false
                 (kero/create-dummy-activity @test-store "2015-10-10T10:20:30.000Z")
                 (wait-for-selector ".show-new-activities__link")))
    (catch Exception e
      (throw e))))