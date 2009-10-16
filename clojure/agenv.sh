#!/bin/bash
# set -x

# This script starts a Clojure REPL for command line use, or with Emacs/Slime/Swank-clojure.
#
# You may need to customize CLOJURE_PROJECTS and AGRAPH_JAVA below.

export AGRAPH_JAVA=$(readlink -f $(dirname "$0")/..)

export CLOJURE_PROJECTS=$(readlink -f "$AGRAPH_JAVA/../../..")

export AGRAPH_CLOJURE=$AGRAPH_JAVA/clojure

export CLASSPATH=$AGRAPH_CLOJURE/lib/clojure-1.0.jar
export CLASSPATH=$CLASSPATH:$AGRAPH_CLOJURE/lib/clojure-contrib-1.0.jar

export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/bin
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/classes
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/commons-codec-1.3.jar
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/commons-httpclient-3.1.jar
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/commons-logging-1.1.1.jar
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/openrdf-sesame-2.2.4-onejar.jar
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/slf4j-api-1.5.8.jar
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/slf4j-nop-1.5.8.jar

# clojure can load src directly, so not using AOT-compilation here
export CLASSPATH=$CLASSPATH:$AGRAPH_CLOJURE/src
export CLASSPATH=$CLASSPATH:$AGRAPH_CLOJURE/test
export CLASSPATH=$CLASSPATH:$AGRAPH_CLOJURE/tutorial

export CLASSPATH=$CLASSPATH:$CLOJURE_PROJECTS/swank-clojure/src

export AGRAPH_JVM_ARGS=-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.NoOpLog
