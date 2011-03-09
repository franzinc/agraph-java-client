#!/bin/bash -eu

java -cp classes:agraph.jar\
:lib/commons-cli-1.2.jar\
:lib/commons-pool-1.5.5.jar\
:lib/sesame-2.3.2/*\
:lib/json.jar\
 test.stress.Events $*
