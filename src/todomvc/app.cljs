(ns todomvc.app
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [secretary.macros :refer [defroute]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! <! chan]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary]
            [todomvc.utils :refer [pluralize now guid store]]
            [clojure.string :as string]
            [todomvc.item :as item])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

(def ENTER_KEY 13)

(def app-state (atom {:showing :all :todos []}))

;; =============================================================================
;; Routing

(defroute "/:filter" []
  (swap! app-state assoc :showing (keyword filter)))

(def history (History.))

(events/listen history EventType/NAVIGATE
  (fn [e] (secretary/dispatch! (.-token e))))

(.setEnabled history true)

;; =============================================================================
;; Main and Footer Components

(declare toggle-all)

(defn visible? [todo filter]
  (case filter
    :all true
    :active (not (:completed todo))
    :completed (:completed todo)))

(defn main [{:keys [showing todos] :as app} opts]
  (om/component
    (dom/section #js {:id "main"
                      :style (if (empty? todos)
                               #js {:display "none"}
                               #js {})}
      (dom/input #js {:id "toggle-all" :type "checkbox"
                      :onChange #(toggle-all % app)
                      :checked (every? #(:completed %) todos)})
      (dom/ul #js {:id "todo-list"}
        (into-array
          (map #(om/build item/todo-item app
                  {:path [:todos %] :opts opts :key :id
                   :fn (fn [todo]
                         (cond-> todo
                           (= (:id todo) (:editing opts))
                           (assoc :editing true)
                           (not (visible? todo showing))
                           (assoc :hidden true)))})
            (range (count (:todos app)))))))))

(defn footer [{:keys [showing todos]} opts]
  (let [{:keys [count completed comm]} opts
        clear-button (when (pos? completed)
                       (dom/button
                         #js {:id "clear-completed"
                              :onClick #(put! comm [:clear (now)])}
                         (str "Clear completed (" completed ")")))
        sel (-> (zipmap [:all :active :completed] (repeat ""))
                (assoc showing "selected"))]
    (om/component
      (dom/footer #js {:id "footer"
                       :style (if (empty? todos)
                                #js {:display "none"}
                                #js {})}
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

(defn toggle-all [e app]
  (let [checked (.. e -target -checked)]
    (om/update! app [:todos]
      (fn [todos] (into [] (map #(assoc % :completed checked) todos))))))

(defn handle-new-todo-keydown [e app owner]
  (when (identical? (.-which e) ENTER_KEY)
    (let [new-field (om/get-node owner "newField")]
      (when-not (string/blank? (.. new-field -value trim))
        (om/update! app [:todos] conj
          {:id (guid) :title (.-value new-field) :completed false})
        (set! (.-value new-field) "")))
    false))

(defn destroy-todo [app {:keys [id]}]
  (om/update! app [:todos]
    (fn [todos] (into [] (remove #(= (:id %) id) todos)))))

(defn edit-todo [app todo]
  (om/update! app #(assoc % :editing (:id todo))))

(defn save-todos [app]
  (om/update! app #(dissoc % :editing)))

(defn cancel-action [app]
  (om/update! app #(dissoc % :editing)))

(defn clear-completed [app]
  (om/update! app [:todos]
    (fn [todos] (into [] (remove :completed todos)))))

(defn handle-event [app [type todo :as e]]
  (case type
    :destroy (destroy-todo app todo)
    :edit    (edit-todo app todo)
    :save    (save-todos app)
    :clear   (clear-completed app)
    :cancel  (cancel-action app)
    nil))

(def render-start nil)

(defn todo-app [{:keys [todos] :as app}]
  (reify
    om/IWillMount
    (will-mount [_ owner]
      ;; TODO: solve the problem of app not being
      ;; "up-to-date" here - David
      (let [comm (chan)]
        (om/set-state! owner [:comm] comm)
        (go (while true
              (handle-event app (<! comm))))))
    om/IWillUpdate
    (will-update [_ _ _ _]
      (set! render-start (now)))
    om/IDidUpdate
    (did-update [_ _ _ _ _]
      (store "todos" todos)
      (let [ms (- (.valueOf (now)) (.valueOf render-start))]
        (set! (.-innerHTML (js/document.getElementById "message")) (str ms "ms"))))
    om/IRender
    (render [_ owner]
      (let [active    (count (remove :completed todos))
            completed (- (count todos) active)
            comm      (om/get-state owner [:comm])]
        (dom/div nil
          (dom/header #js {:id "header"}
            (dom/h1 nil "todos")
            (dom/input
              #js {:ref "newField" :id "new-todo"
                   :placeholder "What needs to be done?"
                   :onKeyDown #(handle-new-todo-keydown % app owner)})
            (om/build main app
              {:path [] :opts {:comm comm :editing (:editing app)}})
            (om/build footer app
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

;; =============================================================================
;; Benchmark Stuff

(aset js/window "benchmark1"
  (fn [e]
    (dotimes [_ 200]
      (swap! app-state update-in [:todos] conj
        {:id (guid) :title "foo" :completed false}))))

(aset js/window "benchmark2"
  (fn [e]
    (dotimes [_ 200]
      (swap! app-state update-in [:todos] conj
        {:id (guid) :title "foo" :completed false}))
    (dotimes [_ 5]
      (swap! app-state update-in [:todos]
        (fn [todos]
          (map #(assoc-in % [:completed] not) todos))))
    (swap! app-state update-in [:todos]
      (fn [todos]
        (into [] (remove :completed todos))))))
