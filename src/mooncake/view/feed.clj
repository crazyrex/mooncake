(ns mooncake.view.feed
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.routes :as routes]
            [mooncake.helper :as mh]
            [mooncake.view.view-helpers :as vh]
            [mooncake.domain.activity :as domain]
            [mooncake.translation :as translation]))

(def max-characters-in-title 140)

(defn activity-source-index [activity activity-sources]
  (let [activity-src (keyword (domain/activity->activity-src activity))]
    (get-in activity-sources [activity-src :index])))

(defn activity-source-class [activity activity-sources]
  (format "activity-src-%s" (activity-source-index activity activity-sources)))

(defn activity-action-message-translation [activity-action-key]
  (case activity-action-key
    :objective "feed/action-text-objective"
    :question "feed/action-text-question"
    nil))

(defn activity-action-message-map [text]
  (when text (str "content:" text)))

(defn generate-activity-stream-items [enlive-m activities activity-sources]
  (let [activity-stream-item (html/select enlive-m [[:.clj--activity-item html/first-of-type]])
        library-m (vh/load-template "public/library.html")
        activity-stream-item-untrusted-source-snippet (first (html/select library-m [:.clj--activity-item__suspicious--untrusted-source]))
        activity-stream-item-unverified-signature-snippet (first (html/select library-m [:.clj--activity-item__suspicious--unverified-signature]))
        original-activity-src-class (re-find #"activity-src-\d+" (-> activity-stream-item first :attrs :class))]
    (html/at activity-stream-item [html/root]
             (html/clone-for [activity activities]
                             [:.clj--activity-item] (html/do->
                                                      (html/remove-class original-activity-src-class)
                                                      (html/add-class (activity-source-class activity activity-sources)))
                             [:.clj--avatar__initials] (html/content (-> (domain/activity->actor-display-name activity)
                                                                         first str clojure.string/upper-case))
                             [:.clj--activity-item__link] (html/do->
                                                            (html/set-attr :href (domain/activity->object-url activity))
                                                            (html/content (vh/limit-text-length-if-above max-characters-in-title (domain/activity->object-display-name activity))))
                             [:.clj--activity-item__time] (let [activity-time (domain/activity->published activity)]
                                                            (html/do->
                                                              (html/set-attr :datetime activity-time)
                                                              (html/content (when activity-time
                                                                              (mh/humanise-time activity-time)))))
                             [:.clj--activity-item__action__author] (html/content (domain/activity->actor-display-name activity))
                             [:.clj--activity-item__action] (let [action-text-key (domain/activity->action-text-key activity)]
                                                              (if (= :default action-text-key)
                                                                (html/content (domain/activity->default-action-text activity))
                                                                (html/set-attr :data-l8n (activity-action-message-map (activity-action-message-translation action-text-key)))))
                             [:.clj--activity-item__suspicious] (let [action-signed (domain/activity->signed activity)]
                                                                  (case action-signed
                                                                    (or false nil) (html/do->
                                                                                     (html/substitute activity-stream-item-untrusted-source-snippet)
                                                                                     (html/remove-class "clj--STRIP"))
                                                                    "verification-failed" (html/do->
                                                                                            (html/substitute activity-stream-item-unverified-signature-snippet)
                                                                                            (html/remove-class "clj--STRIP"))
                                                                    nil))
                             [:.clj--activity-item__id] (html/content (domain/activity->insert-id activity))
                             [:.clj--activity-item__target] (html/do->
                                                              (html/set-attr :href (domain/activity->target-url activity))
                                                              (html/content (domain/activity->target activity)))))))

(defn add-activities [enlive-m activities activity-sources]
  (let [activity-stream-items (generate-activity-stream-items enlive-m activities activity-sources)]
    (html/at enlive-m [:.clj--activity-stream]
             (html/content activity-stream-items))))

(defn add-no-active-activity-sources-message [enlive-m]
  (html/at enlive-m
           [:.clj--activity-stream] (html/content (-> (vh/load-template "public/library.html")
                                                      (html/select [:.clj--empty-activity-item])
                                                      first))
           [:.clj--empty-stream__link] (html/set-attr :href (routes/path :show-customise-feed))))

(defn render-sign-out-link [enlive-m signed-in?]
  (if signed-in?
    (html/at enlive-m [:.clj--sign-out__link] (html/do->
                                                (html/remove-class "clj--STRIP")
                                                (html/set-attr :href (routes/path :sign-out))))
    enlive-m))

(defn render-customise-feed-link [enlive-m signed-in?]
  (if signed-in?
    (html/at enlive-m [:.clj--customise-feed__link] (html/do->
                                                      (html/remove-class "clj--STRIP")
                                                      (html/set-attr :href (routes/path :show-customise-feed))))
    enlive-m))

(defn render-username [enlive-m username]
  (html/at enlive-m [:.clj--username] (html/content username)))

(defn render-activity-stream [enlive-m activities activity-sources]
  (if (empty? activities)
    (add-no-active-activity-sources-message enlive-m)
    (add-activities enlive-m activities activity-sources)))

(defn feed-path-url-with-page-number [value]
  (str (routes/path :feed) "?page-number=" value))

(defn render-newer-activities-link [enlive-m page-number]
  (let [page-number-not-null (or page-number 1)
        dec-page-number (dec page-number-not-null)]
    (if (= page-number-not-null 1)
      (html/at enlive-m [:.clj--newer-activities__link] (html/do->
                                                          (html/add-class "clj--STRIP")))
      (html/at enlive-m [:.clj--newer-activities__link] (html/do->
                                                          (html/set-attr :href (feed-path-url-with-page-number dec-page-number)))))))

(defn render-older-activities-link [enlive-m is-last-page? page-number]
  (let [page-number-not-null (or page-number 1)
        inc-page-number (inc page-number-not-null)]
    (if is-last-page?
      (html/at enlive-m [:.clj--older-activities__link] (html/do->
                                                          (html/add-class "clj--STRIP")))
      (html/at enlive-m [:.clj--older-activities__link] (html/do->
                                                          (html/set-attr :href (feed-path-url-with-page-number inc-page-number)))))))

(defn hide-new-activity-fragments [enlive-m hide-activities?]
  (if hide-activities?
    (html/at enlive-m [:.clj--activity-item] (html/do->
                                               (html/add-class "hidden-new-activity")))
    enlive-m))

(defn feed-fragment [request]
  (let [activities (get-in request [:context :activities])
        activity-sources (get-in request [:context :activity-sources])
        hide-activities? (get-in request [:context :hide-activities?])]
    (-> (vh/load-template "public/feed.html")
        (generate-activity-stream-items activities activity-sources)
        (hide-new-activity-fragments hide-activities?))))

(defn feed [request]
  (let [activities (get-in request [:context :activities])
        activity-sources (get-in request [:context :activity-sources])]
    (-> (vh/load-template-with-lang "public/feed.html" (translation/get-locale-from-request request))
        (render-username (get-in request [:session :username]))
        (render-customise-feed-link (mh/signed-in? request))
        (render-sign-out-link (mh/signed-in? request))
        (render-activity-stream activities activity-sources)
        (render-older-activities-link (get-in request [:context :is-last-page])
                                      (get-in request [:params :page-number]))
        (render-newer-activities-link (get-in request [:params :page-number]))
        (vh/add-script "js/main.js"))))