(ns beat-link-trigger.core
  "Top level organization for starting up the interface, logging, and
  managing online presence."
  (:require [beat-link-trigger.about :as about]
            [beat-link-trigger.logs :as logs]
            [beat-link-trigger.menus :as menus]
            [beat-link-trigger.triggers :as triggers]
            [seesaw.core :as seesaw]
            [taoensso.timbre :as timbre])
  (:import [org.deepsymmetry.beatlink DeviceFinder VirtualCdj]))

(defn try-going-online
  "Search for a DJ link network, presenting a UI in the process."
  []
  (let [searching (about/create-searching-frame)]
    (loop []
      (.setUseStandardPlayerNumber (VirtualCdj/getInstance) (triggers/request-metadata?))
      (if (try (.start (VirtualCdj/getInstance)) ; Make sure we can see some DJ Link devices and start the VirtualCdj
               (catch Exception e
                 (timbre/warn e "Unable to create Virtual CDJ")
                 (seesaw/invoke-now
                  (seesaw/hide! searching)
                  (seesaw/alert (str "<html>Unable to create Virtual CDJ<br><br>" e)
                                :title "DJ Link Connection Failed" :type :error))))
        (seesaw/invoke-soon (seesaw/dispose! searching)) ; We succeeded in finding a DJ Link network
        (do
          (seesaw/invoke-now (seesaw/hide! searching)) ; No luck so far, ask what to do
          (let [options (to-array ["Try Again" "Quit" "Continue Offline"])
                choice (seesaw/invoke-now
                        (javax.swing.JOptionPane/showOptionDialog
                         nil "No DJ Link devices were seen on any network. Search again?"
                         "No DJ Link Devices Found"
                         javax.swing.JOptionPane/YES_NO_OPTION javax.swing.JOptionPane/ERROR_MESSAGE nil
                         options (aget options 0)))]
            (case choice
              0 (do (seesaw/invoke-now (seesaw/show! searching)) (recur)) ; Try Again
              2 (seesaw/invoke-soon (seesaw/dispose! searching))  ; Continue Offline
              (System/exit 1)))))))  ; Quit, or just closed the window, which means the same

  (seesaw/invoke-now
   (triggers/start)))  ; We are online, or the user said to continue offline, so set up the Triggers window.

(defn start
  "Set up logging, make sure we can start the Virtual CDJ, then
  present the Triggers interface. Called when jar startup has detected
  a recent-enough Java version to succcessfully load this namespace."
  [& args]
  (seesaw/native!)  ; Adopt as native a look-and-feel as possible
  (javax.swing.UIManager/setLookAndFeel "org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel")
  (logs/init-logging)
  (timbre/info "Beat Link Trigger starting.")
  (menus/install-mac-about-handler)
  (try-going-online))

(defn try-waveform
  "Temporary function while experimenting with drawing waveforms."
  [id]
  (seesaw/invoke-now
   (let [track (org.deepsymmetry.beatlink.data.DataReference.
                3 org.deepsymmetry.beatlink.CdjStatus$TrackSourceSlot/USB_SLOT id)
         metadata (.requestMetadataFrom (org.deepsymmetry.beatlink.data.MetadataFinder/getInstance) track)
         preview  (.requestWaveformPreviewFrom (org.deepsymmetry.beatlink.data.WaveformFinder/getInstance) track)
         view     (.createViewComponent preview metadata)
         root (seesaw/frame :title (str "Waveform Preview Experimentation, track " id)
                            :on-close :dispose
                            :content view)]
     (seesaw/pack! root)
     (seesaw/show! root)
     view)))

(defn try-waveform-watcher
  "Temporary function while experimenting with drawing player waveform
  views."
  [player]
  (seesaw/invoke-now
   (let [view (org.deepsymmetry.beatlink.data.WaveformPreviewComponent. (int player))
         root (seesaw/frame :title (str "Waveform Watcher Experimentation, player " player)
                            :on-close :dispose
                            :content view)]
     (seesaw/listen root :window-closed
                    (fn [_] (.setMonitoredPlayer view 0)))
     (seesaw/pack! root)
     (seesaw/show! root)
     view)))
