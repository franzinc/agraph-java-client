#!/bin/bash
# set -x

export AGRAPH_JAVA=$(readlink -f $(dirname "$0"))

export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/commons-codec-1.3.jar
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/commons-httpclient-3.1.jar
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/commons-logging-1.1.1.jar
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/openrdf-sesame-2.2.4-onejar.jar
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/slf4j-api-1.5.8.jar
export CLASSPATH=$CLASSPATH:$AGRAPH_JAVA/lib/slf4j-nop-1.5.8.jar

export AGRAPH_JVM_ARGS=-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.NoOpLog

rm -rf classes
mkdir -p classes

javac -sourcepath src -d classes/ src/com/franz/agraph/repository/*.java src/tutorial/TutorialExamples.java
