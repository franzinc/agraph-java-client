#!/bin/bash -eu

java -cp classes:agraph.jar\
:lib/commons-cli-1.2.jar\
:lib/sesame-2.3.2/*\
:lib/logging/*\
:lib/json.jar\
 test.stress.Events $*
