(ns mooncake.db.activity
  (:require [mooncake.domain.activity :as domain]
            [mooncake.db.mongo :as mongo]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]
            [mooncake.config :as config]))

(def activity-collection "activity")
(def activity-metadata-collection "activityMetaData")

(defn fetch-activity-types [store]
  (let [all-activity-metadata (mongo/fetch-all store activity-metadata-collection)]
    (reduce
      (fn [activity-types-m activity-metadata]
        (assoc activity-types-m (:activity-src activity-metadata) (:activity-types activity-metadata)))
      {}
      all-activity-metadata)))

(defn update-activity-types-for-activity-source! [store activity-src activity-type]
  (mongo/add-to-set! store activity-metadata-collection {:activity-src activity-src} :activity-types activity-type))

(defn activity->published-datetime [activity]
  (-> (domain/activity->published activity)
      (time-coerce/from-string)))

(defn store-most-recent-activity-date! [store activity-src date]
  (mongo/upsert! store activity-metadata-collection {:activity-src activity-src} :latest-activity-datetime date))

(defn fetch-most-recent-activity-date [store activity-src]
  (when-let [item (mongo/find-item store activity-metadata-collection {:activity-src activity-src})]
    (-> item
        :latest-activity-datetime
        (time-coerce/from-string))))

(defn fetch-activities [store]
  (mongo/fetch-all store activity-collection))

(defn fetch-activities-by-activity-sources-and-types [store activity-sources-and-types options-m]
  (mongo/find-items-by-alternatives store activity-collection activity-sources-and-types (merge {:sort {:published :descending} :limit config/activities-per-page} options-m)))

(defn store-activity! [store activity]
  (let [activity-src (domain/activity->activity-src activity)
        activity-type (domain/activity->type activity)
        most-recent-activity-date (fetch-most-recent-activity-date store activity-src)
        current-activity-date (activity->published-datetime activity)
        current-activity-date-string (domain/activity->published activity)]
    (when (or (not most-recent-activity-date) (time/after? current-activity-date most-recent-activity-date))
      (do
        (update-activity-types-for-activity-source! store activity-src activity-type)
        (store-most-recent-activity-date! store activity-src current-activity-date-string)
        (mongo/store! store activity-collection activity)))))

(defn fetch-total-count-by-sources-and-types [store activity-source-keys]
  (mongo/fetch-total-count-by-query store activity-collection activity-source-keys))
