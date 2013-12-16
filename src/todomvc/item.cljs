(ns todomvc.item
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [>! <! put!]]
            [todomvc.utils :refer [now]]
            [clojure.string :as string]
            [om.core :as om]
            [om.dom :as dom :include-macros true]))

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)

;; =============================================================================
;; Todo Item

(defn handle-submit [e todo {:keys [owner comm]}]
  (let [val (.trim (:edit-text todo))]
    (if-not (string/blank? val)
      (go
        (>! comm [:save [todo val]])
        (om/replace! todo [:title] (:edit-text todo)))
      (put! comm [:destroy todo]))
    false))

(defn handle-edit [e todo {:keys [owner comm]}]
  ;; NOTE: we have to grab the node here? - David
  (let [node (dom/get-node owner "editField")]
    (go
      (>! comm [:edit todo])
      (.focus node)
      (.setSelectionRange node (.. node -value -length) (.. node -value -length))
      (om/replace! todo [:edit-text] (:title todo)))))

(defn handle-key-down [e todo opts]
  (let [kc (.-keyCode e)]
    (if (identical? kc ESCAPE_KEY)
      (om/replace! todo [:edit-text] (:title todo))
      (if (identical? kc ENTER_KEY)
        (handle-submit e todo opts)))))

(defn handle-change [e todo]
  (om/replace! todo [:edit-text] (.. e -target -value)))

(defn todo-item [{:keys [id editing completed] :as todo} {:keys [comm]}]
  (reify
    dom/IRender
    (-render [_ owner]
      (let [m {:owner owner :comm comm}
            classes (cond-> []
                      completed (conj "completed")
                      editing   (conj "editing"))]
        (dom/li #js {:className (string/join " " classes)}
          (dom/div #js {:className "view"}
            (dom/input #js {:className "toggle" :type "checkbox"
                            :checked (and completed "checked")
                            :onChange (fn [_] (put! comm [:toggle todo]))})
            (dom/label #js {:onDoubleClick #(handle-edit % todo m)} (:title todo))
            (dom/button #js {:className "destroy"
                             :onClick (fn [_] (put! comm [:destroy todo]))}))
          (dom/input #js {:ref "editField" :className "edit" :value (:edit-text todo)
                          :onBlur #(handle-submit % todo m)
                          :onChange #(handle-change % todo)
                          :onKeyDown #(handle-key-down % todo m)}))))))
