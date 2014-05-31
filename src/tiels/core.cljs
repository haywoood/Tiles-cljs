(ns tiels.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(defn make-tile-current [tile _ {:keys [selected-tile] :as opts}]
  (om/update! tile @selected-tile))

(defn make-tile-selected [tile _ {:keys [selected-tile] :as opts}]
  (om/update! selected-tile @tile))

(defn undo []
    (when (> (count @history) 1)
      (reset! app-state (last @history))
      (swap! history pop)))

(defn tile [tile owner {:keys [selected-fn selected-tile] :as opts}]
  (reify
    om/IRender
    (render [_]
            (dom/div #js {:onMouseDown #(selected-fn tile owner opts)
                          :style #js {:width 10
                                      :height 17
                                      :backgroundColor (:bgcolor tile)}
                          :className (str "tile" (if (:is-selected-tile tile)
                                                   " is-selected-tile"
                                                   ""))}
                     (dom/span #js {:className "circle"
                                    :style #js {:backgroundColor (:color tile)}}
                               "")))))

(defn grid [app owner {:keys [width] :as opts}]
  (reify
    om/IRender
    (render [_]
            (apply dom/div #js {:className "grid"
                                :style #js {:width width}}
                   (om/build-all tile (:tiles app)
                      {:opts {:selected-tile (:selected-tile app)
                              :selected-fn make-tile-current}})))))

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
      {:value ""})

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
    om/IRender
    (render [_]
            (apply dom/div #js {:className "legend"}
                     (om/build-all tile (:tiles app)
                                   {:fn #(assoc % :is-selected-tile (= % (:selected-tile app)))
                                    :opts {:selected-tile (:selected-tile app)
                                           :selected-fn make-tile-selected}})))))

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
                     (om/build grid {:tiles (:tiles app)
                                     :selected-tile (:selected-tile app)}
                               {:opts {:width (* 10
                                                 (get-in app [:grid :columns]))}})))))

(defn create-tiles [n]
  (into [] (take n (repeat {:bgcolor "white" :color "red"}))))

(def app-state (atom {:tiles []
                      :grid {:rows 30 :columns 50}
                      :selected-tile {:bgcolor "cyan"  :color "blue"}
                      :legend-tiles [{:bgcolor "#444"   :color "white"}
                                     {:bgcolor "blue"   :color "white"}
                                     {:bgcolor "cyan"   :color "blue"}
                                     {:bgcolor "red"    :color "white"}
                                     {:bgcolor "pink"   :color "white"}
                                     {:bgcolor "yellow" :color "red"}
                                     {:bgcolor "#64c7cc" :color "cyan"}]}))

(swap! app-state assoc :tiles (create-tiles (* (get-in @app-state [:grid :rows])
                                               (get-in @app-state [:grid :columns]))))

(def history (atom [@app-state]))

;; render app
(om/root app-view
         app-state
         {:tx-listen
          (fn [data state]
            (when-not (= (last @history) @state)
              (swap! history conj (:old-state data))))
          :target (. js/document (getElementById "app"))})
