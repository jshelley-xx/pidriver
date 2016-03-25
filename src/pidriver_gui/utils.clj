(ns pidriver-gui.utils
  (:use [seesaw.core])
  )


(defn- safe-config [elm key]
  (try
    (config elm key)
    (catch Exception e
      nil)))


(defn fetch-up [elm key val]
  (if (nil? elm)
    nil
  		(let [val-of-elm (safe-config elm key)]
  			(if (and (not (nil? val-of-elm)) (= val-of-elm val))
  				elm
  			 (fetch-up (.getParent elm) key val)))))


(defn set-hourglass-icon [src-cmp]
  (let [r (.getRootPane src-cmp)]
    (.setCursor r (new java.awt.Cursor java.awt.Cursor/WAIT_CURSOR))))


(defn set-ready-icon [src-cmp]
  (let [r (.getRootPane src-cmp)]
    (.setCursor r (new java.awt.Cursor java.awt.Cursor/DEFAULT_CURSOR))))


(defn set-status [e s]
  (let [
      wrapper-frame (fetch-up e :id :wrapper-frame)
      status (select wrapper-frame [:#status])
    ]
    (text! status s)))
