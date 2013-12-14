(ns todomvc.app
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [put! >! <! chan]]
            [om.core :as om]
            [om.dom :as dom :include-macros true]
            [todomvc.utils :refer [pluralize now guid]]
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
      {:id (guid)
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
          (map #(om/render todos [%] chans :id)
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

(defn toggle [todo]
  (om/replace! todo (update-in todo [:completed] #(not %))))

(defn destroy [app {:keys [id]}]
  (om/replace! app [:todos]
    (into [] (filter #(= (:id %) id) (:todos app)))))

(defn edit [app todo]
  (om/replace! todo [:editing] (:id todo)))

(defn save [todo text]
  (om/replace! todo (update-in todo [:title] text)))

(defn cancel [app]
  (om/replace! app [:editing] nil))

(defn clear [app]
  (om/replace! app [:todos]
    (into [] (remove :completed (:todos app)))))

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
                  toggle ([todo] (toggle todo))
                  destroy ([todo] (destroy app todo))
                  edit ([todo] (edit app todo))
                  save ([[todo text]] (save todo text))
                  clear ([v] (clear app))
                  cancel ([v] (cancel app))))))
          dom/IRender
          (-render [_ owner]
            (let [active (reduce
                           #(if (:completed %2) (inc %1) %1)
                           0 todos)
                  completed (- (count todos) active)]
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
      js/document.body)))

(todo-app)
