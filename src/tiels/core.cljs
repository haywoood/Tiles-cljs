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
                                :style #js {:margin "0 auto"
                                            :width width}}
                   (om/build-all tile (:tiles app)
                      {:opts {:selected-tile (:selected-tile app)
                              :selected-fn make-tile-current}})))))

(defn history-view [app _]
  (reify
    om/IRender
    (render [_]
            (dom/div #js {:className "undo"
                          :onMouseDown #(undo)}
                     "↩"))))

(defn legend [app owner]
  (reify
    om/IRender
    (render [_]
            (apply dom/div #js {:style #js {:display "flex"}}
                     (om/build-all tile (:tiles app)
                                   {:fn #(assoc % :is-selected-tile (= % (:selected-tile app)))
                                    :opts {:selected-tile (:selected-tile app)
                                           :selected-fn make-tile-selected}})))))

;; Component that initializes the UI
(defn app-view [app owner]
  (reify
    om/IRender
    (render [_]
            (dom/div #js {:style #js {:display "flex"}}
                     (dom/div #js {:style #js {:display "flex"
                                               :flexDirection "column"}}
                              (om/build legend {:tiles (:legend-tiles app)
                                                :selected-tile (:selected-tile app)})
                              (om/build history-view app))
                     (om/build grid {:tiles (:tiles app)
                                     :selected-tile (:selected-tile app)}
                               {:opts {:width (* 10
                                                 (get-in app [:grid :columns]))}})))))

(defn create-tiles [n]
  (into [] (take n (repeat {:bgcolor "white" :color "red"}))))

(def app-state (atom {:tiles []
                      :grid {:rows 28 :columns 45}
                      :selected-tile {:bgcolor "cyan"  :color "blue"}
                      :legend-tiles [{:bgcolor "#444"   :color "white"}
                                     {:bgcolor "blue"   :color "white"}
                                     {:bgcolor "cyan"   :color "blue"}
                                     {:bgcolor "red"    :color "white"}
                                     {:bgcolor "pink"   :color "white"}
                                     {:bgcolor "yellow" :color "red"}]}))

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
