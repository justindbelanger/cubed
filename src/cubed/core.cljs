(ns cubed.core
  (:require [cljsjs.three]
            [cljsjs.three-examples.renderers.Projector]
            [cljsjs.three-examples.renderers.SoftwareRenderer]
            [clojure.core.matrix :as ccm]))

;; TODO make one cube and subtractive geom world
;; TODO if subtractive geom world is too hard, just make a "platform" out of immovable cubes
;; TODO move cube with click and drag

;; * graphics

;; * state

(def state (atom [{:transform "matrix goes here"}
                  {:tags #{:world}}]))

;; * entry point

(def *cube (atom nil))
(def *renderer (atom nil))
(def *camera (atom nil))
(def *scene (atom nil))

(def start (.now js/Date))

(defn init
  []
  (let [
        ;; add a container <div> tag to the page
        container-id "demo"
        _            (when-let [old (.getElementById js/document container-id)]
                       (.remove old))
        container    (.createElement js/document "div")
        _            (.setAttribute container "id" container-id)
        _            (.appendChild (.-body js/document) container)

        ;; add camera and scene
        camera       (js/THREE.PerspectiveCamera. 70 (/ (.-innerWidth js/window) (.-innerHeight js/window)) 1 2000)
        _            (reset! *camera camera)
        _            (set! (.-z (.-position camera)) 600)
        scene        (js/THREE.Scene.)
        _            (.set (.-position scene) 100 0 0)
        _            (reset! *scene scene)

        ;; ;; add a cube with flat, random colours
        ;; box          (.toNonIndexed (js/THREE.BoxBufferGeometry. 200 200 200))
        ;; ;; FIXME - the below code doesn't work; not sure why...
        ;; colour  (js/THREE.Color.)
        ;; colours (->> (repeatedly (fn []
        ;;                            (.setHex colour (* (js/Math.random) 0xffffff))
        ;;                            [(.-r colour) (.-g colour) (.-b colour)]))
        ;;              (take (.-count (.-position (.-attributes box))))
        ;;              (apply concat)
        ;;              vec)
        ;; _       (.addAttribute box "color" (js/THREE.Float32BufferAttribute. (clj->js colours) 3))
        ;; cube (js/THREE.Mesh. box (js/THREE.MeshBasicMaterial. (clj->js {:color 0x00ff00 :vertexColors js/THREE.VertexColors})))
        ;; _    (.add scene cube)
        ;; _ (reset! *cube cube)

        cube-material (js/THREE.MeshBasicMaterial. (clj->js {:color 0xffffff :vertexColors js/THREE.FaceColors}))
        cube-geometry (js/THREE.CubeGeometry. 80 80 80 1 1 1)
        _ (doseq [i    (range (.-length (.-faces cube-geometry)))
                  :let [face (aget (.-faces cube-geometry) i)]]
            (.setRGB (.-color face) (js/Math.random) (js/Math.random) (js/Math.random)))
        cube (js/THREE.Mesh. cube-geometry cube-material)
        ;; _ (.set (.-position cube) -100 50 0)
        _ (reset! *cube cube)
        _ (.add scene cube)

        ;; add renderer
        renderer (js/THREE.SoftwareRenderer.)
        _        (.setSize renderer (.-innerWidth js/window) (.-innerHeight js/window))
        _ (.appendChild container (.-domElement renderer))
        _ (reset! *renderer renderer)

        _ (.addEventListener js/window
                             "resize"
                             (fn []
                               (let [_ (set! (.-aspect camera)
                                             (/ (.-innerWidth js/window) (.-innerHeight js/window)))
                                     _ (.updateProjectionMatrix camera)
                                     _ (.setSize renderer (.-innerWidth js/window) (.-innerHeight js/window))]))
                             false)]))

(defn render
  []
  (let [timer    (- (.now js/Date) start)
        cube     @*cube
        _        (set! (.-x (.-rotation cube)) (* timer 0.0002))
        _        (set! (.-z (.-rotation cube)) (* timer 0.0003))
        renderer @*renderer
        scene    @*scene
        camera   @*camera
        _        (.render renderer scene camera)]))

(defn animate
  []
  (let [_ (.requestAnimationFrame js/window animate)
        _ (render)]))

(init)
(animate)
