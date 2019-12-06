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

(def state (atom [{:transform "matrix goes here"
                   :name      "cube 1"
                   :mesh      "mesh info goes here"}
                  {:subtractive true
                   :name        "world boundaries"
                   :mesh        "mesh info goes here"}]))

;; * entry point

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

        ;; allow clicking on parts of cube with mouse to change their colour
        projector (js/THREE.Projector.)
        target-list (clj->js [cube])
        _ (.addEventListener js/window
                             "mousedown"
                             (fn [event]
                               (let [x          (- (* (/ (.-clientX event)
                                                         (.-innerWidth js/window))
                                                      2)
                                                   1)
                                     y          (+ (- (* (/ (.-clientY event)
                                                            (.-innerHeight js/window))
                                                         2))
                                                   1)
                                     vector     (js/THREE.Vector3. x y 1)
                                     _          (js/console.log (clj->js {:x (.-x vector)
                                                                          :y (.-y vector)
                                                                          :z (.-z vector)}))
                                     _          (.unproject vector camera)
                                     ray        (js/THREE.Raycaster. (.-position camera)
                                                                     (.normalize (.sub vector (.-position camera))))
                                     intersects (.intersectObjects ray target-list)
                                     _          (js/console.log "intersects" intersects)
                                     _          (when (> (.-length intersects) 0)
                                                  (do (js/console.log "actually did intersect" intersects)
                                                      (.setRGB (.-color (.-face (aget intersects 0)))
                                                               (+ (* 0.8 (js/Math.random)) 0.2)
                                                               0 0)))]))
                             false)

        _ (defn update!
            [dt-ms]
            (let [
                  ;; _ (set! (.-x (.-rotation cube)) (* timer 0.0002))
                  ;; _ (set! (.-z (.-rotation cube)) (* timer 0.0003))
                  ]))

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
