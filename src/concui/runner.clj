(ns concui.runner
  (:require [concui.renderer :as r])
  (:import [org.lwjgl.opengl
            Display
            DisplayMode
            GL11]))

;; Changes to the renderer state doesn't instantly update the UI. The
;; render loop will read out the renderer state for each frame, making it free of
;; time complexity.

(defn set-clipping-volume
  [width height]
  (GL11/glMatrixMode GL11/GL_PROJECTION)
  (GL11/glLoadIdentity)
  ;; This isn't right. Need to figure out how OpenGL's coordinate system actually works :)
  (let [left 0
        right width
        top height
        bottom 0
        z-near 1.0
        z-far -1.0]
    (GL11/glOrtho left right bottom top z-near z-far))
  (GL11/glMatrixMode GL11/GL_MODELVIEW)
  (GL11/glLoadIdentity))

;; This is just one of many ways we can actually run this. Essentially the renderer
;; is just a bunch of OpenGL calls, so we're free to not use the default lwjgl loop
;; if we don't want to. Such as having multiple rendering threads.
(defn run
  [rdr]
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
           ;; FPS is 2 to make manual debugging easier
           (Display/sync 2)
           (if (Display/wasResized)
             (set-clipping-volume (Display/getWidth) (Display/getHeight)))
           (r/render rdr)
           (Display/update))
         (Display/destroy)))
    (.setName "Render loop ")
    (.start)
    (.join)))