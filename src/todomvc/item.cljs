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
  (let [edit-text (:edit-text todo)
        val       (.trim edit-text)]
    (if-not (string/blank? val)
      (do
        (om/update! todo #(-> % (assoc :title edit-text) (dissoc :edit-text)))
        (put! comm [:save todo]))
      (put! comm [:destroy todo]))
    false))

(defn handle-edit [e todo {:keys [owner comm]}]
  ;; NOTE: we have to grab the node here? - David
  (let [node (dom/get-node owner "editField")]
    (go
      (om/replace! todo [:edit-text] (:title todo))
      (.focus node)
      (.setSelectionRange node (.. node -value -length) (.. node -value -length))
      (>! comm [:edit todo]))))

(defn handle-key-down [e todo {:keys [owner] :as opts}]
  (let [kc (.-keyCode e)]
    (if (identical? kc ESCAPE_KEY)
      (do
        (om/replace! todo [:edit-text] (:title todo))
        (put! (:comm opts) [:cancel todo]))
      (if (identical? kc ENTER_KEY)
        (handle-submit e todo opts)))))

(defn handle-change [e todo owner]
  (om/replace! todo [:edit-text] (.. e -target -value)))

(defn todo-item [{:keys [id title editing completed] :as todo} {:keys [comm]}]
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
          (dom/input #js {:ref "editField" :className "edit"
                          :value (or title (:edit-text todo))
                          :onBlur #(handle-submit % todo m)
                          :onChange #(handle-change % todo owner)
                          :onKeyDown #(handle-key-down % todo m)}))))))
