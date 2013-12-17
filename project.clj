(defproject todomvc "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2122"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [secretary "0.4.0"]
                 [om "0.1.0"]]

  :plugins [[lein-cljsbuild "1.0.0"]]

  :source-paths ["src"]

  :cljsbuild { 
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                :output-to "app.js"
                :output-dir "out"
                :optimizations :none
                :source-map true
                :foreign-libs [{:file "om/react.js"
                                :provides ["React"]}]
                :externs ["om/externs/react.js"]}}
             {:id "release"
              :source-paths ["src"]
              :compiler {
                :output-to "app.js"
                :optimizations :advanced
                :pretty-print false
                :output-wrapper false
                :foreign-libs [{:file "om/react.js"
                                :provides ["React"]}]
                :externs ["om/externs/react.js"]
                :closure-warnings
                {:non-standard-jsdoc :off}}}]})
