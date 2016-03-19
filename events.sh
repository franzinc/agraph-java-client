#!/bin/bash -eu

# Report Java version first
java -version

java -cp classes:agraph.jar\
:lib/commons-cli-1.2.jar\
:lib/commons-pool-1.5.6.jar\
:lib/sesame/commons-codec-1.3.jar\
:lib/sesame/commons-httpclient-3.1.jar\
:lib/sesame/openrdf-sesame-2.7.11-onejar.jar\
:lib/logging/*\
:lib/json.jar\
:clojure/libs/clojure-1.4.0.jar\
 test.stress.Events $*
