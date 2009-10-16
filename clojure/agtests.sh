#!/bin/bash
# set -x

source $(readlink -f $(dirname "$0"))/agenv.sh

# the tutorials expect to be in the root dir
cd $AGRAPH_JAVA

java -server $AGRAPH_JVM_ARGS clojure.main -e "(require 'com.franz.agraph.agtest) (com.franz.agraph.agtest/agraph-tests)"
