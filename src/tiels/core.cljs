(ns tiels.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [datascript :as d]
            [cljs.core.async :refer [put! chan <!]]
            [tiels.state :refer [app-state]]))

(enable-console-print!)

(defn make-tile-current [current tile]
  (.log js/console (:bgColor tile)))

(defn grid-component [tile owner]
  (reify
    om/IRender
    (render [this]
            (dom/div #js {:onMouseDown (fn [e] (om/update! tile (:current-tile @app-state)))
                          :className "tile"
                          :style #js {:width (:width tile)
                                      :height (:height tile)
                                      :backgroundColor (:bgColor tile)
                                      :color (:color tile)
                                      :textAlign "center"}}
                     (dom/span #js {:className "circle"
                                    :style #js {:backgroundColor (:color tile)}}
                               "")))))

(defn legend-component [tile owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [chan]}]
                  (dom/div nil
                    (dom/div #js {:onClick (fn [e] (put! chan @tile))
                                  :className "tile"
                                  :style #js {:width (:width tile)
                                              :height (:height tile)
                                              :backgroundColor (:bgColor tile)
                                              :textAlign "center"}}
                             (dom/span #js {:className "circle"
                                            :style #js {:backgroundColor (:color tile)}}
                                       ""))
                    (dom/div #js {:style #js {:color "yellow"}} "o")))))

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

(defn tile-legend [{:keys [tiles current-tile]} owner]
  (reify
    om/IInitState
    (init-state [_]
                {:update-cur-tile (chan)})
    om/IWillMount
    (will-mount [_]
                (let [update-cur-tile (om/get-state owner :update-cur-tile)]
                  (go (while true
                        (let [tile (<! update-cur-tile)
                              new-tile (assoc tile :type :grid)]
                          (om/update! current-tile new-tile))))))
    om/IRenderState
    (render-state [this {:keys [update-cur-tile]}]
                  (om/build row-component tiles {:init-state {:chan update-cur-tile}}))))

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
    om/IRender
    (render [this]
            (dom/div #js {:style #js {:display "flex"}}
                     (om/build tile-legend {:tiles (:tile-legend app)
                                            :current-tile (:current-tile app)})
                     (om/build grid-view (:tile-grid app))))))

;; render app
(om/root app-view
         app-state
         {:target (. js/document (getElementById "app"))})
