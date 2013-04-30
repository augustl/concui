;; Copyright 2013 August Lilleaas
;; This file is part of concui.
;;
;; concui is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; concui is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public License
;; along with concui. If not, see <http://www.gnu.org/licenses/>.
(ns concui.renderer
  (:require clojure.set)
  (:import [org.lwjgl.opengl
            GL11]))

(defrecord Color [r g b a])
(defn color
  ([r g b] (color r g b 1))
  ([r g b a] (Color. r g b a)))

(defn create-renderer
  []
  {:views (ref {})
   :root-view (ref nil)
   :children-idx (ref {})
   :parents-idx (ref {})
   :bg-color (ref (color 1 0 1))
   :view-tempid (atom 0)})

(defn view-tempid
  [renderer]
  (swap! (:view-tempid renderer) dec))

(defn resolve-view-tempid
  [tx view-tempid]
  (get tx view-tempid))

(defn create-ids-for-tempids
  [tempids]
  (into {} (map
            (fn [id]
              [(str (java.util.UUID/randomUUID)) id])
            tempids)))

(defn get-view-id
  [view-id-or-tempid tempids renderer]
  (or (and (contains? @(:views renderer) view-id-or-tempid) view-id-or-tempid)
      (get @tempids view-id-or-tempid)
      (do
        (let [view-id (str (java.util.UUID/randomUUID))]
          (alter tempids assoc view-id-or-tempid view-id)
          (alter (:views renderer) assoc-in [view-id :_id] view-id)
          view-id))))

(defmulti run-fact (fn [fact tempids renderer] (first fact)))

(defmethod run-fact :view/attr
  [fact tempids renderer]
  (let [view-id (get-view-id (nth fact 1) tempids renderer)
        view-attr-key (nth fact 2)
        view-attr-val (nth fact 3)]
    (alter (:views renderer) assoc-in [view-id view-attr-key] view-attr-val)))

(defmethod run-fact :view/append-child
  [fact tempids renderer]
  (let [parent-view-id (get-view-id (nth fact 1) tempids renderer)
        child-view-id (get-view-id (nth fact 2) tempids renderer)]
    (alter (:children-idx renderer) update-in [parent-view-id] (fn [children] (conj children child-view-id)))))

(defmethod run-fact :renderer/root-view
  [fact tempids renderer]
  (let [view-id-or-tempid (nth fact 1)]
    (ref-set (:root-view renderer) (get-view-id view-id-or-tempid tempids renderer))))

(defmethod run-fact :renderer/bg-color
  [fact tempids renderer]
  (let [color (nth fact 1)]
    (ref-set (:bg-color renderer) color)))

(defn validate-tempids
  [tempids views]
  (let [non-existing-views (clojure.set/difference
                            (set (vals tempids))
                            (set (keys views)))]
    (if (not (empty? non-existing-views))
      (throw (Exception. (str "The following views ids were referenced but not created. TODO: Show tempids, not resolved IDs " non-existing-views))))))

(defn transact
  [renderer facts]
  (future
    (let [tempids (ref {})]
      (dosync
       (doseq [fact facts]
         (run-fact fact tempids renderer))
       (validate-tempids @tempids @(:views renderer)))
      @tempids)))

(defn draw-view
  [view]
  ((:gl view)))

;; TODO: Stack overflow?
(defn draw-view-tree
  [view views children-idx]
  (draw-view view)
  (doseq [child-id (get children-idx (:_id view))]
    (draw-view-tree (get views child-id) views children-idx)))

(defn render
  [renderer]
  (let [renderer-val (dosync
                      {:views @(:views renderer)
                       :root-view @(:root-view renderer)
                       :children-idx @(:children-idx renderer)
                       :parents-idx @(:parents-idx renderer)
                       :bg-color @(:bg-color renderer)})
        views (:views renderer-val)
        root-view (get views (:root-view renderer-val))
        children-idx (:children-idx renderer-val)
        parents-idx (:parents-idx renderer-val)
        bg-color (:bg-color renderer-val)]
    (GL11/glClearColor (:r bg-color) (:b bg-color) (:g bg-color) (:a bg-color))
    (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
    (draw-view-tree root-view views children-idx)))