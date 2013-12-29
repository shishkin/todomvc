(ns todomvc.item
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [>! put! timeout]]
            [todomvc.utils :refer [now]]
            [clojure.string :as string]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)

;; =============================================================================
;; Todo Item

(defn handle-submit [e todo {:keys [owner comm]}]
  (when-let [edit-text (om/get-state owner [:edit-text])]
    (if-not (string/blank? (.trim edit-text))
      (do
        (om/transact! todo #(assoc % :title edit-text))
        (put! comm [:save todo]))
      (put! comm [:destroy todo])))
  false)

(defn handle-edit [e todo {:keys [owner comm]}]
  ;; NOTE: we have to grab the node here? - David
  (let [node (om/get-node owner "editField")]
    (go
      (>! comm [:edit todo])
      ;; NOTE: Annoying that we have to do this - David
      (<! (timeout 100))
      (.focus node)
      (.setSelectionRange node 0 (.. node -value -length)))
    (om/set-state! owner [:edit-text] (om/read todo [:title]))))

(defn handle-key-down [e todo {:keys [owner] :as opts}]
  (let [kc (.-keyCode e)]
    (if (identical? kc ESCAPE_KEY)
      (do
        (om/set-state! owner [:edit-text] (om/read todo [:title]))
        (put! (:comm opts) [:cancel todo]))
      (if (identical? kc ENTER_KEY)
        (handle-submit e todo opts)))))

(defn handle-change [e todo owner]
  (om/set-state! owner [:edit-text] (.. e -target -value)))

(defn todo-item [{:keys [id title editing completed] :as todo} {:keys [comm]}]
  (reify
    om/IInitState
    (init-state [_ owner]
      {:edit-text title})
    om/IRender
    (render [_ owner]
      (let [m {:owner owner :comm comm}
            classes (cond-> []
                      completed (conj "completed")
                      editing   (conj "editing"))]
        (dom/li #js {:className (string/join " " classes)
                     :style (if (true? (:hidden todo))
                              #js {:display "none"}
                              #js {})}
          (dom/div #js {:className "view"}
            (dom/input #js {:className "toggle" :type "checkbox"
                            :checked (and completed "checked")
                            :onChange (fn [_] (om/transact! todo [:completed] #(not %)))})
            (dom/label #js {:onDoubleClick #(handle-edit % todo m)} (:title todo))
            (dom/button #js {:className "destroy"
                             :onClick (fn [_] (put! comm [:destroy todo]))}))
          (dom/input #js {:ref "editField" :className "edit"
                          :value (om/get-state owner [:edit-text])
                          :onBlur #(handle-submit % todo m)
                          :onChange #(handle-change % todo owner)
                          :onKeyDown #(handle-key-down % todo m)}))))))
