(ns tiels.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:tiles []}))

;; Default tile
(def tile {:width 5
           :height 15
           :bgColor "red"
           :color "white"})

(defn tile-row
  "Creates a vector of n tiles"
  [n]
  (into [] (take n (repeat tile))))

(defn make-grid
  "Creates a 2D vector of n rows and n col in each row"
  [row col]
  (into [] (take row (repeat (tile-row col)))))

;; This has to change
(defn tile-component [tile owner]
  (reify
    om/IRender
    (render [this]
            (dom/div
              #js {:style #js {:width (:width tile)
                               :height (:height tile)
                               :backgroundColor (:bgColor tile)
                               :color (:color tile)}
                   :className "tile"}
              (dom/span #js {:className "dot"} ".")))))

;; This has to change
(defn stage-view [app owner]
  (reify
    om/IRender
    (render [this]
            (dom/div #js {:style #js {:width 600
                                      :height 600
                                      :border "1px solid gray"
                                      :margin "0 auto"}}))))

;; Component that initializes the UI
(defn app-view [app owner]
  (reify
    om/IRender
    (render [this]
            (om/build stage-view app))))

;; Make the 2D grid of default tiles
(swap! app-state assoc :tiles (make-grid 25 50))

;; render app
(om/root app-view
         app-state
         {:target (. js/document (getElementById "app"))})
