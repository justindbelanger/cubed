(ns cubed.core
  (:require [cljsjs.three]
            [cljsjs.three-examples.renderers.Projector]
            [cljsjs.three-examples.renderers.SoftwareRenderer]
            [clojure.core.matrix :as ccm]))

;; * state

(def *state (atom {}))

;; * entry point

(defn- transform-origin-to-center
  [num denom]
  (- (* (/ num denom)
        2)
     1))

(defn- sign [n]
  (cond
    (< 0 n) 1
    (> 0 n) -1
    :else   0))

(defn- Vector3->vec
  [v]
  [(.-x v)
   (.-y v)
   (.-z v)])

(defn- vec->Vector3
  [v]
  (let [[x y z] v]
    (js/THREE.Vector3. x y z)))

(def grid-size 80)

(def ui-vertex-color 0x00ffff)
(def ui-line-color 0xffff00)

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
        cube-geometry (js/THREE.CubeGeometry. grid-size grid-size grid-size 1 1 1)
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

        _ (defn show-point!
            [v color]
            (let [material (js/THREE.MeshBasicMaterial. (clj->js {:color color}))
                  geometry (js/THREE.SphereGeometry. 5 3 2)
                  sphere   (js/THREE.Mesh. geometry material)
                  _        (.set (.-position sphere)
                                 (.-x v)
                                 (.-y v)
                                 (.-z v))
                  _ (.add scene sphere)]))

        origin (js/THREE.Vector3. 0 0 0)
        _      (show-line! origin (js/THREE.Vector3. 100 0 0) 0xff0000) ;; x
        _      (show-line! origin (js/THREE.Vector3. 0 100 0) 0x00ff00) ;; y
        _      (show-line! origin (js/THREE.Vector3. 0 0 100) 0x0000ff) ;; z

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
                                                 v      (.add (js/THREE.Vector3. (.-x worldP)
                                                                                 (.-y worldP)
                                                                                 (.-z worldP))
                                                              (.-position (.-object (aget intersects 0))))
                                                 _      (show-point! v ui-vertex-color)
                                                 _      (swap! *state #(assoc % :dragging-from v))])))))

        _ (.addEventListener js/window
                             "mouseup"
                             (fn [event]
                               (when (:dragging-from @*state)
                                 (let [x        (transform-origin-to-center (.-clientX event)
                                                                            (.-innerWidth js/window))
                                       y        (- (transform-origin-to-center (.-clientY event)
                                                                               (.-innerHeight js/window)))
                                       screen-v (js/THREE.Vector2. x y)
                                       _        (.setFromCamera ray screen-v camera)

                                       target-list (clj->js [cube])
                                       intersects  (.intersectObjects ray target-list)]
                                   (when (> (.-length intersects) 0)
                                     (let [p             (.-point (aget intersects 0))
                                           worldP        (.worldToLocal (.-object (aget intersects 0)) p)
                                           v             (.add (js/THREE.Vector3. (.-x worldP)
                                                                                  (.-y worldP)
                                                                                  (.-z worldP))
                                                               (.-position (.-object (aget intersects 0))))
                                           dragging-from (:dragging-from @*state)
                                           delta-p       (.sub v dragging-from)
                                           dest-point    (.add (.clone dragging-from)
                                                               delta-p)
                                           _             (show-line! dragging-from dest-point ui-line-color)
                                           _             (show-point! dest-point ui-vertex-color)
                                           _             (.add (.-position cube) delta-p)])))
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

                                       target-list (clj->js [cube])
                                       intersects  (.intersectObjects ray target-list)]
                                   (when (> (.-length intersects) 0)
                                     (let [p             (.-point (aget intersects 0))
                                           worldP        (.worldToLocal (.-object (aget intersects 0)) p)
                                           v             (.add (js/THREE.Vector3. (.-x worldP)
                                                                                  (.-y worldP)
                                                                                  (.-z worldP))
                                                               (.-position (.-object (aget intersects 0))))
                                           dragging-from (:dragging-from @*state)
                                           _             (show-line! dragging-from v ui-line-color)
                                           _             (show-point! v ui-vertex-color)]))))))

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
