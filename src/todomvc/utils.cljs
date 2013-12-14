(ns todomvc.utils
  (:import [goog.ui IdGenerator]))

(defn guid []
  (IdGenerator/getNextUniqueId))

(defn now []
  (js/Date.))

(defn pluralize [n word]
  (if (== n 1)
    word
    (str word "s")))
