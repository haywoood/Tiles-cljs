(ns tiels.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(defn make-tile-current [tile selected-tile]
  (om/update! tile selected-tile))

(defn tile [tile owner]
  (reify
    om/IRender
    (render [_]
            (let [selected-tile (om/get-shared owner :selected-tile)
                  is-selected (= selected-tile tile)]
              (dom/div #js {:onMouseDown #(make-tile-current tile selected-tile)
                            :style #js {:width 8
                                        :height 15
                                        :backgroundColor (:bgcolor tile)}
                            :className (str "tile" (if is-selected
                                                     " is-selected-tile"
                                                     ""))}
                       (dom/span #js {:className "circle"
                                      :style #js {:backgroundColor (:color tile)}}
                                 ""))))))


(defn grid [tiles owner {:keys [width] :as opts}]
  (reify
    om/IRender
    (render [_]
            (apply dom/div #js {:className "grid"
                                :style #js {:margin "0 auto"
                                            :width width}}
                   (om/build-all tile tiles)))))

;; Component that initializes the UI
(defn app-view [app owner]
  (reify
    om/IRender
    (render [_]
            (dom/div #js {:style #js {:display "flex"}}
                     (om/build grid (:tiles app)
                               {:opts {:width (* 8
                                                 (get-in app [:grid :columns]))}})))))

(defn create-tiles [n]
  (into [] (take n (repeat {:bgcolor "white" :color "red"}))))

(def app-state (atom {:tiles (create-tiles (* 60 30))
                      :grid {:rows 30 :columns 60}
                      :selected-tile {:bgcolor "cyan"  :color "blue"}
                      :legend-tiles [{:bgcolor "#444"   :color "white"}
                                     {:bgcolor "blue"   :color "white"}
                                     {:bgcolor "cyan"   :color "blue"}
                                     {:bgcolor "red"    :color "white"}
                                     {:bgcolor "pink"   :color "white"}
                                     {:bgcolor "yellow" :color "red"}]}))

;; render app
(om/root app-view
         app-state
         {:target (. js/document (getElementById "app"))
          :shared {:selected-tile (:selected-tile @app-state)}})
