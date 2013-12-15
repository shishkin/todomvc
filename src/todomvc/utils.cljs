(ns todomvc.utils
  (:require [clojure.reader :as reader])
  (:import [goog.ui IdGenerator]))

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))

(defn now []
  (js/Date.))

(defn pluralize [n word]
  (if (== n 1)
    word
    (str word "s")))

(defn store
  ([ns] (store ns nil))
  ([ns edn]
    (if-not (nil? edn)
      (.setItem js/localStorage (str edn))
      (let [s (.getItem js/localStorage ns)]
        (if-not (nil? s)
          (reader/read-string s)
          [])))))
