; Copyright (c) Gunnar Völkel. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v1.0.txt at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns jppfnik-tools.file-system
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string])
  (:use
    [clojure.options :only [defn+opts]])
  (:import
    java.io.File))


(defn exists?
  "Checks whether the given argument exists in the file system."
  [f]
  (if (instance? File f)
    (.exists ^File f)
    (-> f io/file .exists)))


(defn file?
  "Checks whether the given argument refers to a file in the file system."
  [f]
  (if (instance? File f)
    (.isFile ^File f)
    (-> f io/file .isFile)))


(defn directory?
  "Checks whether the given argument refers to a directory in the file system."
  [f]
  (if (instance? File f)
    (.isDirectory ^File f)
    (-> f io/file .isDirectory)))


(defn file-from-filesystem
  "Returns the file object to the given url if the file exists."
  ([url]
    (file-from-filesystem nil, url))
  ([prefix, url]
    (let [f (if prefix (io/file prefix, url) (io/file url))]
      (when (.exists f)
        f))))


(defn+opts find-file
  "Searches for the file with the given url first in the filesystem and second in the classpath.
  <possible-directory>Specifies a directory that might be the prefix to the url.
  If this parameter is given, the function will check first for \"possible-directory/url\".</>
  "
  [url | {possible-directory nil}]
  (or
    (when possible-directory
      (file-from-filesystem possible-directory, url))
    (file-from-filesystem url)
    (when possible-directory
      (io/resource (io/as-relative-path (io/file possible-directory url))))
    (io/resource url)))


(defn join-directory-urls
  [path1, path2]
  (if (and path1 path2)
    (.getPath (io/file path1, path2))
    (when-let [p (or path1 path2)]
      (.getPath (io/file p)))))


(defn filename
  "Returns the filename of the given url."
  [url]
  (-> url io/file .getName))


(defn filepath
  "Returns the path of the given url"
  [url]
  (-> url io/file .getAbsoluteFile .getParent))


(defn assure-directory-name
  "Returns the path of the given url with a trailing slash."
  [url]
  (let [filepath (-> url io/file .getPath (string/replace #"\\", "/"))]
    (str filepath "/")))


(defn create-directory
  "Creates the given directory if it does not exist already."
  [url]
  (let [f (io/file url)]
    (when-not (.exists f)
      (.mkdirs f))
    f))


(defn+opts delete-directory
  "Deletes a given directory."
  [f | {silently false} :as options]
  (let [f (io/file f)]
    (when (.exists f)
	    (when (.isDirectory f)
	      (doseq [child (.listFiles f)]
	        (delete-directory child, options)))
	    (io/delete-file f silently))))


(defn copy-file
  [source-file, target-directory]
  (io/copy (io/file source-file) (io/file target-directory (filename source-file))))


(defn file-exists
  [f]
  (let [f (io/file f)]
    (when (.exists f)
      f)))


(defn find-url
  "Tries to find the file for the given URL either in the filesystem or on the classpath."
  [url]
  (or (some-> url file-exists)
    (some-> url io/resource file-exists)))