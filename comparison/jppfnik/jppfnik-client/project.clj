(defproject jppfnik-client "0.0.1"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  ; the logging dependencies are all for JPPF
  ; JPPF's jars are deployed as resources and added to classpath at runtime
  :dependencies
  [[org.clojars.guv/jppf-common "3.1.2"]
   [org.clojars.guv/jppf-common-node "3.1.2"]
   [org.clojars.guv/jppf-client "3.1.2-1"]
   [org.clojars.guv/jmxremote_optional "1.0.1_04"]
   [log4j "1.2.16" 
     :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
   [org.slf4j/slf4j-api "1.6.1"]
   [org.slf4j/slf4j-log4j12 "1.6.1"]
   [jppfnik-tools "0.0.1"]
   [org.clojure/tools.cli "0.2.2"]
   [clojure.options "0.2.9"]]
  
  :profiles
  {:dev 
    {:dependencies 
      [[org.clojure/clojure "1.6.0"]
       [clj-debug "0.7.5"]]}
   :release
    {
    :hooks [leiningen.hooks.special-uberjar]
    ; exclude clojure jar since sputnik uberjar will be deployed with a configured
    :exclude-from-uberjar [#"clojure-.*\.jar"]}}
  
  ; needs to be aot'ed but if you don't want to depend on a specific clojure version, you have to compile it yourself
  :aot [jppfnik-client.task]
  
  :plugins [[lein-jppfnik "0.0.1"]]  
)
