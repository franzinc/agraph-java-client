# AllegroGraph Java client release history

## 2.0.1

### rfe14993: Make AG resources usable in try-with-resources statements

Various resource classes, such as AGServer and
AGRepositoryConnection, now implement the AutoCloseable interface
introduced in Java 7. This makes it possible to use instances of
these classes in try-with-resources statements:

try (AGServer server = ...;
     AGRepository repo = ...;
     AGRepositoryConnection conn = ...;
     TupleQueryResult result = ...) {
    while (result.hasNext()) { ... }
}

The com.franz.util.Closeable interface has been removed, since it
is identical to AutoCloseable.

## 2.0.0

### rfe14986: Upgrade to rdf4j 2.2.2

The Sesame version used by the Java client has been updated from
Sesame 2.9.0 to RDF4J 2.2.2. Since the Java client is an extension of
the Sesame (now RDF4J) API, users should follow the migration guide at
http://docs.rdf4j.org/migration/

### rfe14997: Smarter clear operation for Jena

Performance of `AGGraph.clear()` and `AGGraph.remove()` operations has
been significantly improved.

## 1.0.10

### Moved to Sesame 2.9.0

The Sesame library that the client is based on has been updated to
version 2.9.0. There have been no changes to the client's API.

### Query demo

A new tutorial program has been added and can be found in the
`tutorials` directory. This program can be used to send SPARQL queries
to the server and output results in any format supported by Sesame.

## 1.0.7 - 1.0.9

No changes

## 1.0.6

### rfe14970: Speed up AllegroGraph java client connection pools

Depending on the connection pool configuration, many unnecessary
requests could be made to the AllegroGraph server when
borrowing/returning connections to/from a connection pool.

These calls have been cleaned up, resulting in improved connection
pool performance.

### Fixed some Javadoc warnings

## 1.0.5

### rfe14990: Provide methods to download query results
 The Java client now contains methods to download and save query results to a file. The new methods are:
 - AGRepositoryConnection.downloadStatements
 - AGRepositoryConnection.streamStatements
 - AGQuery.download
 - AGQuery.stream

The methods have multiple overloads, allowing for the output path and desired output format to be passed in a variety of ways.

## 1.0.4
 - Queries are now sent in request bodies instead of query strings.  This results in faster processing on the server.
 - bug24648:inferredGraph parameter missing from AGMaterializer:

When materializing triples through the Java API it is now possible to specify the graph into which the generated triples will be added.

## 1.0.3
No changes

## 1.0.2
Tutorial updates
