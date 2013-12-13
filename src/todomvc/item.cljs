(ns todomvc.item
  (:require React
            [clojure.string :as string]
            [om.core :as om]
            [om.dom :as dom :include-macros true]))

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)

;; =============================================================================
;; Todo Item

(defn handle-submit [e {:keys [data] :as todo}]
  (let [val (.trim (get-node owner "editText"))]
    (if-not (string/blank? val)
      (do
        ((-> data :handlers :on-save) val)
        (om/update! todo assoc :title (:edit-text data)))
      ((-> data :handlers :on-destroy)))
    false))

(defn handle-edit [e {:keys [data] :as todo}]
  ((-> data :handlers :on-edit) 
    (fn []
      (let [node (get-node owner "editField")]
        (.focus node)
        (.setSelectionRange (.. node -value -length) (.. node -value -length)))))
  (om/update! todo assoc :edit-text (:title data)))

(defn handle-key-down [e {:keys [data] :as todo}]
  (if (identical? (.-keyCode e) ESCAPE_KEY)
    (om/update! todo assoc :edit-text (:title data))
    (handle-submit)))

(defn handle-change [e todo]
  (om/update! todo assoc :edit-text (.. e -target -value)))

(defn todo-item [{:keys [completed editing] :as todo} path]
  (dom/component
    (dom/li #js {:className (str (and completed "completed") " "
                                 (and editing "editing"))}
      (dom/div #js {:className "view"}
        (dom/input #js {:className "toggle"
                        :type "checkbox"
                        :checked (and completed "checked")
                        :onChange ((-> todo :handlers :on-toggle))})
        (dom/label #js {:onDoubleClick (om/bind handle-edit todo path)}
          (:title todo))
        (dom/button #js {:className "destroy" :onClick (om/bind delete-todo todo path)})
        (dom/input #js {:ref "editField"
                        :className "edit"
                        :value (:edit-text todo)
                        :onBlur (om/bind handle-submit todo path)
                        :onChange (om/bind handle-todo-change todo path)
                        :onKeyDown (om/bind handle-todo-key-down todo path)})))))
