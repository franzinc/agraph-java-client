#!/bin/bash -eu

java -cp classes:agraph.jar\
:lib/commons-cli-1.2.jar\
:lib/sesame-2.3.1/*\
:lib/json.jar\
 test.stress.Events $*
