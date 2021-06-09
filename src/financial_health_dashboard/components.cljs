(ns financial-health-dashboard.components)

(defn nav []
  [:nav.bg-purple-700.text-white.flex.items-center.justify-between.flex-wrap.p-6
   [:div.flex.items-center.flex-shrink-0.mr-6
    [:span "💰 Financial Health Dashboard"]]
   [:div
    [:a.inline-block.text-sm.px-4.py-2.leading-none.border.rounded.border-white.hover:border-transparent.hover:text-teal-500.hover:bg-purple-300.mt-4.lg:mt-0 {:href "https://www.google.com"} "Upload Data File"]]])
