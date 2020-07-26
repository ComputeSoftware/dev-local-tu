(ns dev-local-tu.internal.impl
  (:require [clojure.java.io :as io])
  (:import (java.nio.file Files SimpleFileVisitor FileVisitResult)
           (java.security SecureRandom)
           (java.util Base64)))

(defn delete-directory!
  [f]
  (Files/walkFileTree
    (.toPath (io/file f))
    (proxy [SimpleFileVisitor] []
      (visitFile [path attrs]
        (Files/delete path)
        FileVisitResult/CONTINUE)
      (postVisitDirectory [path io-ex]
        (Files/delete path)
        FileVisitResult/CONTINUE))))

(let [random (SecureRandom.)
      encoder (.withoutPadding (Base64/getUrlEncoder))]
  (defn rand-str!
    [length]
    (let [buffer (byte-array length)
          _ (.nextBytes random buffer)]
      (.encodeToString encoder buffer))))