(ns daily-log.views
  (:require [reagent.core :as r]
            [re-frame.core :as rf]))

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
       [activity-cell activity-id d])]))

(defn new-activity-row []
  [:div.row
   [:div.col.input-field.s8
    [:i.material-icons.prefix "playlist_add"]
    [:input {:id "new_activity_name" :type "text"}]
    [:label {:for "new_activity_name"} "Activity Name"]]
   [:div.col.s4
    {:div.select-wrapper
     [:input.select-dropdown.dropdown-trigger {:type "text"
                                               :readonly true
                                               :data-target "select-options-99"}]
     [:ul.select-dropdown.dropdown-content {:id "select-options-99"
                                            :tab-index 0}
      [:li.selected {:id "select-options-99-0" :tabindex 0} [:span "Choose a type..."]]
      [:li.selected {:id "select-options-99-1" :tabindex 0} [:span "Yes/No"]]
      [:li.selected {:id "select-options-99-1" :tabindex 0} [:span "Whole number"]]]
     [:svg.caret {:height 24 :view-box "0 0 24 24" :width "24" :xmlns "http://www.w3.org/2000/svg"}
      [:path {:d "M7 10l5 5 5-5z"}]
      [:path {:d "M0 0h24v24H0z" :fill "none"}]]}]])

(def vals->display-vals
  {:bool "Yes/No"
   :int "Whole number"
   :float "Decimal number"
   :percentage "Percentage"})

(defn new-activity-input-row []
  (let [dropdown-clicked? (r/atom false)
        dropdown-value (r/atom nil)
        dropdown-options [:bool :int :float :percentage]
        text-input-entering? (r/atom false)
        text-input-value (r/atom "")]
    (fn []
      [:div.row
       [:div.col.input-field.s8
        [:i.material-icons.prefix "playlist_add"]
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
                                                   :readonly "true"
                                                   :value (and @dropdown-value
                                                               (vals->display-vals @dropdown-value))
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
             ^{:key o} [:li {:tabindex 0 :classname (if (= o @dropdown-value)
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
  (let [visible-dates @(rf/subscribe [:visible-dates])
        activity-ids @(rf/subscribe [:visible-activity-ids])]
    [:div.container.grey.lighten-3.z-depth-1 {:style {:margin-top "10px"}}
     [:div.row {:style {:margin-bottom 0}}
      [:div.row.teal.lighten-2 {:style {:margin-bottom 0}}
       [:div.col.s2 "Activity"]
       (for [d visible-dates]
         [:div.col.s2.center-align d])]]
     [:div.row {:style {:padding-top "10px"}}
      (for [id activity-ids]
        [activity-row id])]
     [new-activity-input-row]]))
