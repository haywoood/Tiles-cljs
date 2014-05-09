(ns tiels.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

;; Default tile
(def tile {:type :grid
           :width 8
           :height 15
           :bgColor "yellow"
           :color "red"})

(def legend-tiles [{:type :legend :bgColor "red" :color "white"}
                   {:type :legend :bgColor "white" :color "red"}
                   {:type :legend :bgColor "blue" :color "white"}])

(defn create-tile [attrs]
  (merge tile attrs))

(defn create-multiple-tiles [props]
  (mapv create-tile props))

(defn tile-row
  "Creates a vector of n tiles"
  [n]
  (into [] (take n (repeat tile))))

(defn make-grid
  "Creates a 2D vector of n rows and n col in each row"
  [row col]
  (into [] (take row (repeat (tile-row col)))))

(defn make-tile-current [current tile]
  (.log js/console (:bgColor tile)))

(defn grid-component [tile owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:onClick (fn [e] (om/update! tile (:current-tile @app-state)))
                    :style #js {:width (:width tile)
                                :height (:height tile)
                                :backgroundColor (:bgColor tile)
                                :color (:color tile)
                                :textAlign "center"}}
        (dom/span nil ".")))))

(defn legend-component [tile owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [chan]}]
      (dom/div #js {:onClick (fn [e] (put! chan @tile))
                    :style #js {:width (:width tile)
                                :height (:height tile)
                                :backgroundColor (:bgColor tile)
                                :color (:color tile)
                                :textAlign "center"}}
        (dom/span nil ".")))))

(defmulti tile-component (fn [tile _] (:type tile)))

(defmethod tile-component :legend
  [tile owner] (legend-component tile owner))

(defmethod tile-component :grid
  [tile owner] (grid-component tile owner))

(defn row-component [row owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [chan]}]
      (apply dom/div #js {:style #js {:display "flex"}}
        (om/build-all tile-component row
                      {:init-state {:chan chan}})))))

(defn tile-legend [tiles owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [chan]}]
      (om/build row-component tiles {:init-state {:chan chan}}))))

(defn grid-view [tile-grid owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div #js {:style #js {:margin "0 auto"
                                      :display "inline-block"}}
        (om/build-all row-component tile-grid)))))

;; Component that initializes the UI
(defn app-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:update-cur-tile (chan)})
    om/IWillMount
    (will-mount [_]
      (let [update-cur-tile (om/get-state owner :update-cur-tile)]
        (go (loop []
          (let [tile (<! update-cur-tile)
                new-tile (assoc tile :type :grid)]
            (om/transact! app :current-tile #(merge % new-tile))
            (recur))))))
    om/IRenderState
    (render-state [this {:keys [update-cur-tile]}]
      (dom/div #js {:style #js {:display "flex"}}
        (om/build tile-legend (:tile-legend app)
                  {:init-state {:chan update-cur-tile}})
        (om/build grid-view (:tile-grid app))))))

(def app-state (atom {:tile-grid []
                      :current-tile (create-tile {:bgColor "cyan"})
                      :tile-legend []}))

;; Make the 2D grid of default tiles
(swap! app-state assoc :tile-grid (make-grid 30 60))

;; Make the various tiles to display in the legend!
(swap! app-state assoc :tile-legend (create-multiple-tiles legend-tiles))

;; render app
(om/root app-view
         app-state
         {:target (. js/document (getElementById "app"))})
