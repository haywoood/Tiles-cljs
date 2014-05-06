(ns tiels.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:tile-grid []}))

;; Default tile
(def tile {:width 8
           :height 15
           :bgColor "yellow"
           :color "red"})

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

;; Component that initializes the UI
(defn app-view [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
        (dom/div #js {:style #js {:margin "0 15px 0 20px"
                                  :color "blue"
                                  :fontWeight "bold"
                                  :fontSize 13
                                  :fontFamily "arial"
                                  :display "inline-block"}} "U S A")
        (om/build grid-view (:tile-grid app))))))

;; Make the 2D grid of default tiles
(swap! app-state assoc :tile-grid (make-grid 25 50))

;; render app
(om/root app-view
         app-state
         {:target (. js/document (getElementById "app"))})
