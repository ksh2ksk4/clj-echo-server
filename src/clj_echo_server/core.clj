(ns clj-echo-server.core
  (:require [clojure.java.io :refer [delete-file]])
  (:import (java.net StandardProtocolFamily UnixDomainSocketAddress)
           (java.nio ByteBuffer)
           (java.nio.channels ServerSocketChannel)
           (java.nio.charset StandardCharsets))
  (:gen-class))

(defonce ^:private socket-file "/tmp/.echo-server-socket")

;;; https://docs.oracle.com/javase/jp/18/docs/api/java.base/java/net/UnixDomainSocketAddress.html
;;; https://docs.oracle.com/javase/jp/18/docs/api/java.base/java/nio/channels/ServerSocketChannel.html
;;; https://docs.oracle.com/javase/jp/18/docs/api/java.base/java/nio/channels/SocketChannel.html

(defn- start-server
  "echoサーバを起動する"
  [socket-file]
  (let [socket-address (UnixDomainSocketAddress/of socket-file)
        server-socket-channel (ServerSocketChannel/open StandardProtocolFamily/UNIX)]
    ;; ソケットファイルが残っていたら削除する
    (delete-file socket-file true)
    (.bind server-socket-channel socket-address)
    (let [buffer (ByteBuffer/allocate 1024)
          socket-channel (.accept server-socket-channel)]
      (.read socket-channel buffer)
      (.flip buffer)
      (let [input (.toString (.decode StandardCharsets/UTF_8 buffer))]
        (println "input: " input)
        (.write socket-channel (ByteBuffer/wrap (.getBytes input StandardCharsets/UTF_8)))))))

(defn -main
  [& args]
  (start-server socket-file))
