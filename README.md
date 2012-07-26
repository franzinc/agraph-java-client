# Franz AllegroGraph client API for Java and Clojure

Client API to [Franz AllegroGraph](http://www.franz.com/agraph/)
triple store database, version 4.

This [agraph-java-client](http://github.com/franzinc/agraph-java-client) provides:

* Java client API
  * Adapter for Sesame
  * Adapter for Jena
* Clojure client API

[AllegroGraph Docs](http://www.franz.com/agraph/support/documentation/v4/)


## Server Prerequisites

* [Download AllegroGraph](http://www.franz.com/agraph/downloads/)
  version 4
  * Requires Linux 64-bit or a Linux VM on Windows or Mac OS X
* Install AllegroGraph
* Java version 1.6.0 or higher, and any operating system should work
  with these jars


## Download options

[Distribution from Franz](http://www.franz.com/agraph/allegrograph/clients.lhtml)

[Maven repository](http://clojars.org/groups/com.franz)

[Git repository](http://github.com/franzinc/agraph-java-client)


## Java

Communicates to AGraph server over http.

Supports SPARQL and Prolog queries.


### OpenRDF/Sesame

[Sesame](http://www.openrdf.org/) 2.6.8,
([API](http://www.openrdf.org/doc/sesame2/2.6.8/apidocs/))

The primary public package is
<code>com.franz.agraph.repository</code>.

[Javadocs](http://www.franz.com/agraph/support/documentation/v4/javadoc/index.html)

[Tutorial](http://www.franz.com/agraph/support/documentation/v4/java-tutorial/java-tutorial-40.html)


### Jena

[Jena](http://jena.sourceforge.net/) 2.6.2

The primary public package is <code>com.franz.agraph.jena</code>.

[Javadocs](http://www.franz.com/agraph/support/documentation/v4/javadoc/index.html)

[Tutorial](http://www.franz.com/agraph/support/documentation/v4/java-tutorial/jena-tutorial-40.html)


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
download](http://www.franz.com/agraph/allegrograph/clients.lhtml)
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


## License

Copyright (c) 2008-2012 Franz Inc.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at
[http://www.eclipse.org/legal/epl-v10.html](http://www.eclipse.org/legal/epl-v10.html)

