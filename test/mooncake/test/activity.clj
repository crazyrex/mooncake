(ns mooncake.test.activity
  (:require [midje.sweet :refer :all]
            [clj-http.client :as http]
            [mooncake.activity :as a]))


(def ten-oclock "2015-01-01T10:00:00.000Z")
(def eleven-oclock "2015-01-01T11:00:00.000Z")
(def twelve-oclock "2015-01-01T12:00:00.000Z")

(fact "retrieve activities retrieves activities from multiple sources, sorts them by published time and assocs activity source into each activity"
      (let [an-activity-src-url "https://an-activity.src"
            another-activity-src-url "https://another-activity.src"]
        (a/retrieve-activities {:an-activity-src an-activity-src-url
                                :another-activity-src another-activity-src-url}) => [{:activity-src :an-activity-src
                                                                                      "actor" {"displayName" "KCat"}
                                                                                      "published" twelve-oclock}
                                                                                     {:activity-src :another-activity-src
                                                                                      "actor" {"displayName" "LSheep"}
                                                                                      "published" eleven-oclock}
                                                                                     {:activity-src :an-activity-src
                                                                                      "actor" {"displayName" "JDog"}
                                                                                      "published" ten-oclock}]
        (provided
          (http/get an-activity-src-url {:accept :json
                                         :as :json-string-keys})  => {:body [{"actor" {"displayName" "JDog"}
                                                                              "published" ten-oclock}
                                                                             {"actor" {"displayName" "KCat"}
                                                                              "published" twelve-oclock}]}
          (http/get another-activity-src-url {:accept :json
                                              :as :json-string-keys}) => {:body [{"actor" {"displayName" "LSheep"}
                                                                                  "published" eleven-oclock}]})))

(fact "can load activity sources from a resource"
      (a/load-activity-sources "test-activity-sources.yml") => {:test-activity-source-1 "https://test-activity.src/activities"
                                                                :test-activity-source-2 "https://another-test-activity.src"})

(fact "get-json-from-activity-source gracefully handles exceptions caused by bad/missing responses"
      (a/get-json-from-activity-source ...invalid-activity-src-url...) => nil
      (provided
        (http/get ...invalid-activity-src-url...
                  {:accept :json :as :json-string-keys}) =throws=> (java.net.ConnectException.)))