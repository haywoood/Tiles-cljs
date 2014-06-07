(ns tiels.components.grid
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [<!]]))

(defn make-tile-current [tile selected-tile]
  (om/update! tile @selected-tile))

(defn grid-component [app owner {:keys [width]}]
  (reify
    om/IInitState
    (init-state [_]
                {:mouse-chan (chan)
                 :dragging false})

    om/IWillMount
    (will-mount [_]
                (let [mouse-chan (om/get-state owner :mouse-chan)]
                  (go
                   (loop []
                     (let [{:keys [tile etype]} (<! mouse-chan)
                           current (:selected-tile app)
                           dragging (om/get-state owner :dragging)]
                       (case etype
                         "mouseDown" (do
                                       (make-tile-current tile current)
                                       (om/set-state! owner :dragging true))
                         "mouseUp" (om/set-state! owner :dragging false)
                         "mouseEnter" (when dragging (make-tile-current tile current)))
                       (recur))))))

    om/IRenderState
    (render-state [_ {:keys [mouse-chan]}]
                  (apply dom/div #js {:className "grid"
                                      :style #js {:width width}}
                         (om/build-all tile (:tiles app)
                                       {:opts {:mouse-chan mouse-chan}})))))
