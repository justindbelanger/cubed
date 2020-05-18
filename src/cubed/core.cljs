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

(defn- constrain-force
  "Constrains force to be an integer multiple of snap. Rounds up for positive numbers; rounds down for negative numbers."
  [force snap]
  (* (sign force)
     (js/Math.ceil (/ (js/Math.abs force) snap))
     snap))

(defn- Vector3->vec
  [v]
  [(.-x v)
   (.-y v)
   (.-z v)])

(defn- vec->Vector3
  [v]
  (let [[x y z] v]
    (js/THREE.Vector3. x y z)))

(defn- constrain-v
  [v snap]
  (->> v
       Vector3->vec
       (mapv #(* 2 %))
       (mapv #(constrain-force % snap))
       vec->Vector3))

(def grid-size 80)

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
                                                 v      (js/THREE.Vector3. (.-x worldP)
                                                                           (.-y worldP)
                                                                           (.-z worldP))
                                                 _      (show-line! origin v 0xffff00)
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
                                           v             (js/THREE.Vector3. (.-x worldP)
                                                                            (.-y worldP)
                                                                            (.-z worldP))
                                           dragging-from (:dragging-from @*state)
                                           constrained   (constrain-v v grid-size)
                                           delta-p       (.sub v dragging-from)
                                           constrained   (constrain-v delta-p grid-size)
                                           _             (show-line! dragging-from constrained 0xff0000)
                                           _             (.add (.-position cube) constrained)])))
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
                                     (let [p (.-point (aget intersects 0))
                                           worldP (.worldToLocal (.-object (aget intersects 0)) p)
                                           v (js/THREE.Vector3. (.-x worldP)
                                                                (.-y worldP)
                                                                (.-z worldP))
                                           dragging-from (:dragging-from @*state)
                                           _ (show-line! dragging-from v 0x0ffff)]))))))

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
