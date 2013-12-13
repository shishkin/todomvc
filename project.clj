(defproject react-cljs "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2120"]
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
                :foreign-libs [{:file "reactjs/react.js"
                                :provides ["React"]}]
                :externs ["reactjs/externs/react.js"]}}
             {:id "release"
              :source-paths ["src"]
              :compiler {
                :output-to "app.js"
                :optimizations :advanced
                :pretty-print false
                :foreign-libs [{:file "reactjs/react.js"
                                :provides ["React"]}]
                :externs ["reactjs/externs/react.js"]
                :closure-warnings
                {:non-standard-jsdoc :off}}}]})
