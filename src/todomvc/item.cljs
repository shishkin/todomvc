(ns todomvc.item
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [>! put!]]
            [todomvc.utils :refer [now]]
            [clojure.string :as string]
            [om.core :as om :include-macros true]
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

(defn handle-edit [e {:keys [title] :as todo} {:keys [owner comm]}]
  ;; NOTE: we have to grab the node here? - David
  (let [node (om/get-node owner "editField")]
    (go
      (om/update! todo #(assoc % :edit-text title))
      (.focus node)
      (.setSelectionRange node (.. node -value -length) (.. node -value -length))
      (>! comm [:edit todo]))))

(defn handle-key-down [e {:keys [title] :as todo} {:keys [owner] :as opts}]
  (let [kc (.-keyCode e)]
    (if (identical? kc ESCAPE_KEY)
      (do
        (om/update! todo #(assoc % :edit-text title))
        (put! (:comm opts) [:cancel todo]))
      (if (identical? kc ENTER_KEY)
        (handle-submit e todo opts)))))

(defn handle-change [e todo owner]
  (om/update! todo
    #(assoc % :edit-text (.. e -target -value))))

(defn todo-item [{:keys [id title editing completed] :as todo} {:keys [comm]}]
  (reify
    om/IRender
    (-render [_ owner]
      (let [m {:owner owner :comm comm}
            classes (cond-> []
                      completed (conj "completed")
                      editing   (conj "editing"))]
        (dom/li #js {:className (string/join " " classes)}
          (dom/div #js {:className "view"}
            (dom/input #js {:className "toggle" :type "checkbox"
                            :checked (and completed "checked")
                            :onChange (fn [_] (om/update! todo [:completed] #(not %)))})
            (dom/label #js {:onDoubleClick #(handle-edit % todo m)} (:title todo))
            (dom/button #js {:className "destroy"
                             :onClick (fn [_] (put! comm [:destroy todo]))}))
          (dom/input #js {:ref "editField" :className "edit"
                          :value (or title (:edit-text todo))
                          :onBlur #(handle-submit % todo m)
                          :onChange #(handle-change % todo owner)
                          :onKeyDown #(handle-key-down % todo m)}))))))
