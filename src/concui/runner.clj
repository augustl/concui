(ns concui.runner
  (:require [concui.renderer :as r])
  (:import [org.lwjgl.opengl
            Display
            DisplayMode
            GL11]))

(defmacro with-glbegin
  [arg & body]
  `(do
     (GL11/glBegin ~arg)
     ~@body
     (GL11/glEnd)))

(def rdr (r/create-renderer))

(defn render-frame
  []
  (let [bgcolor @(:bg-color rdr)]
    (GL11/glClearColor (:r bgcolor) (:b bgcolor) (:g bgcolor) (:a bgcolor)))
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
  (r/render rdr))

(let [root-view-tempid (r/view-tempid rdr)]
  (println
   @(r/transact
     rdr
     [[:view/attr root-view-tempid :pos-x 20]
      [:view/attr root-view-tempid :pos-y 20]
      [:view/attr root-view-tempid :cl (fn []
                                         (GL11/glColor3f (rand) (rand) (rand))
                                         (GL11/glBegin GL11/GL_TRIANGLES)
                                         (GL11/glVertex2i 0 0)
                                         (GL11/glVertex2i 100 0)
                                         (GL11/glVertex2i 50 50)
                                         (GL11/glEnd))]
      [:renderer/root-view root-view-tempid]
      [:renderer/bg-color (r/color 1 0 0)]])))

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