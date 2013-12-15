(ns todomvc.app
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [put! >! <! chan]]
            [om.core :as om]
            [om.dom :as dom :include-macros true]
            [todomvc.utils :refer [pluralize now guid store]]
            [todomvc.item :as item]))

(enable-console-print!)

(def ENTER_KEY 13)

(def app-state (atom {:todos []}))

;; =============================================================================
;; Todos

(defn toggle-all [e todos]
  (let [checked (.. e -target -checked)]
    (om/replace! todos
      (into [] (map #(assoc % :completed checked) todos)))))

(defn handle-new-todo-keydown [e app owner]
  (when (identical? (.-which e) ENTER_KEY)
    (let [new-field (dom/get-node owner "newField")]
      (om/update! app [:todos] conj
        {:id (guid)
         :title (.-value new-field)
         :completed false})
      (set! (.-value new-field) ""))
    false))

(defn main [todos chans]
  (dom/component
    (dom/section #js {:id "main"}
      (dom/input #js {:id "toggle-all"
                      :type "checkbox"
                      :onChange #(toggle-all % todos)})
      (dom/ul #js {:id "todo-list"}
        (into-array
          (map #(om/render item/todo-item todos [%] chans :id)
            (range (count todos))))))))

(defn footer [{:keys [todos] :as app} opts]
  (let [{:keys [active completed showing chans]} opts
        clear-button (when (pos? completed)
                       (dom/button
                         #js {:id "clear-completed"
                              :onClick #(put! (:clear chans) (now))}
                         (str "Clear completed " completed)))
        selected (zipmap [:all :active :completed] (repeat "select"))]
    (dom/component
      (dom/footer #js {:id "footer"}
        (dom/span #js {:id "todo-count"}
          (dom/strong nil (count todos))
          (str " " (pluralize active "item") " left"))
        (dom/ul #js {:id "filters"}
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

(defn toggle-todo [todo]
  (om/replace! todo (update-in todo [:completed] #(not %))))

(defn destroy-todo [app {:keys [id]}]
  (om/replace! app [:todos]
    (into [] (filter #(= (:id %) id) (:todos app)))))

(defn edit-todo [app todo]
  (om/replace! todo [:editing] (:id todo)))

(defn save-todo [todo text]
  (om/replace! todo (update-in todo [:title] text)))

(defn cancel-action [app]
  (om/replace! app [:editing] nil))

(defn clear-completed [app]
  (om/replace! app [:todos]
    (into [] (remove :completed (:todos app)))))

(defn todo-app [{:keys [todos] :as app}]
  (reify
    dom/IWillMount
    (-will-mount [_ owner]
      (let [[toggle destroy edit save clear cancel :as cs]
             (take 6 (repeatedly chan))]
        (dom/set-state! owner :chans
          (zipmap [:toggle :destroy :edit :save :clear :cancel] cs))
        (go
          (while true
            (alt!
              toggle ([todo] (toggle-todo todo))
              destroy ([todo] (destroy-todo app todo))
              edit ([todo] (edit-todo app todo))
              save ([[todo text]] (save-todo todo text))
              clear ([v] (clear-completed app))
              cancel ([v] (cancel-action app)))))))
    dom/IDidUpdate
    (-did-update [_ _ _ _ _]
      (store "todos" app))
    dom/IRender
    (-render [_ owner]
      (let [active (reduce
                     #(if (:completed %2) (inc %1) %1)
                     0 todos)
            completed (- (count todos) active)
            chans (dom/get-state owner :chans)]
        (dom/div nil
          (dom/header #js {:id "header"}
            #js [(dom/h1 nil "todos")
                  (dom/input
                    #js {:ref "newField"
                         :id "new-todo"
                         :placeholder "What needs to be done?"
                         :onKeyDown #(handle-new-todo-keydown % app owner)})
                  (om/render main app [:todos] chans)
                  (om/render footer app []
                    {:active active :completed completed :chans chans})]))))))

(om/root app-state todo-app (.getElementById js/document "todoapp"))

(dom/render
  (dom/div nil
    (dom/p nil "Double-click to edit a todo")
    (dom/p nil
      (dom/a #js {:href "http://github.com/swannodette"}))
    (dom/p nil
      #js ["Part of"
           (dom/a #js {:href "http://todomvc.com"} "TodoMVC")]))
  (.getElementById js/document "info"))

