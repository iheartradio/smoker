(ns smoker.examples.MyTokenizer
  (:require [smoker.udtf.gen :as gen])
  (:import [org.apache.hadoop.hive.serde2.objectinspector.primitive
            PrimitiveObjectInspectorFactory])
  (:require [clojure.contrib.str-utils2 :as su]))

(gen/gen-udtf)
(gen/def-udtf-schema
  ;; Field names for output fields
  :output-fields ["token" "frequency"]
  ;; Classes are Primitives
  :output-classes [String Integer]
  ;; Input classes (ToDo: Add validation here)
  :input-classes [String])

;; Take in a line split it and return a tuple of [token frequency]
(defn -operate [this line]
  (map
   (fn [token] [token (Integer/valueOf 1)])
   (su/split (first line) #"\s+")))
