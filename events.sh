#!/bin/bash -eu

java -cp classes:agraph.jar\
:lib/commons-cli-1.2.jar\
:lib/commons-pool-1.5.6.jar\
:lib/sesame-2.3.2/commons-codec-1.3.jar\
:lib/sesame-2.3.2/commons-httpclient-3.1.jar\
:lib/sesame-2.3.2/openrdf-sesame-2.3.2-onejar.jar\
:lib/logging/*\
:lib/json.jar\
 test.stress.Events $*
