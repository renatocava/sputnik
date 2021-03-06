; Copyright (c) Gunnar Völkel. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v1.0.txt at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns jppfnik-control.host-usage
  "Functionality to monitor cpu usage of remoter computers."
  (:require
    [clojure.string :as string]
    [jppfnik-control.remote :as remote])
  (:use
    [clojure.options :only [defn+opts]]
    [clojure.stacktrace :only [print-cause-trace]]))


(defn split-line
  [line]
  (remove empty? (string/split line #" ")))


(defn parse-line
  "Parses the data contained in one line of the 'ps' output: cpu-usage, user, state, command, full-command"
  [line]
  (let [[cpu-usage, user, state, command, & full-command] (split-line line)]
    {:cpu-usage (try (if (nil? cpu-usage) Double/NaN (Double/parseDouble cpu-usage)) (catch Throwable t (println "ERROR: line =" line) (throw t))), 
     :user user, 
     :state 
	     (case state
	       "R" :running,
	       "S" :sleeping,
	       :other),
     :command command, 
     :full-command (string/join " " full-command)}))


(defn parse-usage-data
  "Parses the string output of `ps`. Discards all lines until the headline and removes the last line."
  [usage-output]
  (->> (string/split usage-output #"\n")
    ; discard lines before headline
    (drop-while #(not= (split-line %) ["%CPU" "RUSER" "S" "COMMAND" "COMMAND"]))
    ; skip headline
    rest
    ; remove last 2 lines ("...done" "logout")
    (drop-last 2)
    ; parse each line
    (map parse-line)    
    vec))


(defn+opts get-host-usage-data
  "Queries the cpu usage of the given hosts using the given user.
  Returns a map consisting of the active processes and their data per host.
  <cols>Specifies the --cols parameter for `ps` which determines the maximum line width.</cols>"
  [host-coll, user | {cols 200} :as options]
  (into {}
    (for [[host {:keys [out]}] (remote/execute-script host-coll, user, (remote/script (ps "-eo pcpu,ruser,state,comm,args k -pcpu H --cols" ~cols)), options)]
      [host (parse-usage-data out)])))


(defn+opts host-usage
  "Queries the cpu usage from the given host using the given user and filters the processes by state and cpu usage threshold.
  <cpu-threshold>Threshold to filter the processes that have a cpu usage greater or equal to this value.</cpu-threshold>
  <states>States used to filter the processes that are in one of these states.</states>"  
  [host-coll, user | {cpu-threshold 10.0, states [:running]} :as options]
  (let [state-filter (set states)]
	  (->> (get-host-usage-data host-coll, user, options)
	    (reduce-kv
	      (fn [m, host, processes] 
	        (assoc m host 
	          (->> processes (filter #(and (state-filter (:state %)) (<= cpu-threshold (:cpu-usage %)))) (sort-by :cpu-usage >))))
	      {}))))


(defn print-host-usage-summary
  "Prints a summary of the cpu usage per host from the given usage data map."
  [host-usage-data]
  (let [host-len (->> host-usage-data keys (map (comp count name)) (reduce max))]
	  (doseq [[host usage-data] (sort-by key host-usage-data)]
	    (println
	      (format (str "%-" host-len "s %7.2f%% (%2d) %s") 
          (name host) 
          (reduce + 0.0 (map :cpu-usage usage-data)) 
          (count usage-data)
          (->> usage-data (map :user) distinct sort (string/join ", ")))))))


(defn user+command-len-vec
  "Determine lengths of username and command."
  [m]
  [(-> m :user count) (-> m :command count)])


(defn vector-max
  "Determine the maximum values of each position in the given vectors."
  ([] nil)
  ([v] v)
  ([v w] (map max v w)))


(defn print-host-usage-details
  "Prints a detailed report of the cpu usage per host from the given usage data map.
  The data for each process per host is displayed."
  [host-usage-data]
  (let [[user-len command-len] (->> host-usage-data (mapcat val) (map user+command-len-vec) (reduce vector-max))
        fmt (str "%7.2f%%   %-" user-len "s   %-" command-len "s   %s")]
	  (doseq [[host usage-data] (sort-by key host-usage-data)]
      (if (seq usage-data)
        (do
			    (println (name host))
			    (println (apply str (repeat (-> host name count) "-")))
			    (doseq [{:keys [cpu-usage, user, command, full-command]} usage-data]
			      (println (format fmt cpu-usage, user, command, full-command)))
			    (println "\n"))
        (println ">>>" (name host) "is idle")))))


(defn+opts host-usage-info
  "Queries the given collection of hosts for their cpu usage using the given user and
  print the results to standard out. This is done query-count times.
  <details>Specifies whether the running processes for each host are shown instead of a summary view.</details>"
  [host-coll, user, query-count | {details false} :as options]
  (let [print-fn (if details print-host-usage-details print-host-usage-summary)] 
    (dotimes [q query-count]
      (println "---------")
      (println "Query" (inc q))
      (println "---------")      
      (try
        (print-fn (host-usage host-coll, user, options))
        (catch Exception e
          (println "Query failed!")
          (print-cause-trace e)))
      (println ""))))