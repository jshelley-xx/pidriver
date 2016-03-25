(ns pidriver-gui.top-window
  (:use [seesaw.core])
  (:use [seesaw.tree])

  (:require [pidriver.utils :as utils])
  (:require [pidriver-gui.utils :refer [fetch-up set-hourglass-icon set-ready-icon set-status]])
  (:require [clojure.core.async
               :as a
               :refer [>! <! >!! <!! go chan buffer close! thread
                       alts! alts!! timeout]])
  (:import org.pushingpixels.substance.api.SubstanceLookAndFeel))


(def category "pidriver-gui.top-window")
(def LOG (org.apache.logging.log4j.LogManager/getLogger category))
;(def get-setting (utils/get-setting-fallback-to-app-setting category))


(def status-pollers (atom {}))



(defn get-boxes []
   [
   {:name "box1" :title "Box 1" :address "192.168.23.105"}
  ])
(defn get-url [m call]
  (str "http://" (:address m) call)
  )



(defn make-id [p m]
  (keyword (str p "-" (:name m))))

(defn make-selector [p m]
  (keyword (str "#" p "-" (:name m))))





(defn make-video-list-model [m]
  (utils/get-json
      (get-url m "/omxplayer-ui/servers/piloader/")
      (fn [json]
        (let [s (map #(%1 "file") (json "content"))]
        (listbox
          :id (make-id "file-list" m)
          :model (-> (into [] s) sort))))))






(defn make-poller [frame box]
  (let [status (select frame [:#status])
    instance (:instance (user-data status))]
      (go
        (.debug LOG (str "starting status poller for " (str (:name box))))
        (while
          (not= "stopped" (get @status-pollers instance))
            (do
              (.debug LOG (str "fetching status for " (str (:name box))))
              (utils/get-json (get-url box "/omxplayer-ui/status")
                (fn [j]
                  (let [
                    status-field (select frame [(make-selector "status-field" box)])
                    position-field (select frame [(make-selector "position-field" box)])
                    playtime-field (select frame [(make-selector "playtime-field" box)])

                    file-list (select frame [(make-selector "file-list" box)])

                    play-button (select frame [(make-selector "play-button" box)])
                    pause-button (select frame [(make-selector "pause-button" box)])
                    stop-button (select frame [(make-selector "stop-button" box)])

                    running (j "running")
                  ]
                  (set-ready-icon frame)
                    (if running
                      (do
                        ;running!
                        (text! status-field (str "Playing: " (j "file")))
                        (text! position-field (j "position"))
                        (text! playtime-field (j "playtime"))

                        (config! file-list :enabled? false)
                        (selection! file-list (j "file"))



                        (config! play-button :enabled? false)

                        (config! pause-button :enabled? true)
                        (config! pause-button :text (if (= (j "status") "Paused") "Resume" "Pause"))

                        (config! stop-button :enabled? true)
                        )
                      (do
                        ;not running
                        (text! status-field "Stopped")
                        (text! position-field "0:00")
                        (text! playtime-field "0:00")

                        (config! file-list :enabled? true)

                        (config! play-button :enabled? true)
                        (config! pause-button :enabled? false)
                        (config! stop-button :enabled? false)

                        )))))
                (Thread/sleep 1000))))))



(defn add-pollers [frame boxes]
  (doseq [box boxes]
    (.info LOG (str "adding status poller to " (:name box)))
    (make-poller frame box))
  (listen frame :window-closing
    (fn [event]
      (let [status (select frame [:#status])
        instance (:instance (user-data status))]
          (.debug LOG (str "shutting down instance: " instance))
          (swap! status-pollers assoc instance "stopped")
          )))
    frame)


(defn make-status-section [m]
  (let [play-button
          (button :id (make-id "play-button" m )
            :text "Play"
            :font "SAN_SERIF-15")
        pause-button
                (button :id (make-id "pause-button" m )
                  :text "Pause"
                  :font "SAN_SERIF-15")
        stop-button
          (button :id (make-id "stop-button" m )
            :text "Stop"
            :font "SAN_SERIF-15")
        panel (grid-panel
                 :columns 1
                 :items [
                    (flow-panel
                      :items [
                        (label :id (make-id "status-field" m )
                          :text "Stopped"
                          :font "SAN_SERIF-20")
                        (label :text "   "
                          :font "SAN_SERIF-20")
                      ])
                      (flow-panel
                        :items [
                          (label :id (make-id "position-field" m )
                            :text "0:00"
                            :font "SAN_SERIF-15")
                            (label :text " / "
                              :font "SAN_SERIF-15")

                          (label :id (make-id "playtime-field" m )
                            :text "0:00"
                            :font "SAN_SERIF-15")
                        ])
                        (flow-panel
                          :items [
                            play-button
                            (label :text " "
                              :font "SAN_SERIF-15")
                            pause-button
                            (label :text " "
                              :font "SAN_SERIF-15")
                            stop-button
                            ])])]

               (listen play-button :action
                 (fn [e]
                   (let [
                     src-cmp (.getSource e)
                     wrapper-panel (fetch-up src-cmp :id :wrapper-frame)
                     file-list (select wrapper-panel [(make-selector "file-list" m)])
                     to-play (selection file-list)
                     ]
                    (if
                      (not-empty to-play)
                      (do
                       (set-hourglass-icon (.getSource e))
                       (utils/post-json
                         (get-url m (str "/omxplayer-ui/servers/piloader/" to-play))
                         {"file" (str "piloader/" to-play) "command" "play"}))
                      (alert "Please Pick a video to play"))
                     )))
               (listen pause-button :action
                 (fn [e]
                   (set-hourglass-icon (.getSource e))
                   (utils/post-json
                     (get-url m (str "/omxplayer-ui/control"))
                     {"file" "null" "command" "pause"})))
               (listen stop-button :action
                 (fn [e]
                   (set-hourglass-icon (.getSource e))
                   (utils/post-json
                     (get-url m (str "/omxplayer-ui/control"))
                     {"file" "null" "command" "stop"})))
               panel))




(defn make-tab [m]
  { :title (:title m)
    :content (border-panel
                :border 5
                :id (make-id "panel" m)
               :hgap 15
               :vgap 5
       :west  (make-video-list-model m)
        :center (border-panel
                  :border 5
                  :id (make-id "status" m)
                  :hgap 15
                  :vgap 5
                  :north  (make-status-section m)))})


(defn maximize [frame]
  (.setExtendedState frame (bit-xor (.getExtendedState frame) javax.swing.JFrame/MAXIMIZED_BOTH))
  frame)


(defn make-frame [boxes]
  (let [
    instance (str "instance-" (System/currentTimeMillis))
    started (swap! status-pollers assoc instance "running")
    f
    (frame
      :title "title" ;(get-setting [:title])
      :id :wrapper-frame
      :on-close :dispose
      ;:menubar (make-menu)
      :content
        (border-panel
          :border 5
           :hgap 5
           :vgap 5
           :north  (tabbed-panel
              :tabs (into [] (map make-tab boxes))
              :font "SAN_SERIF-60"
             )
            :south (text :id :status
                    :user-data {:instance instance}
                    :editable? false
                    :font "MONOSPACED-PLAIN-20")))]
    f)
  )




(defn run [& args]
  (swap! status-pollers assoc :status "running")
  ;(.info LOG "woohoo")
  (org.pushingpixels.substance.api.SubstanceLookAndFeel/setSkin "org.pushingpixels.substance.api.skin.RavenSkin")
  ;(org.pushingpixels.substance.api.SubstanceLookAndFeel/setSkin (get-setting [:skin]))
  (invoke-later
    (let [
      boxes (get-boxes)
      frame (make-frame boxes)
      ]
      (->
        frame
        maximize
        pack!
        show!
        (add-pollers boxes)
        ))))
