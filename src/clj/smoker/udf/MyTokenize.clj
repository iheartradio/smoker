
(ns smoker.udf.MyTokenize
  (:require [smoker.udtf.gen :as gen])
  (:import [org.apache.hadoop.hive.serde2.objectinspector.primitive
            PrimitiveObjectInspectorFactory])
  (:require [clojure.string :as s]))

(gen/gen-udtf)
(gen/gen-wrapper-methods
 [PrimitiveObjectInspectorFactory/javaStringObjectInspector
  PrimitiveObjectInspectorFactory/javaIntObjectInspector])

(defn -operate [this line]
  (map
   (fn [token] [token (Integer/valueOf 1)])
   (s/split line #"\s+")))
