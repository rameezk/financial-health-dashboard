(ns ^:figwheel-hooks financial-health-dashboard.core
  (:require
   [goog.dom :as gdom]
   [goog.dom.classlist :as gc]
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

(defn title [txt]
  [:div.title txt])

(defn subtitle [txt]
  [:div.subtitle txt])

(def click-count (reagent/atom 0))

(defn counting-component []
  [:div
   "Counting component " [:code "click-count"] " has value: "
   @click-count ". "
   [:input {:type "button" :value "Click me!"
            :on-click #(swap! click-count inc)}]])

(defn app2 []
  [:div
   [title "Financial Health Dashboard"]
   [subtitle "...coming soon (tm) ..."]
   [counting-component]])

(defn nav []
  [:div
   [:nav.navbar.is-dark
    [:div.navbar-brand
     [:a.navbar-item {:href "#"} "💰 Dashboard"]
     [:a.navbar-burger.burger {:id "nav-menu-burger"
                               :on-click (fn []
                                           (do (gc/toggle (js/document.getElementById "nav-menu") "is-active")
                                               (gc/toggle (js/document.getElementById "nav-menu-burger") "is-active")))}
      [:span {:aria-hidden "true"}]
      [:span {:aria-hidden "true"}]
      [:span {:aria-hidden "true"}]]]
    [:div.navbar-menu {:id "nav-menu"}
     [:div.navbar-end
      [:a.navbar-item {:on-click #(js/alert "I don't know what this is either.")}
       [:span.icon [:i.fa.fa-question-circle]]]
      [:a.navbar-item {:on-click #(js/alert "I don't know what this is either.")}
       [:span.icon [:i.fa.fa-upload]]]
      [:a.navbar-item {:on-click #(js/alert "I don't know what this is either.")}
       [:span.icon [:i.fa.fa-save]]]
      [:a.navbar-item {:on-click #(js/alert "I don't know what this is either.")}
       [:span.icon [:i.fa.fa-history]]]]]]])

(defn app []
  [:div
   [nav]])

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
