(ns todomvc.app
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :refer [put! >! <! chan]]
            [om.core :as om]
            [om.dom :as dom :include-macros true]
            [todomvc.utils :refer [pluralize now guid store]]
            [todomvc.item :as item]))

(enable-console-print!)

(def ENTER_KEY 13)

(def app-state (atom {:showing :all :todos []}))

;; =============================================================================
;; Main and Footer Components

(declare toggle-all)

(defn main [todos opts]
  (dom/component
    (dom/section #js {:id "main"}
      (dom/input #js {:id "toggle-all" :type "checkbox"
                      :onChange #(toggle-all % todos)})
      (dom/ul #js {:id "todo-list"}
        (into-array
          (map #(om/render item/todo-item todos
                  {:path [%] :opts opts :key :id
                   :fn (fn [todo]
                         (if (= (:id todo) (:editing opts))
                           (assoc todo :editing true)
                           todo))})
            (range (count todos))))))))

(defn footer [{:keys [showing todos] :as app} opts]
  (let [{:keys [count completed comm]} opts
        clear-button (when (pos? completed)
                       (dom/button
                         #js {:id "clear-completed"
                              :onClick #(put! comm [:clear (now)])}
                         (str "Clear completed " completed)))
        sel (-> (zipmap [:all :active :completed] (repeat ""))
                (assoc showing "selected"))]
    (dom/component
      (dom/footer #js {:id "footer"}
        (dom/span #js {:id "todo-count"}
          (dom/strong nil count)
          (str " " (pluralize count "item") " left"))
        (dom/ul #js {:id "filters"}
          (dom/li nil
            (dom/a #js {:href "#/" :className (sel :all)} "All"))
          (dom/li nil
            (dom/a #js {:href "#/active" :className (sel :active)} "Active"))
          (dom/li nil
            (dom/a #js {:href "#/completed" :className (sel :completed)} "Completed")))
        clear-button))))

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
        {:id (guid) :title (.-value new-field) :completed false})
      (set! (.-value new-field) ""))
    false))

(defn toggle-todo [todo]
  (om/replace! todo (update-in todo [:completed] #(not %))))

(defn destroy-todo [app {:keys [id]}]
  (om/replace! app [:todos]
    (into [] (filter #(= (:id %) id) (:todos app)))))

(defn edit-todo [app todo]
  (om/replace! app [:editing] (:id todo)))

(defn save-todo [todo text]
  (om/replace! todo (update-in todo [:title] text)))

(defn cancel-action [app]
  (om/replace! app [:editing] nil))

(defn clear-completed [app]
  (om/replace! app [:todos] (into [] (remove :completed (:todos app)))))

(defn handle-event [app [type val :as e]]
  (case type
    :toggle  (toggle-todo val)
    :destroy (destroy-todo app val)
    :edit    (edit-todo app val)
    :save    (let [[todo text] val]
               (save-todo todo text))
    :clear   (clear-completed app)
    :cancel  (cancel-action app)
    nil))

(defn todo-app [{:keys [todos] :as app}]
  (reify
    dom/IWillMount
    (-will-mount [_ owner]
      (let [comm (chan)]
        (dom/set-state! owner [:comm] comm)
        (go (while true
              (handle-event app (<! comm))))))
    dom/IDidUpdate
    (-did-update [_ _ _ _ _]
      (store "todos" app))
    dom/IRender
    (-render [_ owner]
      (let [active    (count (remove :completed todos))
            completed (- (count todos) active)
            comm      (dom/get-state owner [:comm])]
        (dom/div nil
          (dom/header #js {:id "header"}
            (dom/h1 nil "todos")
            (dom/input
              #js {:ref "newField" :id "new-todo"
                   :placeholder "What needs to be done?"
                   :onKeyDown #(handle-new-todo-keydown % app owner)})
            (om/render main app
              {:path [:todos] :opts {:comm comm :editing (:editing app)}})
            (om/render footer app
              {:path [] :opts {:count active :completed completed :comm comm}})))))))

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
