# AllegroGraph Java client release history

## 3.0.5

### Add support for server returning a 204 response

The AllegroGraph server, starting in vesion 7.1.0, may return
a 204 (no content) response when it knows that it will not
be returning any data.  The client will
then create a null cursor, rather than creating an http
cursor which when used immediately returns 'no data left'


## 3.0.4

### Add support for query options API

The server-side support for setting query options on a per-user,
per-repository basis is available in AG v7.1.0 and later and can be
accessed from the Java client via `AGRepositoryConnection` methods:

   - `Map<String, String> getQueryOptions()`;
   - `String getQueryOption(String name)`;
   - `void setQueryOption(String name, String value)`;
   - `void removeQueryOption(Sring name)`.

These methods mimic the namespaces API, the only difference being the
`AGRepositoryConnection.getQueryOptions()` method that returns a
`Map<String, String>` instead of a `RepositoryResult` for simplicity
reasons.

## 3.0.3

### Add connection pool property AGPoolProp.lifo

If this property has a value true (the default) then
connections are taken out of the pool in a Last In First Out manner.
If this property has a value false then connections
are taken out of the pool in a First In First Out manner.
For a connection pool to a load balancer FIFO is a better choice
as it will cause work to be distributed to all backends.

Also add a document a test cases related to handling failures during
a transaction.

## 3.0.2

### Use HTTPS URL to reference repo1.maven.org

Use HTTPS to access repo1.maven.org to avoid errors.

### rfe16340: Add test for loading data into multiple contexts

The server-side support for loading data into multiple contexts is
available since v7.0.0. In order to enable it, the system property
`"com.franz.agraph.http.AGProtocol.overrideContext"` (the name is also
accessible via `AGProtocol.PROP_OVERRIDE_CONTEXT`) must be set to
`"true"`, otherwise all but first context arguments are ignored.

### rfe16331: Add a system property to toggle context overriding

The new system property
`"com.franz.agraph.http.AGProtocol.overrideContext"` (the name is also
accessible via `AGProtocol.PROP_OVERRIDE_CONTEXT`) can be used to
enable context overriding behaviour according to RDF4J API
specification. The server-side support is only available since v7.0.0.

## 3.0.1

### bug25987: Add implementation for AGServer.changeUserPassword()

The `AGServer.changeUserPassword()` method was previously
unimplemented. It is now supported.

### rfe16301: Speed up conn.hasStatement()

`AGRepositoryConnection.hasStatement()` is significantly faster now,
because the counting is moved to the server side and does not require
parser initialization.

## 3.0.0

### rfe15904: Upgrade to rdf4j 2.4.0

The RDF4J dependency has been upgraded from 2.2.2 to 2.4.0.  RDF4J
versions prior to 2.4.0 have a moderate severity security
vulnerability (CVE-2018-1000644).

### rfe16203: Improved efficiency of conn.hasStatement()

AGRepositoryConnection.hasStatement() is now much more efficient.  

### rfe15921: nD geospatial automation

nD Geospatial Automation can now be enabled, disabled and queried.

New methods:
 - AGRepositoryConnection.getNDGeospatialDatatypeAutomation
 - AGRepositoryConnection.enableNDGeospatialDatatypeAutomation
 - AGRepositoryConnection.disableNDGeospatialDatatypeAutomation

### Removed logging framework

Users must now choose their preferred logging framework as described
here: http://docs.rdf4j.org/programming/#_logging_slf4j_initialization

## 2.2.0

### rfe15689: Warmup operation

It is now possible to execute the 'warmup' operation through
the Java client. This will cause the server to read some 
internal structures of a repository into memory, thus speeding 
up processing of future requests in that repository.

Warmup can be executed in two ways:

   - by calling the `warmup()` method of a connection object.
   - by creating a connection pool with the `WARMUP` parameter
     set to true.
     
### bug25360: HTTP connection leaks

HTTP connections used to not be cleaned up properly if
the server returned an error in response to a query.
Connections used by Jena select queries were also leaked.
This resulted in suboptimal resource usage.

