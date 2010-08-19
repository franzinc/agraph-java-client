#!/bin/bash

java -cp classes:agraph.jar\
:lib/sesame-2.3.1/*\
:lib/json.jar\
 test.stress.Events $*
