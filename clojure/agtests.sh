#!/bin/bash
# set -x

source ./agenv.sh

java -server $AGRAPH_JVM_ARGS clojure.main -e "(require 'com.franz.agraph.agtest) (com.franz.agraph.agtest/agraph-tests)"
