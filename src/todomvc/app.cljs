(ns todomvc.app
  (:require [om.core :as om]
            [om.dom :as dom :include-macros true]
            [todomvc.utils :refer [pluralize now]]
            [todomvc.item :as todo-item]))

(def ENTER_KEY 13)

(def app-state (atom {:todos []}))

;; =============================================================================
;; Todos

(defn toggle-all [e todos]
  (let [checked (.. e -target -checked)]
    (om/replace! todos
      (into [] (map #(assoc % :completed checked) todos)))))

(defn handle-new-todo-keydown [e app owner]
  (when (not (identical? (.-which e) ENTER_KEY))
    (om/update! app [:todos] conj
      {:id (uuid)
       :title (.-value (dom/get-node owner "newField"))
       :completed false})))

(defn main [todos chans]
  (dom/component
    (dom/section #js {:id "main"}
      (dom/input #js {:id "toggle-all"
                      :type "checkbox"
                      :onChange #(toggle-all % todos)})
      (dom/ul #js {:id "todo-list"}
        (into-array
          (map #(om/render todo-item todos path [%] chans)
            todos (range (count todos))))))))

(defn footer [{:keys [todos] :as app} opts]
  (let [{:keys [active completed showing chans]} opts
        clear-button (when (pos? completed)
                       (dom/buttton
                         #js {:id "clear-completed"
                              :onClick #(put! (:clear chans) (now))}
                         (str "Clear completed " completed)))
        selected (zipmap [:all :active :completed] (repeat "select"))]
    (dom/component
      (dom/footer #js {:id "footer"}
        (dom/span #js {:id "todo-count"}
          (dom/strong nil (count todos))
          (str " " (pluralize active "item") " " left))
        (dom/ul #js {:id filters}
          (dom/li nil
            (dom/a #js {:href "#/" :className (selected showing)}
              "All"))
          (dom/li nil
            (dom/a #js {:href "#/active" :className (selected showing)}
              "Active"))
          (dom/li nil
            (dom/a #js {:href "#/completed" :className (selected showing)}
              "Completed"))
          clear-button)))))

(defn todo-app []
  (let [[toggle destroy edit save clear cancel :as cs] (take 6 (repeat chan))
        chans (zipmap [:toggle :destroy :edit :save :clear :cancel] cs)]
    (om/root
      app-state
      (fn [{:keys [todos] :as app}]
        (reify
          dom/IWillMount
          (-will-mount [_ _]
            (go
              (while true
                (alt!
                  toggle ([v])
                  destroy ([v])
                  edit ([v])
                  save ([v])
                  clear ([v])
                  cancel ([v])))))
          dom/IRender
          (-render [_ owner]
            (let [active (reduce
                           #(if (:completed %2) (inc %1) %1)
                           0 todos)
                  completed (- (count todos) active)]
              (dom/div nil
                (dom/header #js {:id "header"}
                  #js [(dom/h1 nil "todos")
                       (om/bind dom/input
                         #js {:ref "newField"
                              :id "new-todo"
                              :placeholder "What needs to be done?"
                              :onKeyDown #(handle-new-todo-keydown % app owner)})
                       (om/render main app [:todos] chans)
                       (om/render footer app []
                         {:active active :completed completed :chans chans})]))))))
      js/document.body)))
