PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
insert data {
  <http://example.org/graph1> <http://purl.org/dc/elements/1.1/publisher> 'Bob' .
  <http://example.org/graph1> <http://example.org/containsPerson> <http://example.org/bob> .
  <http://example.org/graph2> <http://purl.org/dc/elements/1.1/publisher> 'Alice' .
  <http://example.org/graph2> <http://example.org/containsPerson> <http://example.org/alice> .
  graph <http://example.org/graph1> {
    <http://example.org/bob> <http://xmlns.com/foaf/0.1/name> 'Bob'  .
    <http://example.org/bob> <http://xmlns.com/foaf/0.1/mbox> 'bob@example.org' .
    <http://example.org/bob> <http://example.org/age> '42'^^<http://www.w3.org/2001/XMLSchema#integer> .
    <http://example.org/bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/alice> .
    <http://example.org/alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/bob> .
  }
  graph  <http://example.org/graph2> {
    <http://example.org/alice> <http://xmlns.com/foaf/0.1/name> 'Alice' .
    <http://example.org/alice> <http://xmlns.com/foaf/0.1/mbox> 'alice@example.org' .
  }
} ;

#PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
#PREFIX foaf: <http://xmlns.com/foaf/0.1/>
#INSERT {?x rdfs:label ?y . } WHERE { <http://example.org/bob>  foaf:name ?y } ;
#
#PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
#PREFIX foaf: <http://xmlns.com/foaf/0.1/>
#INSERT {?x rdfs:label ?y . } WHERE { ?x  foaf:name ?y } ;

INSERT { GRAPH <http://new.org> {?x ?y ?z} } WHERE { ?x ?y ?z } ;
