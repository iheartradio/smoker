
(ns smoker.udtf.gen
  (:import
   [smoker.udf ClojureUDTF]
   [java.util ArrayList List]
   [org.apache.hadoop.hive.ql.exec UDFArgumentException]
   [org.apache.hadoop.hive.ql.metadata HiveException]
   [org.apache.hadoop.hive.ql.udf.generic GenericUDTF]
   [org.apache.hadoop.hive.serde2.objectinspector
            ObjectInspector ObjectInspectorFactory PrimitiveObjectInspector
            StructObjectInspector]
   [org.apache.hadoop.hive.serde2.objectinspector.primitive
              PrimitiveObjectInspectorFactory]
   [org.apache.hadoop.hive.ql.exec UDF]
   [org.apache.hadoop.io Text]
   [java.util Date]))

(defmacro gen-udtf
  "Creates a UDTF that takes exactly one argument and can return 0 or more tuples.
You also need to call gen-wrapper-methods."
  []
  (let [the-name (.replace (str (ns-name *ns*)) \- \_)]
    `(do
       (gen-class
        :name ~the-name
        :extends smoker.udf.ClojureUDTF
        :init "init"
        :constructors {[] []}
        :methods [["operate" [Object] clojure.lang.ISeq]]
        :state "state"))))

(defn -init []
  [[] (atom [])])

(defn build-initialize [emits]
  (fn [this args]
    ;; todo: take the input-fields and perform arg-type validation here
    ;; todo: remove this and allow user to set the state via an init fn
    ;;       like a stateful defmapcatop
    (swap! (.state this) (fn [_] args))
    (let [fieldNames (ArrayList. (map str (range 0 (count emits))))
          fieldIOs (ArrayList. emits)]
      (.setArgs this (into-array ObjectInspector args))
      (ObjectInspectorFactory/getStandardStructObjectInspector
       fieldNames fieldIOs))))

(defn -close [this])

(defn -process [this record]
  (let [primitives
        (map
         (fn [[i thingy]]
           (.getPrimitiveJavaObject thingy (nth record i)))
         (map-indexed vector @(.state this)))]
     (doall
      (let [result (.operate this primitives)]
        (map
          (fn [results] (.emit this (into-array Object results)))
          result)))))


(defn gen-wrapper-methods
  "Generates the methods needed to use your UDTF.

For now, use the PrimitiveObjectInspectorFactory to specify the types you'd plan on returning (syntax sugar to come eventually). Example:

(gen/gen-wrapper-methods
 [PrimitiveObjectInspectorFactory/javaStringObjectInspector
  PrimitiveObjectInspectorFactory/javaIntObjectInspector])

Will allow you to return a tuple of (String, int)

Now you need to write an -operate method that accepts [this line] and returns a seq of tuples that match your types. In the above case, we could return:

    [[\"hi\" 1] [\"bye\" 2]]

"
  [emits]
  (intern *ns* '-init -init)
  (intern *ns* '-initialize (build-initialize emits))
  (intern *ns* '-close -close)
  (intern *ns* '-process -process))

(defn- mk-primitives
  "Convert output-classes to HivePrimitive inspectors"
  [output-classes]
  (map #(PrimitiveObjectInspectorFactory/getPrimitiveObjectInspectorFromClass %) output-classes))

;; todo: move this out and make a single one for udf/udtf etc...
(defn def-udtf-schema
  "Define schema
   Keyword argument:
  `:output-fields` - Represents the output output fields or columns names`.
  `:output-classes` - Represents the output classes for the columns.
  `:input-classes` - Represents the input classes to the UDTF currently
                     not used for validating input."
  [& {:keys [output-fields output-classes input-classes]}]
  (let [[output-fields output-classes]
        (cond
          (and (coll? output-fields) (coll? output-classes))
            (do
              (when-not (= (count output-fields) (count output-classes))
                (throw (IllegalArgumentException. "mismatched item counts for output-fields & output-classes")))
              [output-fields output-classes])
          (and (coll? output-fields) (nil? output-classes))
              [output-fields (map (fn [_] String) (range (count output-fields)))]
          (and (coll? output-classes) (nil? output-fields))
              [(map str (range (count output-classes))) output-classes]
          :else
            (throw (IllegalArgumentException. "Invalid input. Valid input :output-fields [] :output-classes []")))
        input-primitives (mk-primitives output-classes)]
    (gen-wrapper-methods input-primitives)))
