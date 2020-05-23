(ns ^:figwheel-hooks financial-health-dashboard.core
  (:require
   [financial-health-dashboard.changelog :as changelog]
   [goog.dom :as gdom]
   [goog.dom.classlist :as gc]
   [reagent.core :as reagent :refer [atom]]))

(defn multiply [a b] (* a b))

;; define your app data so that it doesn't get over-written on reload
(defonce state (reagent/atom {:page :loading
                              :modal {:key :hidden :data nil}}))

(defmulti render-page :page)

(defmulti render-modal (fn [state] (get-in @state [:modal :key])))

(defn show-modal [key data]
  (swap! state #(assoc % :modal {:key key :data data})))

(defn hide-modal []
  (swap! state #(assoc % :modal {:key :hidden})))

(defmethod render-page :main [{:keys [data model]}]
  [:div "main"])

(defmethod render-modal :upload [{:keys [modal delimiter]}]
  [:div.has-text-dark
   [:h1.heading.has-text-centered "Choose file"]
   [:form
    [:div.file.is-centered
     [:label.file-label
      [:input.file-input {:type "file" :name "storage"}]
      [:span.file-cta
       [:span.file-icon
        [:i.fa.fa-upload]]
       [:span.file-label "Upload"]]]]]])

(defmethod render-modal :help [{:keys [modal delimiter]}]
  (println "help modal"))

(defmethod render-modal :save [{:keys [modal delimiter]}]
  (println "save modal"))

(defmethod render-modal :changelog []
  (changelog/render))

(defn chart
  []
  (let [context (.getContext (.getElementById js/document "my-chart") "2d")
        chart-data {:type "bar"
                    :options {:legend {:labels {:fontColor "white"}}
                              :scales {:xAxes [{:ticks {:fontColor "white"}}]}}
                    :data {:labels ["2012" "2013" "2014" "2015" "2016"]
                           :datasets [{:data [5 10 15 20 25]
                                       :label "Rev in MM"
                                       :backgroundColor "#90EE90"}
                                      {:data [3 6 9 12 15]
                                       :label "Cost in MM"
                                       :backgroundColor "#F08080"}]}}]
    (js/Chart. context (clj->js chart-data))))

(defn chart-component []
  (reagent/create-class
   {:component-did-mount #(chart)
    :display-name "chart"
    :reagent-render (fn [] [:canvas {:id "my-chart"}])}))

(defn nav []
  [:div
   [:nav.navbar.is-dark
    [:div.navbar-brand
     [:a.navbar-item {:href "#"} "💰 Dashboard (alpha)"]
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
    [:div.box.has-background-light
     (render-modal model-state)]
    [:button.modal-close.is-large {:on-click hide-modal}]]])

(defn col [size & children]
  [:div.column {:class (str "is-" size)}
   [:div.box.is-shadowless.has-text-grey-lighter
    children]])

(defn info-box [title info & [class]]
  [:div.has-text-centered.info-box
   [:p.heading title]])

(defn chart-box [title content & [class]]
  [:div.has-text-centered.info-box
   [:p.heading title]
   [:div.is-centered
    [:canvas {:id "my-chart" :height "100vm"} content]]])

(defn page []
  [:div.columns.is-multiline.is-centered
   [col 12 (chart-box "Revenue" [chart-component] )]])

(defn app [state]
  [:div
   [nav]
   (when-not (= :hidden (get-in @state [:modal :key]))
     [modal state])
   [:div.section.has-background-light
    [page]]])

(defmethod render-page :loading [state]
  [:div "loading"])

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
