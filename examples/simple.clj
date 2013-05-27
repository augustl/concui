;; Setup
(do
  (require 'clojure.tools.nrepl.server)
  (require 'concui.runner)
  (require '[concui.renderer :as r])
  (import [org.lwjgl.opengl GL11])

  (def rdr (r/create-renderer))

  (.start
   (Thread.
    (fn [] (concui.runner/run rdr)))))

;; Initial set of triangle views
(do
  (def root-view-tempid (r/view-tempid rdr))
  (def child-a-tempid (r/view-tempid rdr))
  (def child-b-tempid (r/view-tempid rdr))

  ;; To change the view state, use a transaction. This ends up
  ;; as changing multiple values via Clojure STM (refs, and dosync),
  ;; and updating some indexes etc. You could just mamually update
  ;; the state, but the indexes and validations makes that inconvenient.
  ;;
  ;; Similar to datomic, tempids are used for referential integrity
  ;; in transaction creation. After the transaction commits, the tempids
  ;; resolve to the actual view ids.
  (def initial-tx
    @(r/transact
      rdr
      [;; pos-x and pox-y is actually ignored now.
       [:view/attr root-view-tempid :pos-x 20]
       [:view/attr root-view-tempid :pos-y 20]
       ;; :gl is a function that executes OpenGL commands. This
       ;; can be any command, such as texture mapping polygons to
       ;; render a button, or drawing a triangle, or whatever.
       [:view/attr root-view-tempid :gl (fn []
                                          (GL11/glColor3f (rand) (rand) (rand))
                                          (GL11/glBegin GL11/GL_TRIANGLES)
                                          (GL11/glVertex2i 0 0)
                                          (GL11/glVertex2i 100 0)
                                          (GL11/glVertex2i 50 50)
                                          (GL11/glEnd))]

       [:view/attr child-a-tempid :gl (fn []
                                        (GL11/glColor3f 1 (rand) 0.5)
                                        (GL11/glBegin GL11/GL_TRIANGLES)
                                        (GL11/glVertex2i 0 0)
                                        (GL11/glVertex2i 50 0)
                                        (GL11/glVertex2i 20 10)
                                        (GL11/glEnd))]
       [:view/attr child-b-tempid :gl (fn []
                                        (GL11/glColor3f (rand) 1 0.5)
                                        (GL11/glBegin GL11/GL_TRIANGLES)
                                        (GL11/glVertex2i 0 0)
                                        (GL11/glVertex2i 60 0)
                                        (GL11/glVertex2i 70 20)
                                        (GL11/glEnd))]
       [:view/append-child root-view-tempid child-a-tempid]
       [:view/append-child root-view-tempid child-b-tempid]
       ;; Some properties are about the renderer itself, such as the
       ;; view that is the root view, and the bg color of the entire
       ;; viewport.
       [:renderer/root-view root-view-tempid]
       [:renderer/bg-color (r/color 1 0 0)]]))

  (def root-view-id (r/resolve-view-tempid initial-tx root-view-tempid)))

;; Can be changed at any time.
@(r/transact rdr [[:renderer/bg-color (r/color 1 1 0)]])
;; We use the resolved root view id to update the gl (function that calls OpenGL stuffs)
;; of the view to change the colors and position of the triangle.
@(r/transact rdr [[:view/attr root-view-id :gl (fn []
                                                 (GL11/glColor3f 1 1 0)
                                                 (GL11/glBegin GL11/GL_TRIANGLES)
                                                 (GL11/glVertex2i 0 0)
                                                 (GL11/glVertex2i 120 0)
                                                 (GL11/glVertex2i 70 50)
                                                 (GL11/glEnd))]])