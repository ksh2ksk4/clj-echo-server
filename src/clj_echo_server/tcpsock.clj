(ns clj-echo-server.tcpsock
  (:require [clojure.core.server :refer [start-server stop-server]]
            [clojure.string :refer [split]])
  (:import (java.net StandardProtocolFamily UnixDomainSocketAddress)
           (java.nio ByteBuffer)
           (java.nio.channels ServerSocketChannel SocketChannel)
           (java.nio.charset Charset StandardCharsets)
           (java.nio.file Files Path))
  (:gen-class))

(defonce ^:private socket-file "/tmp/.echo-server-socket")
;(defonce ^:private address (. UnixDomainSocketAddress of (. Path of socket-file)))

;(def ^:private echo-server (ref nil))


;;; https://docs.oracle.com/javase/jp/18/docs/api/java.base/java/nio/channels/ServerSocketChannel.html
;;; https://docs.oracle.com/javase/jp/18/docs/api/java.base/java/nio/channels/SocketChannel.html

;; clojureで書けるところはclojureで
(defn- start-server
  "echoサーバを起動する"
  [socket-file]
  (let [socket-address (UnixDomainSocketAddress/of socket-file)
        server-socket-channel (ServerSocketChannel/open StandardProtocolFamily/UNIX)]
    ;;todo socketふぁいるが残っていたら消す
    ;; socketファイルが残っているとエラーになる
    ;; これを実行するとREPLが無応答になる
    (.bind server-socket-channel socket-address)
    (let [buffer (ByteBuffer/allocate 1024)
          socket-channel (.accept server-socket-channel)]
      (.read socket-channel buffer)
      (.flip buffer)
      (let [foo (.decode StandardCharsets/UTF_8 buffer)
            input (.toString foo)]
        (println "input: " input)
        (println "output: " input)
        (.write socket-channel (ByteBuffer/wrap (.getBytes input StandardCharsets/UTF_8)))))))

(comment
  (defn- echo
    [in out]
    (let [caption (str "*echo(" (.getId (Thread/currentThread)) ") ")]
      (println (str caption "start"))
      (let [buf (make-array Byte/TYPE 256)]
        (loop []
          (let [size (.read in buf)]
            (when (not= size -1)
              (.write out buf 0 size)
              (println (str caption "loop"))
              (recur)))))

      (println (str caption "end"))))

  (defn- start-echo-server
    []
    (dosync (ref-set echo-server (start-server {:address "localhost"
                                                :port 3333
                                                :name "echo-server"
                                                :accept `echo}))))

  (defn- stop-echo-server
    []
    (stop-server @echo-server)
    (dosync (ref-set echo-server nil))))

(comment
  $ brew install socat

  そもそも、clojureにUnix Domain Socketのサポートはない?

  $ echo -en 'GET / HTTP/1.0\r\n\r\n' | socat stdio /tmp/.echo-server-socket
)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (start-server socket-file)
  )
