(ns mooncake.test.middleware
  (:require [midje.sweet :refer :all]
            [mooncake.middleware :as m]))

(defn example-handler [request]
  "return value")

(defn wrap-function [handler]
  (fn [request] "wrap function return value"))

(def handlers {:handler-1 example-handler
               :handler-2 example-handler
               :handler-3 example-handler})

(facts "about wrap-handlers"
      (fact "wrap handlers wraps all handlers in a wrap-function"
            (let [wrapped-handlers (m/wrap-handlers handlers wrap-function nil)]
              ((:handler-1 wrapped-handlers) "request") => "wrap function return value"
              ((:handler-2 wrapped-handlers) "request") => "wrap function return value"
              ((:handler-3 wrapped-handlers) "request") => "wrap function return value"))

      (fact "wrap handlers takes a set of exclusions which are not wrapped"
            (let [wrapped-handlers (m/wrap-handlers handlers wrap-function #{:handler-1 :handler-3})]
              ((:handler-1 wrapped-handlers) "request") => "return value"
              ((:handler-2 wrapped-handlers) "request") => "wrap function return value"
              ((:handler-3 wrapped-handlers) "request") => "return value")))

(fact "renders 403 error page when response status is 403"
      (let [handler-that-always-403s (fn [req] {:status 403})
            stub-error-403-handler (fn [req] ...error-403-response...)
            wrapped-handler (m/wrap-handle-403 handler-that-always-403s stub-error-403-handler)]
        (wrapped-handler ...request...) => ...error-403-response...))
