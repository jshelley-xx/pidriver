(defproject pidriver "0.1.0-SNAPSHOT"
  :description "TODO"
  :url "TODO"
  :license {:name "TODO: Choose a license"
            :url "http://choosealicense.com/"}
  :dependencies [
                [org.clojure/clojure "1.7.0"]
                [org.clojure/tools.namespace "0.2.4"]
                [com.stuartsierra/component "0.2.1"]
                [clj-http "2.0.0"]
                [cheshire "5.5.0"]
                [clj-time "0.11.0"]
                [com.cemerick/url "0.1.1"]
                [org.apache.logging.log4j/log4j-api "2.5"]
                [org.apache.logging.log4j/log4j-core "2.5"]
                [seesaw "1.4.6-SNAPSHOT"]
                [org.clojure/core.async "0.2.374"]
                 [com.github.insubstantial/substance "7.1"]
                 ]



  :profiles {:dev {
                   :source-paths ["dev"]}})
