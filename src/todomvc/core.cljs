(ns todomvc
  (:require React
            [om.core :as om]
            [om.dom :as dom :include-macros true]))

(def ENTER_KEY 13)

(def app-state (atom {:todos []}))

;; =============================================================================
;; Todo Item

(defn edit-todo [e m])

(defn delete-todo [e m])

(defn toggle-todo [e m])

(defn save-todo [e m]
  )

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
                      :onBlur (om/bind save-todo path)
                      :onChange (om/bind handle-todo-change path)
                      :onKeyDown (om/bind handle-todo-key-down path)}))))

;; =============================================================================
;; Todos

(defn toggle-all [e]
  (let [checked (.. e -target -checked)]
    (swap! app-state update-in [:todos]
      (fn [todos] (map (fn [todo] (assoc todo :completed checked)) todos)))))

(defn handle-new-todo-keydown [e {:keys [state path owner]}]
  (when (not (identical? (.-which e) ENTER_KEY))
    (swap! state update-in (conj path :todos) conj
      {:id (uuid)
       :title (.-value (dom/get-node owner "newField"))
       :completed false})))

(defn main [todos path]
  (dom/section #js {:id "main"}
    (dom/input #js {:id "toggle-all"
                    :type "checkbox"
                    :onChange toggle-all})
    (dom/ul #js {:id "todo-list"}
      (into-array
        (map #(om/render todo-item todos path %)
          todos (range (count todos)))))))

(defn footer [data path]
  (dom/footer #js {:id "footer"}
    (dom/span #js {:id "todo-count"}
      (dom/strong nil (count todos))
      (str " " active-todo-word " " left))
    (dom/ul #js {:id filters}
      (dom/li nil
        (dom/a #js {:href "#/" :className "all"}
          "All"))
      (dom/li nil
        (dom/a #js {:href "#/active" :className "active"}
          "Active"))
      (dom/li nil
        (dom/a #js {:href "#/completed" :className "completed"}
          "Completed")))))

(defn todo-app []
  (om/root
    app-state
    (fn [data path]
      (dom/div nil
        (dom/header #js {:id "header"}
          #js [(dom/h1 nil "todos")
               (om/bind dom/input
               #js {:ref "newField"
                    :id "new-todo"
                    :placeholder "What needs to be done?"
                    :onKeyDown (om/bind handle-new-todo-keydown path)})
               (om/render main data path :todos)
               (om/render footer data path)])))
    js/document.body))
