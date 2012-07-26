#!/bin/bash -eu

java -cp classes:agraph.jar\
:lib/commons-cli-1.2.jar\
:lib/commons-pool-1.5.6.jar\
:lib/sesame/commons-codec-1.3.jar\
:lib/sesame/commons-httpclient-3.1.jar\
:lib/sesame/openrdf-sesame-2.6.8-onejar.jar\
:lib/logging/*\
:lib/json.jar\
:clojure/libs/clojure-1.4.0.jar\
 test.stress.Events $*
