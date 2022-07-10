(ns clj-echo-server.core
  (:require [clojure.java.io :refer [delete-file]]
            [clojure.tools.cli :refer [parse-opts]])
  (:import (java.net StandardProtocolFamily UnixDomainSocketAddress)
           (java.nio ByteBuffer)
           (java.nio.channels ServerSocketChannel SocketChannel)
           (java.nio.charset StandardCharsets))
  (:gen-class))

;;; https://docs.oracle.com/javase/jp/18/docs/api/java.base/java/net/UnixDomainSocketAddress.html
;;; https://docs.oracle.com/javase/jp/18/docs/api/java.base/java/nio/channels/ServerSocketChannel.html
;;; https://docs.oracle.com/javase/jp/18/docs/api/java.base/java/nio/channels/SocketChannel.html

(defonce ^:private socket-file "/tmp/.echo-server-socket")
(defonce ^:private cl-options
  [["-p" "--port PORT" "Port number"
    :default 80
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc]
   ["-h" "--help"]])

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
        (println "Request: " input)
        (.write socket-channel (ByteBuffer/wrap (.getBytes input StandardCharsets/UTF_8)))))))

(defonce input "Hello")

(defn- start-client
  "クライアントを起動する"
  [socket-file]
  (let [socket-address (UnixDomainSocketAddress/of socket-file)
        client-socket-channel (SocketChannel/open StandardProtocolFamily/UNIX)]
    (.connect client-socket-channel socket-address)
    (.write client-socket-channel (ByteBuffer/wrap (.getBytes input StandardCharsets/UTF_8)))
    (let [buffer (ByteBuffer/allocate 1024)]
      (.read client-socket-channel buffer)
      (.flip buffer)
      (println "Response: " (.toString (.decode StandardCharsets/UTF_8 buffer))))))

(defn -main
  [& args]
  (let [options (parse-opts args cl-options)]
    (println options)
    (if (= (first (:arguments options)) "server")
      (start-server socket-file)
      (start-client socket-file))))
