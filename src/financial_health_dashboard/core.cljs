(ns ^:figwheel-hooks financial-health-dashboard.core
  (:require
   [financial-health-dashboard.changelog :as changelog]
   [financial-health-dashboard.parse :as parse]
   [financial-health-dashboard.domain :as domain]
   [cljs-time.core :as time]
   [cljs-time.format :as tf]
   [goog.dom :as gdom]
   [goog.dom.classlist :as gc]
   [reagent.core :as reagent :refer [atom]]))

(defn multiply [a b] (* a b))

(def number-formatter (js/Intl.NumberFormat.))

(defn format-number [number] (.format number-formatter number))

;; define your app data so that it doesn't get over-written on reload
(defonce state (reagent/atom {:page      :loading
                              :delimiter parse/pipe
                              :modal     {:key :hidden :data nil}
                              :data      nil}))

(defmulti render-page :page)

(defmulti render-modal (fn [state] (get-in @state [:modal :key])))

(defn show-modal [key data]
  (swap! state #(assoc % :modal {:key key :data data})))

(defn hide-modal []
  (swap! state #(assoc % :modal {:key :hidden})))

(defmethod render-page :main [{:keys [data model]}]
  [:div "main"])

(defn set-file-data [data]
  (swap! state #(assoc-in % [:modal :data] data)))

(defn set-app-data [data]
  (swap! state #(assoc % :data data)))

(defmethod render-modal :upload [{:keys [modal delimiter]}]
  [:div.has-text-dark
   [:h1.heading.has-text-centered "Choose file"]
   [:form
    [:div.file.is-centered
     [:label.file-label
      [:input.file-input {:type      "file" :name "storage"
                          :on-change (fn [e]
                                       (let [file   (aget (.. e -target -files) 0)
                                             reader (js/FileReader.)]
                                         (set! (.-onload reader)
                                               #(set-file-data (.. % -target -result)))
                                         (.readAsText reader file)))}]
      [:span.file-cta
       [:span.file-icon
        [:i.fa.fa-upload]]
       [:span.file-label "Upload"]]]]]
   (when-let [content (get-in @state [:modal :data])]
     (let [result
           (->> content
                (parse/parse parse/pipe))
           errors
           (->> result
                (filter (comp not :valid?))
                (map (juxt :i :error)))]
       (if-not (empty? errors)
         [:div.content
          [:hr]
          [:p.heading.has-text-centered.has-text-danger "Oops. You have some errors"]
          [:ul
           (map-indexed
             (fn [i [row e]]
               [:li {:key i} "row: " row ": " e])
             errors)]]
         [:div.content
          [:hr]
          [:p.heading.has-text-centered.has-text-primary "Awesome! No errors!"]
          [:div.buttons.is-centered
           [:button.button.is-primary
            {:on-click (fn [_] (set-app-data (-> result
                                                parse/as-domain-values
                                                domain/all-your-bucks))
                         (hide-modal)
                         (page :loading)
                         (js/setTimeout #(page :main)))}
            "GO"]]])))])

(defmethod render-modal :help [{:keys [modal delimiter]}]
  (println "help modal"))

(defmethod render-modal :save [{:keys [modal delimiter]}]
  (println "save modal"))

(defmethod render-modal :changelog []
  (changelog/render))

(defn bar-chart
  [id]
  (let [context    (.getContext (.getElementById js/document id) "2d")
        chart-data {:type    "bar"
                    :options {:legend {:labels {:fontColor "white"}}
                              :scales {:xAxes [{:ticks {:fontColor "white"}}]
                                       :yAxes [{:ticks {:fontColor "white"}}]}}
                    :data    {:labels   ["2012" "2013" "2014" "2015" "2016"]
                              :datasets [{:data            [5 10 15 20 25]
                                          :label           "Rev in MM"
                                          :backgroundColor "#90EE90"}
                                         {:data            [3 6 9 12 15]
                                          :label           "Cost in MM"
                                          :backgroundColor "#F08080"}]}}]
    (js/Chart. context (clj->js chart-data))))

(defn line-chart
  [id cdx cdy]
  (let [context    (.getContext (.getElementById js/document id) "2d")
        chart-data {:type    "line"
                    :options {:legend {:labels {:fontColor "white"}}
                              :scales {:xAxes [{:ticks {:fontColor "white" :maxTicksLimit 12}}]
                                       :yAxes [{:ticks {:fontColor "white" :beginAtZero true}}]}}
                    :data    {:labels   cdx
                              :datasets [{:data            cdy
                                          :label           "Salary"
                                          :backgroundColor "#90EE90"}]}}]

    (js/Chart. context (clj->js chart-data))))

(defn draw-chart [id chart x y]
  (reagent/create-class
    {:component-did-mount #(chart id x y)
     :display-name        "chart"
     :reagent-render      (fn [] [:canvas {:id id :height "100vw"}])}))

(defn nav []
  [:div
   [:nav.navbar.is-dark
    [:div.navbar-brand
     [:a.navbar-item {:href "#"} "💰 Dashboard (alpha)"]
     [:a.navbar-burger.burger {:id       "nav-menu-burger"
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

(defn col-sample [size & children]
  [:div.column {:class (str "is-" size)}
   [:div.box.is-shadowless.has-text-grey-lighter
    [:span.tag.is-warning "sample"]
    children]])

(defn info-box [title info & [class]]
  [:div.has-text-centered.info-box
   [:p.heading title]
   [:p.title {:class (or class "has-text-light")} info]])

(defn chart-box [title content & [class]]
  [:div.has-text-centered.info-box
   [:p.heading title]
   [:div.is-centered
    [content]]])

(def custom-month-year
  (tf/formatter "MMM-yy"))

(defn salary-over-time-chart [{:keys [salaries]}]
  (let [x (->> salaries (map :cljs-date) (map #(tf/unparse custom-month-year %)))
        y (->> salaries (map :amount))]
    (chart-box "SALARY OVER TIME" (draw-chart
                                    "salary-over-time"
                                    line-chart x y))))

(defn page [data]
  [:div.columns.is-multiline.is-centered
   [col 12 (salary-over-time-chart data)]])

(defn sample-page []
  [:div.columns.is-multiline.is-centered
   [col-sample 4 (info-box "CURRENT NET WORTH" (format-number 100000))]
   [col-sample 4 (info-box "EMERGENCY FUND MONTHS" 1.23)]
   [col-sample 4 (info-box "MONTHLY PERFORMANCE" "14 %")]
   [col-sample 6 (chart-box "SALARY" (draw-chart "salary" line-chart))]
   [col-sample 6 (chart-box "NET WORTH" (draw-chart "net-worth" line-chart))]])

(defn app [state]
  [:div
   [nav]
   (when-not (= :hidden (get-in @state [:modal :key]))
     [modal state])
   [:div.section.has-background-light
    (let [data (get-in @state [:data])]
      (if (nil? data) [sample-page] [page data]))]])

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
