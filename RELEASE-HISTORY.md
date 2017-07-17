# AllegroGraph Java client release history

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
