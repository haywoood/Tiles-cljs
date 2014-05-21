(ns tiels.state
  (:require [cljs-uuid.core :as uuid]))

(defn uuid []
  (uuid/make-random))

(defn create-tile [attrs]
  (merge tile attrs))

(defn create-multiple-tiles [props]
  (mapv create-tile props))

(def tile {:id (uuid)
           :type :grid
           :width 8
           :height 15
           :bgColor "white"
           :color "red"})

(defn tile-row
  "Creates a vector of n tiles"
  [n]
  (into [] (take n (repeat tile))))

(defn make-grid
  "Creates a 2D vector of n rows and n col in each row"
  [row col]
  (into [] (take row (repeat (tile-row col)))))

(def legend-tiles (create-multiple-tiles [{:type :legend :bgColor "#444" :color "white"}
                                          {:type :legend :bgColor "red" :color "white"}
                                          {:type :legend :bgColor "pink" :color "white"}
                                          {:type :legend :bgColor "blue" :color "white"}
                                          {:type :legend :bgColor "yellow" :color "red"}]))

(def app-state (atom {:tile-grid (make-grid 30 60)
                      :current-tile (assoc (nth legend-tiles 3) :type :grid)
                      :tile-legend legend-tiles}))
