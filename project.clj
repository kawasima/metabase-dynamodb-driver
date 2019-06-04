(defproject metabase/dynamodb-driver "0.1.0-SNAPSHOT"
  :min-lein-version "2.5.0"
  :dependencies [[com.amazonaws/aws-java-sdk-dynamodb "1.11.563"]]

  :jvm-opts
  ["-XX:+IgnoreUnrecognizedVMOptions"]
  :prep-tasks ["javac" "compile"]

  :profiles
  {:provided
   {:dependencies
    [[org.clojure/clojure "1.10.0"]
     [metabase-core "1.0.0-SNAPSHOT"]]}

   :dev [:project/dev :profiles/dev]
   :profiles/dev {:main ^:skip-aot metabase.driver.dynamodb

                  }
   :project/dev {:dependencies
                 [[eftest "0.5.7"]]
                 :source-paths ["dev/src"]
                 :resource-paths ["dev/resources"]}
   :uberjar
   {:auto-clean    true
    :aot           :all
    :omit-source   true
    :javac-options ["-target" "1.8"]
    :target-path   "target/%s"
    :uberjar-name  "dynamodb.metabase-driver.jar"}})
