# Franz AllegroGraph client API for Java and Clojure

Client API to [Franz AllegroGraph](http://franz.com/agraph/)
triple store database, version 4.

This [agraph-java-client](http://github.com/franzinc/agraph-java-client) provides:

* Java client API
  * Adapter for Sesame
  * Adapter for Jena
* Clojure client API

[AllegroGraph Docs](http://franz.com/agraph/support/documentation/current/)


## Server Prerequisites

* [Download AllegroGraph](http://franz.com/agraph/downloads/)
  version 4
  * Requires Linux 64-bit or a Linux VM on Windows or Mac OS X
* Install AllegroGraph
* Java version 1.6.0 or higher, and any operating system should work
  with these jars


## Download options

[Distribution from Franz](http://franz.com/agraph/allegrograph/clients.lhtml)

[Maven repository](http://clojars.org/groups/com.franz)

[Git repository](http://github.com/franzinc/agraph-java-client)


## Java

Communicates to AGraph server over http.

Supports SPARQL and Prolog queries.


### OpenRDF/Sesame

[Sesame](http://www.rdf4j.org/) 2.7.11,
([API](http://rdf4j.org/sesame/2.7/apidocs/))

The primary public package is
<code>com.franz.agraph.repository</code>.

[Javadocs](http://franz.com/agraph/support/documentation/current/javadoc/index.html)

[Tutorial](http://franz.com/agraph/support/documentation/current/java-tutorial/java-tutorial.html)


### Jena

[Jena](http://jena.apache.org/) 2.11.1

The primary public package is <code>com.franz.agraph.jena</code>.

[Javadocs](http://franz.com/agraph/support/documentation/current/javadoc/index.html)

[Tutorial](http://franz.com/agraph/support/documentation/current/java-tutorial/jena-tutorial.html)


## Clojure

Uses [Clojure](http://clojure.org) 1.4

The tutorial included is similar to the Python and Java tutorials.
The comment section at the top of the file gives instructions to get
started.

* clojure/test/com/franz/agraph/tutorial.clj


### Usage with Leiningen or Maven

Add to your leiningen project.clj dependencies:

    [com.franz/agraph-clj "4.7"]

See [agraph-clj on clojars](http://clojars.org/com.franz/agraph-clj)

This in turn depends on agraph-java-client, apache httpclient, jena,
sesame, and others, which lein will download.

To download dependencies into your lib directory:

    lein deps

To start a REPL:

    lein repl

For more help, see
[Leiningen](http://github.com/technomancy/leiningen/tree/stable).


### Usage with Ant

The [agraph-java-client
download](http://franz.com/agraph/allegrograph/clients.lhtml)
includes jar files for agraph-java-client and jars on which it depends.

The agraph-clj.jar file can be created using ant

    ant build

Alternatively, add the agraph-java-client/clojure/src directory to
your classpath. Clojure will compile as needed.


## Development

For Ant users, the Java library includes build.xml. The following
command line will build the agraph-java-client jar:

    ant build

For Maven users, the Java library includes pom.xml and an Ant target
to install. A pom-sesame.xml is also included because
openrdf-sesame-onejar is not available in another public maven repo.
Both agraph-java-client and openrdf-sesame-onejar are available
on [Clojars](http://clojars.org/groups/com.franz).
The following command line will build and install the jars for
agraph-java-client and openrdf-sesame-onejar to your local maven
directory (~/.m2/).

    ant mvn-install

The Clojure library includes a project.clj for use with Leiningen.
It depends on the agraph-java-client, so you will need to use the
mvn-install command above before using lein. The following command
line will install all dependencies in agraph-java-client/clojure/lib/.

    lein deps

Alternatively, for Ant users, the Clojure library includes a
build.xml and libs/clojure-1.4.0.jar.

    ant build


## Tutorials

There are three tutorials located in src/tutorial/ that can be
compiled and run. They are:

  TutorialExamples.java     - Example usage of the AG Sesame interface
  JenaTutorialExamples.java - Example usage of the AG Jena interface
  AttributesExample.java    - Example usage of AG Triple Attributes

### PREREQUISITES

An AllegroGraph server must be up and running in order to run the
tutorials.

The class for each tutorial declares a number of variables near the
top of its respective class definition, which provide the
information necessary to communicate with AllegroGraph. If necessary,
modify these variables to match the settings for your server before
compiling each source file.

By default, each tutorial looks for AllegroGraph on localhost at port
10035. Each will create a repository named after the respective
tutorial in the "java-catalog" catalog.

In order for the tutorial to run successfully, you must ensure that
the "java-catalog" catalog has been defined in your agraph.cfg prior
to starting AG, or change the value of CATALOG_ID to name a catalog
that exists on your server. Use the empty string ("") or null to
indicate the root catalog. All other variables must be updated to
correspond to your server configuration as well.


### Compiling Tutorials

From the current directory the following commands can be used to compile each tutorial.

  javac -cp 'lib/*:lib/sesame/*' src/tutorial/TutorialExamples.java
  javac -cp 'lib/*:lib/jena/*:lib/sesame/*' src/tutorial/JenaTutorialExamples.java 
  javac -cp 'lib/*:lib/sesame/*' src/tutorial/AttributesExample.java


### Running Tutorials

To run the Sesame and Jena tutorials, use the following command
lines. The argument 'all' indicates that all examples should be
run. You may also specify any number of integer arguments to indicate
which specific examples to run. If no args are passed, the default is
to run all examples.

<code>java -cp 'src:lib/*:lib/sesame/*:lib/logging/commons-logging-1.1.1.jar:lib/logging/slf4j-api-1.6.4.jar:lib/logging/log4j-1.2.16.jar:lib/logging/slf4j-log4j12-1.6.4.jar' tutorial/TutorialExamples all</code>

or 

<code>java -cp 'src:lib/*:lib/jena/*::lib/sesame/*:lib/logging/commons-logging-1.1.1.jar:lib/logging/slf4j-api-1.6.4.jar:lib/logging/log4j-1.2.16.jar:lib/logging/slf4j-log4j12-1.6.4.jar' tutorial/JenaTutorialExamples all</code>


To run the Attributes Example, use the following command line. The AttributesExample class accepts no arguments:

<code>java -cp 'src:lib/*:lib/sesame/*:lib/logging/commons-logging-1.1.1.jar:lib/logging/slf4j-api-1.6.4.jar:lib/logging/log4j-1.2.16.jar:lib/logging/slf4j-log4j12-1.6.4.jar' tutorial/AttributesExample</code>


## License

Copyright (c) 2008-2016 Franz Inc.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at
[http://www.eclipse.org/legal/epl-v10.html](http://www.eclipse.org/legal/epl-v10.html)

