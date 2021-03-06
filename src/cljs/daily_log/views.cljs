(ns daily-log.views
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [daily-log.dates :as d]))

(defn toasts []
  (let [toasts (rf/subscribe [:toasts])]
    [:div {:id "toast-container"}
     (for [t @toasts]
       ^{:key (:id t)}
       [:div.toast.clickable
        {:on-click #(rf/dispatch [:remove-toast (:id t)])
         :class (if (= :error
                       (:type t))
                  "red lighten-2"
                  "green lighten-2")}
        [:span (:content t)]])]))

(defn table-header []
  (let [visible-dates (rf/subscribe [:visible-dates])
        date-being-edited (rf/subscribe [:date-being-edited])]
    [:div.row.mb0
     [:div.row.valign-wrapper.mb0.teal.lighten-3
      [:div.col.s2 "Activity"]
      (doall
       (for [d @visible-dates]
         ^{:key d}
         [:div.col.s2.center-align
          [:div.row.mb1.mt1 (d/->MMM-str d)]
          [:div.row.mb2
           [:div.mb0.mt0.circle-h5 {:class (if (= d @date-being-edited)
                                             "white lighten-1"
                                             "teal lighten-3")}
            (d/->DD-str d)]]]))]]))

(defn log-val->display-str [type val]
  (if (= :bool type)
    (if (zero? val) "No" "Yes")
    (str val)))

(defn new-value-modal [toggle update-log show-modal?]
  (let [interacting? (r/atom false)
        value (r/atom "")]
    (fn [_ _ _]
      [:div.modal {:style {:display (if @show-modal? "block" "none")}}
       [:div.modal-content
        [:div.input-field
         [:input {:id "new_activity_value"
                  :type "text"
                  :value @value
                  :on-click #(reset! interacting? true)
                  :on-blur #(reset! interacting? false)
                  :on-change #(reset! value (-> % .-target .-value))}]

         [:label {:class (str (if (not= "" @value) "selected ")
                              (if @interacting? "interacting"))}
          "New Value"]]]
       [:div.modal-footer
        [:a.modal-close.btn-flat
         {:on-click #(do (toggle)
                         (reset! value ""))}
         "Cancel"]
        [:a.modal-close.btn-flat
         {:on-click #(do (toggle)
                         (update-log (js/parseInt @value))
                         (reset! value ""))}
         "Save"]]])))

(defn activity-cell [activity-id date]
  (let [t (rf/subscribe [:activity-type activity-id])
        val (rf/subscribe [:log activity-id date])
        timeout (r/atom nil)
        show-modal? (r/atom false)
        toggle-modal #(swap! show-modal? not)
        update-log #(rf/dispatch [:update-log activity-id date %])]
    (fn [_ _]
      [:div.col.s2.center-align
       [new-value-modal toggle-modal update-log show-modal?]
       [:span.cell-val.unselectable.clickable
        {:on-mouse-down
         #(reset! timeout (js/setTimeout toggle-modal 1000))
         :on-mouse-up
         #(let [new-val (if (= :bool @t) 1 (inc @val))]
            (js/clearTimeout @timeout)
            (update-log new-val))}
        (log-val->display-str @t @val)]])))

(defn activity-row [activity-id]
  (let [activity-name @(rf/subscribe [:activity-name activity-id])
        visible-dates @(rf/subscribe [:visible-dates])]
    [:div.row
     [:div.col.s2 activity-name]
     (for [d visible-dates]
       ^{:key (str activity-id d)}
       [activity-cell activity-id d])]))

(defn table-body []
  [:div.row.pt2
   (let [activity-ids @(rf/subscribe [:visible-activity-ids])]
     (for [id activity-ids]
       ^{:key id}
       [activity-row id]))])

(defn autocomplete-dropdown [value]
  (let [activities (rf/subscribe [:search-activities @value])]
    [:ul.autocomplete-content.dropdown-content
     {:style {:display "block" :top "40px"}}
     (for [{:keys [id search-result]} @activities]
       ^{:key id}
       [:li
        {:on-click #(do (rf/dispatch [:display-activity id])
                        (reset! value ""))}
        [:span
         (for [{:keys [type val]} search-result]
           (if (= :non-match type)
             ^{:key (random-uuid)} val
             ^{:key (random-uuid)} [:span.highlight val]))]])]))

(defn text-input [value interacting? on-submit]
  (let [interacting-internal? (r/atom false)]
    (reset! value "")
    (fn [_ _]
      [:div.input-field
       [:i.material-icons.prefix.unselectable.clickable
        {:class (if @interacting? "active")
         :on-click on-submit}
        "playlist_add"]
       [:input {:id "new_activity_name"
                :type "text"
                :value @value
                :on-click #(do (reset! interacting? true)
                               (reset! interacting-internal? true))
                :on-blur #(do (reset! interacting? false)
                              (reset! interacting-internal? false))
                :on-change #(reset! value (-> % .-target .-value))}]
       [autocomplete-dropdown value]
       [:label {:class (str (if (not= "" @value)
                              "selected ")
                            (if @interacting-internal?
                              "interacting"))}
        "Activity Name"]])))

(defn dropdown [option->display-str selected-option interacting? label-text]
  (let [options (keys option->display-str)
        interacting-internal? (r/atom false)]
    (reset! selected-option nil)
    (fn [_ _ _]
    [:div.input-field
     [:div
      [:input.select-dropdown.dropdown-trigger
       {:type "text"
        :on-click #(do (reset! interacting-internal? true)
                       (reset! interacting? true))
        :read-only true
        :data-target "select-options"
        :value (or (some-> @selected-option option->display-str)
                   "")}]
      [:ul.select-dropdown.dropdown-content
       {:id "select-options"
        :tab-index 0
        :class (if @interacting-internal? "interacting")
        :style {:height (str (* 50 (count options))
                             "px")}}
       (doall
        (for [o options]
          ^{:key o}
          [:li {:tab-index 0
                :class (if (= o @selected-option) "selected")
                :on-click #(do (reset! selected-option o)
                               (reset! interacting-internal? false)
                               (reset! interacting? false))}
           [:span (option->display-str o)]]))]
      [:svg.caret.clickable
       {:on-click #(do (reset! interacting-internal? true)
                       (reset! interacting? true))
        :height 24
        :view-box "0 0 24 24"
        :width 24
        :xmlns "http://www.w3.org/2000/svg"}
       [:path {:d "M7 10l5 5 5-5z"}]
       [:path {:d "M0 0h24v24H0z" :fill "none"}]]]
     [:label {:class (str (if (some? @selected-option)
                            "selected ")
                          (if @interacting-internal?
                            "interacting"))}
      label-text]])))

(def activity-type-option->display-str
  {:bool "Yes/No"
   :int "Whole number"})

(defn new-activity-form []
  (let [interacting? (r/atom false)
        new-activity-type (r/atom nil)
        new-activity-name (r/atom "")
        on-submit #(do (rf/dispatch [:post-activity
                                     @new-activity-type
                                     @new-activity-name])
                       (reset! new-activity-type nil)
                       (reset! new-activity-name ""))]
    (fn []
      [:div.row
       [:div.col.s8
        [text-input new-activity-name
                    interacting?
                    on-submit]]
       [:div.col.s4
        [dropdown activity-type-option->display-str
                  new-activity-type
                  interacting?
                  "Activity Type"]]])))

(defn app []
  (let []
    [:div.container.grey.lighten-3.z-depth-1.mt2
     [toasts]
     [table-header]
     [table-body]
     [new-activity-form]]))
