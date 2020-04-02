(ns daily-log.views
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [daily-log.dates :as d]))

(defn header []
  (let [visible-dates (rf/subscribe [:visible-dates])
        date-being-edited (rf/subscribe [:date-being-edited])]
    [:div.row.mb0
     [:div.row.valign-wrapper.mb0.teal.lighten-3
      [:div.col.s2 "Activity"]
      (for [d @visible-dates]
        ^{:key d} [:div.col.s2.center-align
                   [:div.row.mb1.mt1 (d/->MMM-str d)]
                   (if (= d @date-being-edited)
                     [:div.row.mb2 [:div.mb0.mt0.circle-h5.white.lighten-1 (d/->DD-str d)]]
                     [:div.row.mb2 [:div.mb0.mt0.circle-h5.teal.lighten-3 (d/->DD-str d)]])])]]))

(defn log-val->display-str [type val]
  (cond
    (= :bool type) (if (= 1 val) "Yes" "No")
    (= :percentage type) (str val "%")
    (= :float type) (str (/ val 1000))
    :else (str val)))

(defn activity-cell [activity-id date]
  (let [log @(rf/subscribe [:log activity-id date])
        t @(rf/subscribe [:activity-type activity-id])]
    [:div.col.s2.center-align (log-val->display-str t log)]))

(defn activity-row [activity-id]
  (let [activity-name @(rf/subscribe [:activity-name activity-id])
        visible-dates @(rf/subscribe [:visible-dates])]
    [:div.row
     [:div.col.s2 activity-name]
     (for [d visible-dates]
       ^{:key (str activity-id d)}
       [activity-cell activity-id d])]))

(defn body []
  [:div.row {:style {:padding-top "10px"}}
   (let [activity-ids @(rf/subscribe [:visible-activity-ids])]
     (for [id activity-ids]
       ^{:key id}
       [activity-row id]))])

(def vals->display-vals
  {:bool "Yes/No"
   :int "Whole number"
   :float "Decimal number"
   :percentage "Percentage"})

(defn new-activity-input []
  (let [dropdown-clicked? (r/atom false)
        dropdown-value (r/atom nil)
        dropdown-options [:bool :int :float :percentage]
        text-input-entering? (r/atom false)
        text-input-value (r/atom "")]
    (fn []
      [:div.row
       [:div.col.input-field.s8
        [:i.material-icons.prefix {:class (if (or @dropdown-clicked?
                                                  @text-input-entering?)
                                            "active")}
         "playlist_add"]
        [:input {:id "new_activity_name"
                 :type "text"
                 :value @text-input-value
                 :on-click #(reset! text-input-entering? true)
                 :on-blur #(reset! text-input-entering? false)
                 :on-change #(reset! text-input-value
                                     (-> % .-target .-value))}]
        [:label (merge {:for "new_activity_name"}
                       (if (or @text-input-entering?
                               (not= "" @text-input-value))
                         {:style {:transform "translateY(-14px) scale(0.8)"}}))
         "Activity Name"]]
       [:div.col.input-field.s4 {:on-click #(swap! dropdown-clicked? not)}
        [:div
         [:input.select-dropdown.dropdown-trigger {:type "text"
                                                   :read-only true
                                                   :value (or (and @dropdown-value
                                                                   (vals->display-vals @dropdown-value))
                                                              "")
                                                   :data-target "select-options"}]
         [:ul.select-dropdown.dropdown-content {:id "select-options"
                                                :tab-index 0
                                                :style {:display (if @dropdown-clicked?
                                                                   "block"
                                                                   "none")
                                                        :width "213.031px"
                                                        :left "10px"
                                                        :top "10px"
                                                        :height "200px"
                                                        :transform-origin "0px 0px"
                                                        :opacity "1"
                                                        :transform "scaleX(1) scaleY(1)"}}
          (doall
           (for [o dropdown-options]
             ^{:key o} [:li {:tab-index 0 :class (if (= o @dropdown-value)
                                                        "selected")}
                        [:span {:on-click #(reset! dropdown-value o)} (vals->display-vals o)]]))]
         [:svg.caret {:style {:position "absolute"
                              :right "10px"
                              :top "0"
                              :bottom "0"
                              :margin "auto 0"
                              :z-index "0"
                              :fill "rgba(0,0,0,0.87)"}
                      :height 24 :view-box "0 0 24 24" :width "24" :xmlns "http://www.w3.org/2000/svg"}
          [:path {:d "M7 10l5 5 5-5z"}]
          [:path {:d "M0 0h24v24H0z" :fill "none"}]]]
        [:label (merge {}
                       (if (some? @dropdown-value)
                         {:style {:transform "translateY(-14px) scale(0.8)"}})
                       (if @dropdown-clicked?
                         {:style {:color "#26a69a" :transform "translateY(-14px) scale(0.8)"}}))
         "Activity type"]]])))

(defn app []
  (let []
    [:div.container.grey.lighten-3.z-depth-1 {:style {:margin-top "10px"}}
     [header]
     [body]
     [new-activity-input]]))
