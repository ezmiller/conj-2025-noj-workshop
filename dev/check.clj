(ns dev.check
  (:gen-class))

(defn -main [& _]
  (println "Environment check:")
  (println "- Java:" (System/getProperty "java.version"))
  (println "- Clojure:" (clojure-version))
  (println "- User.dir:" (System/getProperty "user.dir"))
  (println "Tip: to inspect dependency versions, run: clojure -T:deps tree"))
