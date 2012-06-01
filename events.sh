#!/bin/bash -eu

java -cp classes:agraph.jar\
:lib/commons-cli-1.2.jar\
:lib/commons-pool-1.5.6.jar\
:lib/sesame-2.6.5/commons-codec-1.3.jar\
:lib/sesame-2.6.5/commons-httpclient-3.1.jar\
:lib/sesame-2.6.5/openrdf-sesame-2.6.5-onejar.jar\
:lib/logging/*\
:lib/json.jar\
:clojure/libs/clojure-1.4.0.jar\
 test.stress.Events $*
