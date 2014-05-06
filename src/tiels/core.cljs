(ns tiels.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:tile-grid []
                      :tile-legend []}))

;; Default tile
(def tile {:width 8
           :height 15
           :bgColor "yellow"
           :color "red"})

(def legend-tiles [{:bgColor "maroon" :color "purple"}
                   {:bgColor "blue" :color "cyan"}
                   {:bgColor "teal" :color "pink"}])

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

(defn tile-component [tile owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:style #js {:width (:width tile)
                                :height (:height tile)
                                :backgroundColor (:bgColor tile)
                                :color (:color tile)
                                :textAlign "center"}}
        (dom/span nil ".")))))

(defn row-component [row owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div #js {:style #js {:display "flex"}}
        (om/build-all tile-component row)))))

(defn grid-view [app owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div #js {:style #js {:margin "0 auto"
                                      :display "inline-block"}}
        (om/build-all row-component app)))))

(defn tile-legend [tiles owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
        (dom/div nil "LEGEND")
        (om/build row-component tiles)))))

;; Component that initializes the UI
(defn app-view [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:style #js {:display "flex"}}
        (om/build tile-legend (:tile-legend app))
        (om/build grid-view (:tile-grid app))))))

;; Make the 2D grid of default tiles
(swap! app-state assoc :tile-grid (make-grid 30 50))

;; Make the various tiles to display in the legend!
(swap! app-state assoc :tile-legend (create-multiple-tiles legend-tiles))

;; render app
(om/root app-view
         app-state
         {:target (. js/document (getElementById "app"))})
