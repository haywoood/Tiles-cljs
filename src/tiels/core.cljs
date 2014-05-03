(ns tiels.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def tile {:width 5 :height 15})

(def app-state (atom {:tiles [{:width 5
                               :height 15
                               :bgColor "red"
                               :color "white"}
                              {:width 5
                               :height 15
                               :bgColor "blue"
                               :color "white"}]}))

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

(defn stage-view [app owner]
  (reify
    om/IRender
    (render [this]
            (dom/div #js {:style #js {:width 600
                                     :height 600
                                     :border "1px solid gray"
                                     :margin "0 auto"}}))))

(defn app-view [app owner]
  (reify
    om/IRender
    (render [this]
            (om/build stage-view app))))

(om/root
  app-view
  app-state
  {:target (. js/document (getElementById "app"))})
