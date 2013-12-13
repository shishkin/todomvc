(ns todomvc.item
  (:require React
            [clojure.string :as string]
            [om.core :as om]
            [om.dom :as dom :include-macros true]))

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)

;; =============================================================================
;; Todo Item

(defn handle-submit [e {:keys [state data path owner]}]
  (let [val (.trim (get-node owner "editText"))]
    (if-not (string/blank? val)
      (do
        ((-> data :handlers :on-save) val)
        (swap! state assoc-in (conj path :title) (:edit-text data)))
      ((-> data :handlers :on-destroy)))
    false))

(defn handle-edit [e {:keys [state data path owner]}]
  ((-> data :handlers :on-edit) 
    (fn []
      (let [node (get-node owner "editField")]
        (.focus node)
        (.setSelectionRange (.. node -value -length) (.. node -value -length)))))
  (swap! state assoc-in (conj path :edit-text) (:title data)))

(defn handle-key-down [e {:keys [state data path]}]
  (if (identical? (.-keyCode e) ESCAPE_KEY)
    (swap! state assoc-in (conj path :edit-text) (:title data))
    (handle-submit)))

(defn handle-change [e {:keys [state path]}]
  (swap! state assoc-in (conj path :edit-text) (.. e -target -value)))

(defn todo-item [{:keys [completed editing] :as todo} path]
  (dom/li #js {:className (str (and completed "completed") " "
                              (and editing "editing"))}
    (dom/div #js {:className "view"}
      (dom/input #js {:className "toggle"
                      :type "checkbox"
                      :checked (and completed "checked")
                      :onChange (om/bind toggle-todo path)})
      (dom/label #js {:onDoubleClick (om/bind edit-todo path)}
        (:title todo))
      (dom/button #js {:className "destroy" :onClick (om/bind delte-todo path)})
      (dom/input #js {:ref "editField"
                      :className "edit"
                      :value (:text todo)
                      :onBlur (om/bind handle-submit todo path)
                      :onChange (om/bind handle-todo-change todo path)
                      :onKeyDown (om/bind handle-todo-key-down todo path)}))))
