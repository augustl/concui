(ns concui.runner
  (:require [concui.renderer :as r])
  (:import [org.lwjgl.opengl
            Display
            DisplayMode
            GL11]))

;; The state of the UI
(def rdr (r/create-renderer))

(let [root-view-tempid (r/view-tempid rdr)
      ;; To change the view state, use a transaction. This ends up
      ;; as changing multiple values via Clojure STM (refs, and dosync),
      ;; and updating some indexes etc. You could just mamually update
      ;; the state, but the indexes and validations makes that inconvenient.
      ;;
      ;; Similar to datomic, tempids are used for referential integrity
      ;; in transaction creation. After the transaction commits, the tempids
      ;; resolve to the actual view ids.
      tx @(r/transact
           rdr
           [;; pos-x and pox-y is actually ignored now.
            [:view/attr root-view-tempid :pos-x 20]
            [:view/attr root-view-tempid :pos-y 20]
            ;; The cl is a function that executes OpenGL commands. This
            ;; can be any command, such as texture mapping polygons to
            ;; render a button.
            [:view/attr root-view-tempid :cl (fn []
                                               (GL11/glColor3f (rand) (rand) (rand))
                                               (GL11/glBegin GL11/GL_TRIANGLES)
                                               (GL11/glVertex2i 0 0)
                                               (GL11/glVertex2i 100 0)
                                               (GL11/glVertex2i 50 50)
                                               (GL11/glEnd))]
            ;; Some properties are about the renderer itself, such as the
            ;; view that is the root view, and the bg color of the entire
            ;; viewport.
            [:renderer/root-view root-view-tempid]
            [:renderer/bg-color (r/color 1 0 0)]])
      root-view-id (r/resolve-view-tempid tx root-view-tempid)]
  (comment
    ;; Can be changed at any time.
    @(r/transact rdr [[:renderer/bg-color (r/color 0 1 1)]])
    ;; We use the resolved root view id to update the cl (command list function)
    ;; of the view to change the colors and position of the triangle.
    @(r/transact rdr [[:view/attr root-view-id :cl (fn []
                                                     (GL11/glColor3f 1 (rand) 1)
                                                     (GL11/glBegin GL11/GL_TRIANGLES)
                                                     (GL11/glVertex2i 0 0)
                                                     (GL11/glVertex2i 120 0)
                                                     (GL11/glVertex2i 70 50)
                                                     (GL11/glEnd))]])))


;; Boring render loop stuff below

;; Basically, changes to the renderer state doesn't instantly update the UI. The
;; render loop will read out the renderer state for each frame, making it free of
;; time complexity.

(defn set-clipping-volume
  [width height]
  (GL11/glMatrixMode GL11/GL_PROJECTION)
  (GL11/glLoadIdentity)
  (let [left 0
        right width
        top height
        bottom 0
        z-near 1.0
        z-far -1.0]
    (GL11/glOrtho left right bottom top z-near z-far))
  (GL11/glMatrixMode GL11/GL_MODELVIEW)
  (GL11/glLoadIdentity))

(defn render-frame
  []
  (let [bgcolor @(:bg-color rdr)]
    (GL11/glClearColor (:r bgcolor) (:b bgcolor) (:g bgcolor) (:a bgcolor)))
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
  (r/render rdr))

(defn run
  []
  (doto
      (Thread.
       (fn []
         (Display/setTitle "Hai!")
         (Display/setDisplayMode (DisplayMode. 800 600))
         (Display/setResizable true)
         (Display/setVSyncEnabled true)
         (Display/create)

         (set-clipping-volume (Display/getWidth) (Display/getHeight))
         (while (not (Display/isCloseRequested))
           (Display/sync 2)
           (if (Display/wasResized)
             (set-clipping-volume (Display/getWidth) (Display/getHeight)))
           (render-frame)
           (Display/update))
         (Display/destroy)))
    (.setName "Render loop ")
    (.start)
    (.join)))