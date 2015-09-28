(ns mooncake.controller.feed
  (:require [mooncake.activity :as a]
            [mooncake.db.user :as user]
            [mooncake.helper :as mh]
            [mooncake.controller.customise-feed :as cfc]
            [mooncake.view.feed :as f]))

(defn retrieve-activities-from-user-sources [db active-activity-source-keys]
  (a/retrieve-activities db active-activity-source-keys))

(defn activity-src-preferences->feed-query [preferences-for-an-activity-src]
  (let [selected-types (map :id (filter :selected (:activity-types preferences-for-an-activity-src)))]
    (when-not (empty? selected-types)
      {"activity-src" (name (:id preferences-for-an-activity-src))
       "@type" selected-types})))

(defn generate-feed-query [feed-settings activity-sources]
  (remove nil? (map activity-src-preferences->feed-query (cfc/generate-activity-source-preferences activity-sources feed-settings))))

(defn feed [db request]
  (let [context (:context request)
        username (get-in request [:session :username])
        user (user/find-user db username)
        user-feed-settings (:feed-settings user)
        activity-sources (:activity-sources context)
        feed-query (generate-feed-query user-feed-settings activity-sources)
        activities (retrieve-activities-from-user-sources db feed-query)
        updated-context (assoc context :activities activities)]
    (mh/enlive-response (f/feed (assoc request :context updated-context)) (:context request))))