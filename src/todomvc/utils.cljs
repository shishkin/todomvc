(ns todomvc.utils)

(defn now []
  (js/Date.))

(defn pluralize [n word]
  (if (== n 1)
    word
    (str word "s")))
