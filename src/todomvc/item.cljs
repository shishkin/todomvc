(ns todomvc.item
  (:require React
            [clojure.string :as string]
            [om.core :as om]
            [om.dom :as dom :include-macros true]))

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)

;; =============================================================================
;; Todo Item

(defn handle-submit [e todo owner]
  (let [val (.trim (dom/get-node todo "editText"))
        opts (om/get-opts owner)]
    (if-not (string/blank? val)
      (do
        ((:on-save opts) val)
        (om/set! todo :title (:edit-text data)))
      ((:on-destroy opts)))
    false))

(defn handle-edit [e todo owner]
  ((-> data :handlers :on-edit) 
    (fn []
      (let [node (dom/get-node owner "editField")]
        (.focus node)
        (.setSelectionRange (.. node -value -length) (.. node -value -length)))))
  (om/set! todo :edit-text (:title todo)))

(defn handle-key-down [e todo]
  (if (identical? (.-keyCode e) ESCAPE_KEY)
    (om/set! todo :edit-text (:title todo))
    (handle-submit e todo)))

(defn handle-change [e todo]
  (om/set! todo :edit-text (.. e -target -value)))

(defn todo-item [{:keys [completed editing] :as todo}]
  (reify
    IRender
    (-render [_ owner]
      (dom/li #js {:className (str (and completed "completed") " "
                                (and editing "editing"))}
        (dom/div #js {:className "view"}
          (dom/input #js {:className "toggle"
                           :type "checkbox"
                           :checked (and completed "checked")
                           :onChange (:on-change (get-opts owner))})
          (dom/label #js {:onDoubleClick #(handle-edit % todo owner)}
            (:title todo))
          (dom/button #js {:className "destroy" :onClick (om/bind delete-todo todo)})
          (dom/input #js {:ref "editField"
                           :className "edit"
                           :value (:edit-text todo)
                           :onBlur #(handle-submit % todo owner)
                           :onChange #(handle-change % todo)
                           :onKeyDown (om/bind handle-key-down todo)}))))))