## 2.1.0

### Implemented support of TupleQueryResultFormat.TSV

Changed preferred TupleQueryResultFormat for tuple queries to
be TSV when possible (as long as the server version is 6.4.2
or higher). Also implemented results streaming for TSV and made
the results streaming by default.

All of the above is expected to boost the performance of tuple queries.

### rfe10713: Two phase commit (2PC) support

The AllegroGraph Java client can now operate as a JTA (Java
Transaction API) resource manager. This was specifically tested
against [Atomikos Transaction Essentials Transaction
Manager](https://www.atomikos.com/Main/TransactionsEssentials).
v4.0.6.  

2PC operation requires AllegroGraph server version 6.5.0 or better.

See the AllegroGraph Two Phase Commit (2PC) documentation for details.


## 2.0.3

### bug25277: AGVirtualRepository.federate() constructs incomplete triple store spec
    
AGVirtualRepository.federate() uses getSpec() on each supplied
AGRepository to construct the triple store spec to use for the
federation.  getSpec() returns something like `<reponame>` or
`<catname:reponame>`.  This is missing information that is necessary to
federate against a repo that is not local to the AG server
performing the federation.

Missing info: scheme, host, port, user, password.

This has been fixed.

### rfe15577: Automatic retry of idempotent HTTP requests

In case a GET request (e.g. a repository query) fails due to a
connection problem, it will now be automatically retried once. In
particular this covers "connection reset" socket errors and HTTP 408
"Request Timeout" server responses. For now this is limited to GET
requests only.

New property:
 - com.franz.agraph.http.numRetries (default: 1)

## 2.0.2

### Additional multi-master replication commit setting

transactionLatencyTimeout has been added to the TransactionSettings
object.  This setting determines how long a commit operation should
wait for the transaction latency count to be satisfied before throwing
an error.

### Updated connection pooling implementation

Connection pooling now uses Commons Pool 2.4.2 instead of 1.6. 
The only change to the public interface of AG connection pools
is that closing a pool no longer shuts down connections that 
have been borrowed from a pool, but not returned. Such 
connections will now be closed as soon as they are returned
to the pool.

### Optionally buffer AGRepositoryConnection.add() for efficiency

Inside a transaction add() statements can optionally be buffered, then
sent to the backend in batches to improve efficiency.

New methods:
 - AGRepositoryConnection.setAddStatementBufferEnabled
 - AGRepositoryConnection.setAddStatementBufferMaxSize

New properties:
 - com.franz.agraph.repository.AGRepositoryConnection.useAddStatementBuffer
 - com.franz.agraph.repository.AGRepositoryConnection.addStatementBufferMaxSize

## 2.0.1

### Multi-master replication commit settings

It is now possible to specify distributed transaction behavior by:
   
   - using the new `setTransactionSettings` method of the 
     connection object 
     
   - passing a `TransactionSettings` instance to the `commit` method.
   
   - Wrapping code in a `try-with-resources` statement using
     the object returned by the `transactionSettingsCtx` method
     of the connection object.

Note that use of these settings will only have effect when using
multi-master replication on AllegroGraph 7 (which is not yet
released).  If an older version of AllegroGraph is used, or if you're
connected to a repository that is not a member of a replica set, the
settings will be ignored.
 
## 2.0.0

### rfe14986: Upgrade to rdf4j 2.2.2

The Sesame version used by the Java client has been updated from
Sesame 2.9.0 to RDF4J 2.2.2. Since the Java client is an extension of
the Sesame (now RDF4J) API, users should follow the migration guide at
http://docs.rdf4j.org/migration/

### rfe14997: Smarter clear operation for Jena

Performance of `AGGraph.clear()` and `AGGraph.remove()` operations has
been significantly improved.

### rfe14994: Move to Jena 3 

The Jena client is now based on Jena 3.3.0 instead of Jena 2. Users
should follow the migration guide at
https://jena.apache.org/documentation/migrate_jena2_jena3.html

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
