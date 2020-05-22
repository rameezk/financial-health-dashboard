(ns ^:figwheel-hooks financial-health-dashboard.core
  (:require
   [goog.dom :as gdom]
   [goog.dom.classlist :as gc]
   [reagent.core :as reagent :refer [atom]]))

(defn multiply [a b] (* a b))


;; define your app data so that it doesn't get over-written on reload


(defonce state (reagent/atom {:modal {:key :hidden :data nil}}))

(defn show-modal [key data]
  (swap! state #(assoc % :modal {:key key :data data})))

(defn hide-modal []
  (swap! state #(assoc % :modal {:key :hidden})))

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
      [:a.navbar-item {:on-click #(show-modal :help nil)}
       [:span.icon [:i.fa.fa-question-circle]]]
      [:a.navbar-item {:on-click #(show-modal :upload nil)}
       [:span.icon [:i.fa.fa-upload]]]
      [:a.navbar-item {:on-click #(show-modal :save nil)}
       [:span.icon [:i.fa.fa-save]]]
      [:a.navbar-item {:on-click #(show-modal :changelog nil)}
       [:span.icon [:i.fa.fa-history]]]]]]])

(defn modal [model-state]
  [:div.modal.is-active
   [:div.modal-background {:on-click hide-modal}]
   [:div.modal-content
    [:div.box
     [:div "I'm a modal!"]]
    [:button.modal-close.is-large {:on-click hide-modal}]]])

(defn app [state]
  [:div
   [nav]
   (when-not (= :hidden (get-in @state [:modal :key]))
     [modal])
   [:div.section
    [:div "section"]]])

(defn get-app-element []
  (gdom/getElement "app"))

(defn mount [el]
  (reagent/render-component [app state] el))

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
