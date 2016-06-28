
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>

insert data {
  <eh:graph1> <eh:containsPerson> <eh:bob> .
  <eh:graph2> <eh:containsPerson> <eh:alice> .
  <eh:this> <eh:is> _:b1 .
  _:b1 <eh:is> 'triple' .
  graph <eh:graph1> {
    <eh:bob> <http://xmlns.com/foaf/0.1/name> 'Bob'  .
    <eh:this> <eh:is> 'triple'  .
  }
  graph  <eh:graph2> {
    <eh:alice> <http://xmlns.com/foaf/0.1/name> 'Alice' .
    <eh:this> <eh:is> 'triple'  .
  }
} ;

# Adds's the two containsPerson triples to graph3
# ADD DEFAULT TO <eh:graph3> ;

# Add's to the default graph
# INSERT DATA { <eh:c> <eh:yyyy> <eh:here> }

# Only removes the triple in the default graph
# DELETE DATA {  <eh:this> <eh:is> 'triple'  . }

# DROP DEFAULT