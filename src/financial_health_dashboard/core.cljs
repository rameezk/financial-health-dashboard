(ns ^:figwheel-hooks financial-health-dashboard.core
  (:require
   [financial-health-dashboard.changelog :as changelog]
   [financial-health-dashboard.parse :as parse]
   [financial-health-dashboard.domain :as domain]
   [financial-health-dashboard.localstorage :as localstorage]
   [financial-health-dashboard.example :as example]
   [cljs-time.core :as time]
   [clojure.edn :as edn]
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

(defn page [page] (swap! state #(assoc % :page page)))

(defmulti render-modal (fn [state] (get-in @state [:modal :key])))

(defn show-modal [key data]
  (swap! state #(assoc % :modal {:key key :data data})))

(defn hide-modal []
  (swap! state #(assoc % :modal {:key :hidden})))

(defn set-file-data [data]
  (swap! state #(assoc-in % [:modal :data] data)))

(defn set-app-data [data]
  (swap! state #(assoc % :data data)))

(defn save-data-to-localstorage [data]
  (localstorage/set-item! "data" (prn-str data)))

(defn build-app-data-from-uploaded-data [parsed-data]
  (save-data-to-localstorage parsed-data)
  (set-app-data (-> parsed-data parse/as-domain-values domain/all-your-bucks)))

(defn build-app-data-from-localstorage-data [localstorage-data]
  (when-not (nil? localstorage-data)
    (set-app-data (-> localstorage-data
                      parse/as-domain-values
                      domain/all-your-bucks))))

(defn get-data-from-localstorage []
  (or (->> (localstorage/get-item "data") (edn/read-string)) nil))

(defn get-sample-data []
  (parse/parse parse/pipe example/data-piped))

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
            {:on-click (fn [_] (build-app-data-from-uploaded-data result)
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
                                       :yAxes [{:ticks {:fontColor "white" }}]}}
                    :data    {:labels   cdx
                              :datasets [{:data                   cdy
                                          :label                  "Salary"
                                          :lineTension            0
                                          :fill                   false
                                          :borderColor            "#90EE90"
                                          :cubicInterpolationMode "linear"
                                          :backgroundColor        "#90EE90"}]}}]

    (js/Chart. context (clj->js chart-data))))

(defn net-worth-line-chart
  [id labels net-worth assets liabilities]
  (let [context    (.getContext (.getElementById js/document id) "2d")
        chart-data {:type    "line"
                    :options {:legend {:labels {:fontColor "white"}}
                              :scales {:xAxes [{:ticks {:fontColor "white" :maxTicksLimit 12}}]
                                       :yAxes [{:ticks {:fontColor "white"}}]}}
                    :data    {:labels   labels
                              :datasets [{:data                   net-worth
                                          :label                  "Net Worth"
                                          :lineTension            0
                                          :fill                   false
                                          :borderColor            "#90EE90"
                                          :cubicInterpolationMode "linear"
                                          :backgroundColor        "#90EE90"}
                                         {:data                   assets
                                          :label                  "Assets"
                                          :lineTension            0
                                          :fill                   false
                                          :hidden                 true
                                          :borderColor            "#EA3C53"
                                          :cubicInterpolationMode "linear"
                                          :backgroundColor        "#EA3C53"}
                                         {:data                   liabilities
                                          :label                  "Liabilities"
                                          :lineTension            0
                                          :fill                   false
                                          :hidden                 true
                                          :borderColor            "#e67e22"
                                          :cubicInterpolationMode "linear"
                                          :backgroundColor        "#e67e22"}]}}]

    (js/Chart. context (clj->js chart-data))))

(defn tfsa-chart-1
  [id]
  (let [context    (.getContext (.getElementById js/document id) "2d")
        chart-data {:type    "horizontalBar"
                    :options {:legend {:labels {:fontColor "white"}}
                              :scales {:xAxes [{:ticks {:fontColor "white" :beginAtZero true}}]
                                       :yAxes [{:ticks {:fontColor "white"}}]}}
                    :data    {:labels   ["2015" "2016" "2017" "2018" "2019" "2020"]
                              :datasets [{:data            [30000 30000 33000 33000 33000 36000]
                                          :label           "Yearly Limit"
                                          :backgroundColor "#EA3C53"}
                                         {:data            [0 0 0 0 17000 24000]
                                          :label           "Yearly Contribution"
                                          :backgroundColor "#90EE90"}]}}]
    (js/Chart. context (clj->js chart-data))))

(defn tfsa-chart-2
  [id]
  (let [context    (.getContext (.getElementById js/document id) "2d")
        chart-data {:type    "horizontalBar"
                    :options {:legend {:labels {:fontColor "white"}}
                              :scales {:xAxes [{:ticks {:fontColor "white" :beginAtZero true}}]
                                       :yAxes [{:ticks {:fontColor "white"}}]}}
                    :data    {:labels   ["Lifetime"]
                              :datasets [{:data            [500000]
                                          :label           "Limit"
                                          :backgroundColor "#EA3C53"}
                                         {:data            [(+ 17000 24000)]
                                          :label           "Contribution"
                                          :backgroundColor "#90EE90"}]}}]
    (js/Chart. context (clj->js chart-data))))

(defn pie-chart-1
  [id]
  (let [context    (.getContext (.getElementById js/document id) "2d")
        chart-data {:type    "pie"
                    :options {:legend {:labels {:fontColor "white"}}
                              :scales {}}
                    :data    {:labels   ["Retirement Annuity"
                                         "Preservation Fund"
                                         "Emergency Fund"
                                         "TFSA"
                                         "Discretionary Investments"]
                              :datasets [{:data            [20 20 20 20 20]
                                          :backgroundColor ["#2ecc71"
                                                            "#3498db"
                                                            "#e67e22"
                                                            "#9b59b6"
                                                            "#1abc9c"]}]}}]
    (js/Chart. context (clj->js chart-data))))

(defn pie-chart-2
  [id]
  (let [context    (.getContext (.getElementById js/document id) "2d")
        chart-data {:type    "pie"
                    :options {:legend {:labels {:fontColor "white"}}
                              :scales {}}
                    :data    {:labels   ["Local"
                                         "Offshore"]
                              :datasets [{:data            [90 10]
                                          :backgroundColor ["#2ecc71"
                                                            "#3498db"]}]}}]
    (js/Chart. context (clj->js chart-data))))

(defn pie-chart-3
  [id]
  (let [context    (.getContext (.getElementById js/document id) "2d")
        chart-data {:type    "pie"
                    :options {:legend {:labels {:fontColor "white"}}
                              :scales {}}
                    :data    {:labels   ["Property"
                                         "Bonds"
                                         "Equity"
                                         "Cash"
                                         "Vehicle"
                                         "Offshore"]
                              :datasets [{:data            [2 20 29 35 8 6]
                                          :backgroundColor ["#2ecc71"
                                                            "#3498db"
                                                            "#e67e22"
                                                            "#9b59b6"
                                                            "#f1c40f"
                                                            "#c0392b"]}]}}]
    (js/Chart. context (clj->js chart-data))))

(defn draw-chart [id chart x y1 y2 y3]
  (reagent/create-class
    {:component-did-mount #(chart id x y1 y2 y3)
     :display-name        "chart"
     :reagent-render      (fn [] [:canvas {:id id}])}))

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

(defn col-real-data [size-desktop size-mobile & children]
  [:div.column {:class (str "is-" size-desktop "-desktop " "is-" size-mobile "-mobile " "is-" size-mobile "-tablet")}
   [:div.box.is-shadowless.has-text-grey-lighter
    children]])

(defn col-sample-data [size-desktop size-mobile & children]
  [:div.column {:class (str "is-" size-desktop "-desktop " "is-" size-mobile "-mobile " "is-" size-mobile "-tablet")}
   [:div.box.is-shadowless.has-text-grey-lighter
    [:span.tag.is-warning.is-size-7.sample-tag "sample"]
    children]])

(defn info-box [title info change & [class]]
  [:div.has-text-centered.info-box
   [:p.heading title]
   [:p.title {:class (or class "has-text-light")} info]
   (if (> change 0)
     [:p.subtitle.is-size-7.has-text-light.has-text-success
      [:i.fa.fa-arrow-up] (str " " change)]
     [:p.subtitle.is-size-7.has-text-light.has-text-danger
      [:i.fa.fa-arrow-down] (str " " change)])])

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
                                    line-chart x y nil nil))))

(defn net-worth-over-time-chart [{:keys [net-assets net-liabilities net-worths]}]
  (let [labels      (->> net-assets (map :cljs-date) (map #(tf/unparse custom-month-year %)))
        assets      (->> net-assets (map :amount))
        liabilities (->> net-liabilities (map :amount))
        net-worth   (->> net-worths (map :amount))]
    (chart-box "NET WORTH OVER TIME" (draw-chart
                                       "net-worth-over-time"
                                       net-worth-line-chart labels net-worth assets liabilities))))

(defn emergency-fund-months-info-box [{:keys [emergency-fund-months emergency-fund-months-change]}]
  [:div.has-text-centered.info-box
   [:p.heading "EMERGENCY FUND MONTHS"]
   [:p.title {:class "has-text-light"} (format-number emergency-fund-months)]
   (if (= (get emergency-fund-months-change :direction) :up)
     [:p.subtitle.is-size-7.has-text-light.has-text-success
      [:i.fa.fa-arrow-up] (str " " (format-number (get emergency-fund-months-change :delta)) " (" (format-number (get emergency-fund-months-change :percentage)) "%)")]
     (if (= (get emergency-fund-months-change :direction) :down)
       [:p.subtitle.is-size-7.has-text-light.has-text-danger
        [:i.fa.fa-arrow-down] (str " " (format-number (get emergency-fund-months-change :delta)) " (" (format-number (get emergency-fund-months-change :percentage)) "%)")]
       [:p.subtitle.is-size-7.has-text-light.has-text-warning
        [:i.fa.fa-arrow-right] (str " " (format-number (get emergency-fund-months-change :delta)) " (" (format-number (get emergency-fund-months-change :percentage)) "%)")]))])

(defn net-worth-info-box [{:keys [net-worth net-worth-change]}]
  [:div.has-text-centered.info-box
   [:p.heading "NET WORTH"]
   [:p.title {:class "has-text-light"} (format-number net-worth)]
   (if (= (get net-worth-change :direction) :up)
     [:p.subtitle.is-size-7.has-text-light.has-text-success
      [:i.fa.fa-arrow-up] (str " " (format-number (get net-worth-change :delta)) " (" (format-number (get net-worth-change :percentage)) "%)")]
     (if (= (get net-worth-change :direction) :down)
       [:p.subtitle.is-size-7.has-text-light.has-text-danger
        [:i.fa.fa-arrow-down] (str " " (format-number (get net-worth-change :delta)) " (" (format-number (get net-worth-change :percentage)) "%)")]
       [:p.subtitle.is-size-7.has-text-light.has-text-warning
        [:i.fa.fa-arrow-right] (str " " (format-number (get net-worth-change :delta)) " (" (format-number (get net-worth-change :percentage)) "%)")]))])

(defn tfsa-yearly-contributions-chart []
  (chart-box "TFSA YEARLY CONTRIBUTIONS"
             (draw-chart
               "tfsa-yearly-contributions"
               tfsa-chart-1 nil nil nil nil)))

(defn tfsa-lifetime-contribution-chart []
  (chart-box "TFSA LIFETIME CONTRIBUTION"
             (draw-chart
               "tfsa-lifetime-contribution"
               tfsa-chart-2 nil nil nil nil)))

(defn asset-distribution-chart []
  (chart-box "ASSET TYPE DISTRIBUTION"
             (draw-chart "asset-distribution" pie-chart-1 nil nil nil nil)))

(defn asset-geographic-distribution-chart []
  (chart-box "ASSET GEOGRAPHIC DISTRIBUTION"
             (draw-chart "asset-geographic-distribution" pie-chart-2 nil nil nil nil)))

(defn asset-allocation-chart []
  (chart-box "ASSET ALLOCATION"
             (draw-chart "asset-allocation" pie-chart-3 nil nil nil nil)))

(defmethod render-page :main [{:keys [data]}]
  (let [col
        (if (= (->
                 data
                 (get :sample)
                 (first)
                 (get :is-sample))
               "yes")
          col-sample-data
          col-real-data)]
    [:div.columns.is-multiline.is-centered
     [col 6 12 (net-worth-info-box data)]
     [col 6 12 (emergency-fund-months-info-box data)]
     [col 12 12 (net-worth-over-time-chart data)]
     [col 12 12 (salary-over-time-chart data)]
     [col 6 12 (tfsa-yearly-contributions-chart)]
     [col 6 12 (tfsa-lifetime-contribution-chart)]
     [col 4 12 (asset-distribution-chart)]
     [col 4 12 (asset-geographic-distribution-chart)]
     [col 4 12 (asset-allocation-chart)]]))

(defn app []
  [:div
   [nav]
   (when-not (= :hidden (get-in @state [:modal :key]))
     [modal state])
   [:div.section.has-background-light
    (render-page @state)]])

(defn sleep [f ms]
  (js/setTimeout f ms))

(defmethod render-page :loading [state]
  [:div "loading"])

(defn get-app-element []
  (gdom/getElement "app"))

(defn mount [el]
  (reagent/render-component [app] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

(when (= :loading (:page @state))
  (build-app-data-from-localstorage-data (or
                                           (get-data-from-localstorage)
                                           (get-sample-data)))
  (js/setTimeout #(page :main)))

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
