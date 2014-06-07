(ns tiels.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [cljs.core.async :refer [put! chan <!]]
            [tiels.components.grid :refer [grid-component]]))

(enable-console-print!)


(defn make-tile-selected [tile selected-tile]
  (om/update! selected-tile @tile))

(defn undo []
  (when (> (count @history) 1)
    (reset! app-state (last @history))
    (swap! history pop)))

(defn tile [tile owner {:keys [mouse-chan]}]
  (reify
    om/IRender
    (render [_]
            (dom/div #js {:onMouseEnter #(put! mouse-chan {:tile tile :etype "mouseEnter"})
                          :onMouseDown #(put! mouse-chan {:tile tile :etype "mouseDown"})
                          :onMouseUp #(put! mouse-chan {:tile tile :etype "mouseUp"})
                          :style #js {:width 10
                                      :height 17
                                      :backgroundColor (:bgcolor tile)}
                          :className (str "tile" (if (:is-selected-tile tile)
                                                   " is-selected-tile"
                                                   ""))}
                     (dom/span #js {:className "circle"
                                    :style #js {:backgroundColor (:color tile)}}
                               "")))))

(defn undo-view [_ _]
  (reify
    om/IRender
    (render [_]
            (dom/div #js {:className "undo"
                          :onMouseDown #(undo)}
                     "⌫"))))

(defn new-tile [_ owner {:keys [create-fn] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
                {:value "#"})

    om/IRenderState
    (render-state [_ state]
                  (let [value (:value state)]
                    (dom/input #js {:className "new-tile"
                                    :value value
                                    :onChange (fn [e]
                                                (let [new-value (aget e "target" "value")]
                                                  (om/set-state! owner :value new-value)))
                                    :onKeyUp (fn [e]
                                               (let [return-key (= 13 (aget e "which"))]
                                                 (when return-key
                                                   (do
                                                     (om/set-state! owner :value "")
                                                     (create-fn value)))))})))))


(defn legend [app owner]
  (reify
    om/IInitState
    (init-state [_]
                {:mouse-chan (chan)})
    om/IWillMount
    (will-mount [_]
                (let [mouse-chan (om/get-state owner :mouse-chan)]
                  (go
                    (loop []
                      (let [{:keys [tile etype]} (<! mouse-chan)
                            selected (:selected-tile app)]
                        (when (= etype "mouseDown")
                          (make-tile-selected tile selected))
                        (recur))))))
    om/IRenderState
    (render-state [_ {:keys [mouse-chan]}]
            (apply dom/div #js {:className "legend"}
                   (om/build-all tile (:tiles app)
                                 {:fn #(assoc % :is-selected-tile (= % (:selected-tile app)))
                                  :opts {:mouse-chan mouse-chan}})))))

(defn toolbar [app owner]
  (reify
    om/IInitState
    (init-state [_]
                {:creating-new-tile false})

    om/IRenderState
    (render-state [_ state]
                  (dom/div #js {:className "toolbar"}
                           (om/build legend {:tiles (:legend-tiles app)
                                             :selected-tile (:selected-tile app)})

                           (dom/div #js {:className "actions"}
                                    (om/build undo-view {})
                                    (dom/div #js {:className (str "new-tile-icon"
                                                                  (when (om/get-state owner :creating-new-tile)
                                                                    " is-active"))
                                                  :onMouseDown
                                                  #(om/set-state! owner :creating-new-tile
                                                                  (not (om/get-state owner :creating-new-tile)))}
                                             "░"))

                           (when (:creating-new-tile state)
                             (om/build new-tile {}
                                       {:opts {:create-fn
                                               (fn [value]
                                                 (let [legend-tiles (:legend-tiles @app)
                                                       new-legend-tile {:bgcolor value :color "white"}
                                                       new-legend-tiles (conj (:legend-tiles @app)
                                                                              {:bgcolor value :color "white"})]
                                                   (when (nil? (some #(= % new-legend-tile) legend-tiles))
                                                     (om/update! app :legend-tiles new-legend-tiles))))}}))))))





;; Component that initializes the UI
(defn app-view [app owner]
  (reify
    om/IRender
    (render [_]
            (dom/div #js {:style #js {:display "flex"}}
                     (om/build toolbar app)
                     (om/build grid-component {:tiles (:tiles app)
                                               :selected-tile (:selected-tile app)}
                               {:opts {:width (* 10
                                                 (get-in app [:grid :columns]))}})))))

(defn create-rand-tiles [n]
  (mapv #(rand-nth colors) (range n)))

(defn create-tiles [n]
  (mapv (fn [_] {:bgcolor "white" :color "red"}) (range n)))

(def colors [{:bgcolor "#444"   :color "white"}
             {:bgcolor "blue"   :color "white"}
             {:bgcolor "cyan"   :color "blue"}
             {:bgcolor "red"    :color "white"}
             {:bgcolor "pink"   :color "white"}
             {:bgcolor "yellow" :color "red"}
             {:bgcolor "#64c7cc" :color "cyan"}
             {:bgcolor "#00a64d" :color "#75f0c3"}
             {:bgcolor "#f5008b" :color "#ffdbbf"}
             {:bgcolor "#0469bd" :color "#75d2fa"}
             {:bgcolor "#fcf000" :color "#d60000"}
             {:bgcolor "#010103" :color "#fa8e66"}
             {:bgcolor "#7a2c02" :color "#fff3e6"}
             {:bgcolor "#07c3f7" :color "#0d080c"}
             {:bgcolor "#f5989c" :color "#963e03"}
             {:bgcolor "#ed1c23" :color "#fff780"}
             {:bgcolor "#f7f7f7" :color "#009e4c"}
             {:bgcolor "#e04696" :color "#9c2c4b"}])

(def app-state (atom {:tiles (create-tiles 1500)
                      :grid {:rows 30 :columns 50}
                      :selected-tile {:bgcolor "cyan"  :color "blue"}
                      :legend-tiles colors}))

(def history (atom [@app-state]))

;; render app
(om/root app-view
         app-state
         {:tx-listen
          (fn [data state]
            (when-not (= (last @history) @state)
              (swap! history conj (:old-state data))))
          :target (. js/document (getElementById "app"))})
