# Franz AllegroGraph client API for Java

Client API to [Franz AllegroGraph](http://franz.com/agraph/)
triple store database.

This [agraph-java-client](http://github.com/franzinc/agraph-java-client) provides:

* Java client API
  * Adapter for Sesame
  * Adapter for Jena
  
[AllegroGraph Docs](http://franz.com/agraph/support/documentation/current/)


## Server Prerequisites

* [Download AllegroGraph](http://franz.com/agraph/downloads/)
  * Requires Linux 64-bit or a Linux VM on Windows or Mac OS X
* Install AllegroGraph
* Java version 8 or higher, and any operating system should work
  with these jars


## Download options

[Distribution from Franz](http://franz.com/agraph/allegrograph/clients.lhtml)

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


### Usage

The [agraph-java-client
download](http://franz.com/agraph/allegrograph/clients.lhtml)
includes jar files for agraph-java-client and jars on which it depends.

Programs that use the API should include all JAR files from the lib/ directory of the distribution on their classpath.

It is also possible to use the JAR file as a Maven artifact. To do this, first install it into your local repository (it is located in ~/.m2/):

    mvn install:install-file -Dfile=lib/agraph-java-client-*.jar -DpomFile=pom.xml

Then reference that in your Maven project

    <dependency>
      <groupId>com.franz</groupId>
      <artifactId>agraph-java-client</artifactId>
      <version>1.0.0</version>
      <scope>compile</scope>
    </dependency>

For Gradle, use this

    compile group: 'com.franz', name: 'agraph-java-client', version: '1.0.0'

Apache Ivy syntax:

    <dependency org="com.franz" name="agraph-java-client" rev="1.0.0"/>

SBT (Scala):

    libraryDependencies += "com.franz" % "agraph-java-client" % "1.0.0"

Leiningen (Clojure):

    [com.franz/agraph-java-client "1.0.0"]

Note that when using the API through Maven all dependencies are downloaded from the central repository - JARs included in the distribution are not used.

## Development

### Testing

Use this command to run the default testsuite. This assumes that AllegroGraph is listening on the local machine on the default port (10035).

    mvn test

To include long running tests, do

    mvn test -Dtests.include=test.TestSuites\$Prepush,test.TestSuites\$Stress   

### Distribution

To build a distribution zip, do

    mvn package    

## Tutorials

There are three tutorials located in tutorials/ that can be
compiled and run. They are:

  sesame/        - Example usage of the AG Sesame interface
  jena/          - Example usage of the AG Jena interface
  attributes/    - Example usage of AG Triple Attributes

### PREREQUISITES

An AllegroGraph server must be up and running in order to run the
tutorials.

The class for each tutorial declares a number of variables near the
top of its respective class definition, which provide the
information necessary to communicate with AllegroGraph. If necessary,
modify these variables to match the settings for your server before
compiling each tutorial.

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

Each tutorial is a separate Maven project. To compile the tutorials
first install the AllegroGraph Java client into your local repository.
This process is described in the 'Usage' section. Then run the
following command in the directory containing the tutorial:

     mvn compile

### Running Tutorials

To run one of the tutorials, use the following command line:

    mvn java:exec

in the directory containing the tutorial you wish to run.

Sesame and Jena tutorials contain multiple, numbered examples.
It is possible to run just the specified examples by passing
their numbers as arguments in the following way:

   mvn java:exec -Dexec.args="1 2 3 5 8 13 21"

The argument 'all' indicates that all examples should be
run.

## License

Copyright (c) 2008-2016 Franz Inc.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at
[http://www.eclipse.org/legal/epl-v10.html](http://www.eclipse.org/legal/epl-v10.html)
