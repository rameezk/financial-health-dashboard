(ns ^:figwheel-hooks financial-health-dashboard.core
  (:require
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]))

(println "Reloaded...")

(defn multiply [a b] (* a b))


;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "Financial Health Dashboard"}))

(defn get-app-element []
  (gdom/getElement "app"))

(defn hello-world []
  [:div
   [:h1 (:text @app-state)]
   [:h3 "Dashboard coming soon (TM)."]
   [:h4 "... maybe ..."]])

(defn simple-component []
  [:div
   [:p "I am a component!"]])

(defn green-button [txt]
  [:button.green txt])

(defn title [txt]
  [:div.title txt])

(defn subtitle [txt]
  [:div.subtitle txt])

(defn app []
  [:div
   [title "Financial Health Dashboard"]
   [subtitle "...coming soon (tm) ..."]])

(defn mount [el]
  (reagent/render-component [app] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
