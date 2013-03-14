
(ns smoker.utils
  (:require
   [clojure.string :as s])
  (:import [java.net URI URL]))

(defmacro nil-if-exception [& body]
  `(try (do ~@body) (catch Exception e# nil)))

(defn re-sub-all-but-last [regex replacement string]
  (let [count (count (doall (re-seq regex string)))]
    (loop [str string i (- count 1)]
      (if (<= i 0)
        str
        (recur (s/replace-first str regex replacement) (dec i))))))

(defn truncate [s c]
  (if (and s (> (.length s) c))
    (.substring s 0 c)
    s))

