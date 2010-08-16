#!/bin/bash

java -cp classes:lib/commons-cli-1.2.jar\
:lib/sesame-2.3.1/*\
:lib/json.jar\
 test.stress.Events $*
