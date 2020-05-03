(ns cubed.core
  (:require [cljsjs.three]
            [cljsjs.three-examples.renderers.Projector]
            [cljsjs.three-examples.renderers.SoftwareRenderer]
            [clojure.core.matrix :as ccm]))

;; TODO make one cube and subtractive geom world
;; TODO if subtractive geom world is too hard, just make a "platform" out of immovable cubes
;; TODO move cube with click and drag

;; TODO animation can be as simple as prerendered sprites for the most noticeable parts of rotating cubes
;; this can be coupled with changing colour palettes for different kinds of cubes

;; want to be able to see the exact point in the world that my mouse pointer is affecting
;; want to be able to predict what will happen before i click
;; moving cubes means
;; 1. clicking on a point on a cube
;; 2. dragging to create a force vector along the plane parallel to the relevant face
;; 3. once the vector is large enough/once the force is strong enough, move the cube a fixed distance. OR start out by always moving the cube along with the mouse cursor along that same vector; then add moving in fixed increments.

;; * state

#_(def state (atom [{:transform "matrix goes here"
                   :name      "cube 1"
                   :mesh      "mesh info goes here"}
                  {:subtractive true
                   :name        "world boundaries"
                   :mesh        "mesh info goes here"}]))

(def *state (atom {}))

;; * entry point

(defn- transform-origin-to-center
  [num denom]
  (- (* (/ num denom)
        2)
     1))

