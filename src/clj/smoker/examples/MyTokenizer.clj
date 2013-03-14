;;
;; create temporary function my_tokenizer as 'smoker.examples.MyTokenizer';
;; select my_tokenizer(rc) AS (tok, freq) from str_rows;
;;
(ns smoker.examples.MyTokenizer
  (:require [smoker.udtf.gen :as gen]
            [clojure.string :as s]
            [clojure.tools.logging :as log])
  (:import [org.apache.hadoop.hive.serde2.objectinspector.primitive
            PrimitiveObjectInspectorFactory]))

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
  (log/info "input line tuple: " line)
  (map
   (fn [token] [token (Integer/valueOf 1)])
   (s/split (first line) #"\s+")))
