#!/bin/bash
# set -x

source $(readlink -f $(dirname "$0"))/agenv.sh

# the tutorials expect to be in the root dir
cd $AGRAPH_JAVA

if [ -z "$1" ]; then
    # export CLASSPATH=$CLASSPATH:$CLOJURE_PROJECTS/jline-0.9.94/jline-0.9.94.jar
    # java jline.ConsoleRunner clojure.main
    echo classpath $CLASSPATH
    java -server $AGRAPH_JVM_ARGS clojure.main
else
    # set -x
    java -server $AGRAPH_JVM_ARGS clojure.main $*
fi