(defn init!
  []
  (let [start (.now js/Date)

        ;; add a container <div> tag to the page
        container-id "demo"
        _            (when-let [old (.getElementById js/document container-id)]
                       (.remove old))
        container    (.createElement js/document "div")
        _            (.setAttribute container "id" container-id)
        _            (.appendChild (.-body js/document) container)

        ;; add camera
        camera (js/THREE.PerspectiveCamera. 70 (/ (.-innerWidth js/window) (.-innerHeight js/window)) 1 2000)
        _      (set! (.-z (.-position camera)) 600)

        ;; add scene
        scene (js/THREE.Scene.)
        _     (.set (.-position scene) 100 0 0)

        ;; make a cube with flat colours
        cube-material (js/THREE.MeshBasicMaterial. (clj->js {:color 0xffffff :vertexColors js/THREE.FaceColors}))
        cube-geometry (js/THREE.CubeGeometry. 80 80 80 1 1 1)
        _             (doseq [i    (range (.-length (.-faces cube-geometry)))
                              :let [face (aget (.-faces cube-geometry) i)]]
                        (.setRGB (.-color face) (js/Math.random) (js/Math.random) (js/Math.random)))
        cube          (js/THREE.Mesh. cube-geometry cube-material)
        _             (.add scene cube)

        ;; add renderer
        renderer (js/THREE.SoftwareRenderer.)
        _        (.setSize renderer (.-innerWidth js/window) (.-innerHeight js/window))
        _        (.appendChild container (.-domElement renderer))

        _ (.addEventListener js/window
                             "resize"
                             (fn []
                               (let [_ (set! (.-aspect camera)
                                             (/ (.-innerWidth js/window) (.-innerHeight js/window)))
                                     _ (.updateProjectionMatrix camera)
                                     _ (.setSize renderer (.-innerWidth js/window) (.-innerHeight js/window))]))
                             false)

        _ (defn show-line!
            [u v color]
            (let [line-material (js/THREE.LineBasicMaterial. (clj->js {:color color}))
                  line-geometry (js/THREE.Geometry.)
                  _             (.push (.-vertices line-geometry) u)
                  _             (.push (.-vertices line-geometry) v)
                  line          (js/THREE.Line. line-geometry line-material)
                  _             (.add scene line)]))

        origin (js/THREE.Vector3. 0 0 0)
        _      (show-line! origin (js/THREE.Vector3. 100 0 0) 0xff0000) ;; x
        _      (show-line! origin (js/THREE.Vector3. 0 100 0) 0x00ff00) ;; y
        _      (show-line! origin (js/THREE.Vector3. 0 0 100) 0x0000ff) ;; z

        ;; allow clicking on parts of cube with mouse to change their colour
        projector (js/THREE.Projector.)
        ray       (js/THREE.Raycaster.)
        _         (.addEventListener js/window
                                     "mousedown"
                                     (fn [event]
                                       (let [x           (transform-origin-to-center (.-clientX event)
                                                                                     (.-innerWidth js/window))
                                             y           (- (transform-origin-to-center (.-clientY event)
                                                                                        (.-innerHeight js/window)))
                                             screen-v    (js/THREE.Vector2. x y)
                                             _           (.setFromCamera ray screen-v camera)
                                             target-list (clj->js [cube])
                                             intersects  (.intersectObjects ray target-list)]
                                         (when (> (.-length intersects) 0)
                                           (let [p      (.-point (aget intersects 0))
                                                 worldP (.worldToLocal (.-object (aget intersects 0)) p)
                                                 v      (js/THREE.Vector3. (.-x worldP)
                                                                           (.-y worldP)
                                                                           (.-z worldP))
                                                 _      (show-line! origin v 0xffff00)
                                                 _      (swap! *state #(assoc % :dragging-from v))])))))

        _ (.addEventListener js/window
                             "mouseup"
                             (fn [_event]
                               (when (:dragging-from @*state)
                                 (swap! *state #(dissoc % :dragging-from)))))

        _ (.addEventListener js/window
                             "mousemove"
                             (fn [event]
                               (when (:dragging-from @*state)
                                 (let [x        (transform-origin-to-center (.-clientX event)
                                                                            (.-innerWidth js/window))
                                       y        (- (transform-origin-to-center (.-clientY event)
                                                                               (.-innerHeight js/window)))
                                       screen-v (js/THREE.Vector2. x y)
                                       _        (.setFromCamera ray screen-v camera)
                                       ;; TODO: create a plane parallel to the
                                       ;; face of the object being dragged,
                                       ;; then find intersection with where the
                                       ;; mouse cursor is.
                                       ;; Draw a line from the drag start position
                                       ;; to this intersection point.
                                       ]))))
        ;; target-list (clj->js [cube])
        ;; _ (.addEventListener js/window
        ;;                      "mousedown"
        ;;                      (fn [event]
        ;;                        (let [transform-origin-to-center (fn [num denom]
        ;;                                                           (- (* (/ num denom)
        ;;                                                                 2)
        ;;                                                              1))
        ;;                              x (transform-origin-to-center (.-clientX event)
        ;;                                                            (.-innerWidth js/window))
        ;;                              y (- (transform-origin-to-center (.-clientY event)
        ;;                                                               (.-innerHeight js/window)))
        ;;                              screen-v      (js/THREE.Vector2. x y)
        ;;                              ray (js/THREE.Raycaster.)
        ;;                              _ (.setFromCamera ray screen-v camera)
        ;;                              intersects (.intersectObjects ray target-list)
        ;;                              _          (when (> (.-length intersects) 0)
        ;;                                           (let [p (.-point (aget intersects 0))
        ;;                                                 worldP (.worldToLocal (.-object (aget intersects 0)) p)
        ;;                                                 v (js/THREE.Vector3. (.-x worldP)
        ;;                                                                      (.-y worldP)
        ;;                                                                      (.-z worldP))
        ;;                                                 _ (show-line! origin v 0xffff00)
        ;;                                                 random-colour #(+ (* 0.8 (js/Math.random)) 0.2)]
        ;;                                             (.setRGB (.-color (.-face (aget intersects 0)))
        ;;                                                      (random-colour)
        ;;                                                      (random-colour)
        ;;                                                      (random-colour))))]))
        ;;                      false)

        _ (defn update!
            [dt-ms])

        _ (defn render!
            []
            (let [_ (.render renderer scene camera)]))

        _ (defn animate!
            []
            (let [_     (.requestAnimationFrame js/window animate!)
                  dt-ms (- (.now js/Date) start)
                  _     (update! dt-ms)
                  _     (render!)]))

        _ (animate!)]))

(init!)
