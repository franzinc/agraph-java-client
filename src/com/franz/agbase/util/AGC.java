
 // BEGIN GENERATED CODE


 // Code generated on 2008-08-26 16:08 
 //   from agraph\lisp\agdirect.lisp 2008-08-26 10:21
 //   from agraph\lisp\agjava.lisp 2008-08-26 16:08

 package com.franz.agbase.util;

  

 public class AGC {

  

 public static final int AG_DIRECT_LEVEL = 2 ;  // 0x2

 // These constants define the tag bytes that 
 //  identify data in the socket stream. 
 // 
 // Tagged data stream: integer character string float double 
 //   seqences of integer string float double 
 // All tags are 1xxx xxxx.
 // 

 public static final int TAG_START = 128 ;  // 0x80

 //  tag=10sn nnnn
 //  integer: [tag][low-byte][byte]... [high-byte]
 //	              neg value is (-1-n) 
 //	              s=0 positive s=1 negative 
 //	              nnn=length code nnn=0..22 integers 0..22
 //	              or (1 + (nnn - 23)) is number of bytes
 public static final int TAG_INT_START = 128 ;  // 0x80

 //  Top end of integer tags.  Start of other tags.
 public static final int TAG_INT_END = 192 ;  // 0xc0

 //  Mask bits for immediate part of integer tag.
 public static final int TAG_INT_MASK = 31 ;  // 0x1f

 //  Mask bit for sign part of integer tag.
 public static final int TAG_SIGN_MASK = 32 ;  // 0x20

 //  null: [tag] 
 public static final int TAG_NULL = 192 ;  // 0xc0

 //  byte: [tag][integer]
 public static final int TAG_BYTE = 193 ;  // 0xc1

 //  short: [tag][integer]
 public static final int TAG_SHORT = 194 ;  // 0xc2

 //  int: [tag][integer]
 public static final int TAG_INT = 195 ;  // 0xc3

 //  long: [tag][integer]
 public static final int TAG_LONG = 128 ;  // 0x80

 //  char: [tag][integer]
 public static final int TAG_CHAR = 196 ;  // 0xc4

 //  [tag][sign byte][e0 byte][e1 byte][c0 byte][c1 byte][c2 byte]
 public static final int TAG_FLOAT = 197 ;  // 0xc5

 //  [tag][sign byte] [e0 byte] [e1 byte]
 //  	                [c0 byte] [c1 byte] [c2 byte] 
 //  	                [c3 byte] [c4 byte] [c5 byte] [c6 byte]
 public static final int TAG_DOUBLE = 198 ;  // 0xc6

 public static final int TAG_open7 = 199 ;  // 0xc7

 //  sequence: [tag][integer elt count][subtag][tagged element]... 
 //            all elements must be consistent with subtag 
 public static final int TAG_SEQ = 200 ;  // 0xc8

 //  sparse sequence: [tag][integer length] [integer elt count]
 //                        [subtag][tagged default elt]
 //                        { [integer index] [tagged elt]}...
 //                   all elements must have same tag
 public static final int TAG_SPARSE = 201 ;  // 0xc9

 //  object: [tag][string type] [integer parts count] 
 //               { [string part name] [part]}...
 public static final int TAG_OBJECT = 202 ;  // 0xca

 //  boolean true: [tag]
 public static final int TAG_TRUE = 203 ;  // 0xcb

 //  boolean false: [tag]
 public static final int TAG_FALSE = 204 ;  // 0xcc

 public static final int TAG_OPENd = 205 ;  // 0xcd

 //  operation: [tag][string op name]
 //                  [integer op index] [integer parts count]
 //             one-way code is zero op index
 //             negative op index means ignored result
 //  result(s) is(are) sent as an operation
 //      [tag][op name][op index][vals count][tagged val]...
 //      [tag][op name][neg index][2][err type string][err string]
 public static final int TAG_OP = 206 ;  // 0xce

 //  end tag: [tag]
 //            when total length given as -1 -- could be used to
 //       re-sync data stream? 
 public static final int TAG_ENDER = 207 ;  // 0xcf

 //  byte array: [tag][int length][byte]...[byte] 
 public static final int TAG_BYTES = 210 ;  // 0xd2

 //  UPI: [tag][byte0]...[byte11] 
 public static final int TAG_UPI = 211 ;  // 0xd3

 //  Duplicate item in sequence: {tag][integer n] 
 //       entry is copied from (this_index - n)     
 public static final int TAG_DUP = 212 ;  // 0xd4

 //  Repeated item in sequence: {tag][integer n][integer m] 
 //      entry is copied from (this_index - n) m times      
 public static final int TAG_REP = 213 ;  // 0xd5

 //  long string: [tag][integer length][string fragment]... 
 //  ONLY string tags at or above this code.
 public static final int TAG_LSTR = 222 ;  // 0xde

 public static final int TAG_STRING = 222 ;  // 0xde

 //  string run fragment: [tag][integer length][integer Unicode code]
 //                      replaces long run of the same char 
 public static final int TAG_FRAG = 223 ;  // 0xdf

 // tag=111n nnnn 
 //  short string: [tag][integer Unicode code]...
 public static final int TAG_SSTR_START = 224 ;  // 0xe0

 //  Top end of short string tags.
 public static final int TAG_SSTR_END = 256 ;  // 0x100

 //  Top end of all tags.
 public static final int TAG_END = 256 ;  // 0x100

 // Immediate integer values are less than this.
 public static final int TAG_IMM_TOP = 23 ;  // 0x17

 // Short strings are shorter than this.
 public static final int TAG_SSTR_MAX = 32 ;  // 0x20

 // String fragments are longer than this.
 public static final int TAG_FRAG_MIN = 5 ;  // 0x5

 // Sequence fragments are longer than this.
 public static final int TAG_RUN_MIN = 2 ;  // 0x2

 // These constants define socket and port parameters. 

 public static final int DEFAULT_BUFFER_SIZE = 1024 ;  // 0x400

 public static final int PORT_IDLE = 0 ;  // 0x0

 public static final int PORT_CLOSED = -1 ;  // 0x-1

 public static final int PORT_REQUEST = 3 ;  // 0x3

 public static final int PORT_WAITING_RESPONSE = 2 ;  // 0x2

 public static final int PORT_WAITING_REPLY = 4 ;  // 0x4

 public static final int PORT_MESSAGE = 1 ;  // 0x1

 public static final int PORT_INVOKE = 5 ;  // 0x5

 public static final int PORT_WAITING_RESULT = 6 ;  // 0x6

 //Number of bytes in a UPI object byte-array
 public static final int UPI_WIDTH = 12 ;  // 0xc

 //  Operation: :call    
 //  Arguments: op-string arg...       
 //    Results: value-of-call...         
 public static final String OP_CALL = ":call" ;

 //  Operation: :verify    
 //  Arguments: [client-level]       
 //    Results: server-version-string        
 public static final String OP_VERIFY = ":verify" ;

 //  Operation: :disconnect    
 //  Arguments: ---       
 //    Results: ---        
 public static final String OP_DISCONNECT = ":disconnect" ;

 //  Operation: :debug    
 //  Arguments: [debug-setting]       
 //    Results: debug-values         
 public static final String OP_DEBUG = ":debug" ;

 //=============================================================================

 // Constants passed back and forth between Lisp and Java.

 public static final int AGU_NULL_CONTEXT = -2 ;  // 0x-2

 public static final int AGU_WILD = -1 ;  // 0x-1

 public static final int AGU_MAP_QUERY = 1 ;  // 0x1

 public static final int AGU_MAP_ADD = 11 ;  // 0xb

 public static final int AGU_MAP_REP = 21 ;  // 0x15

 public static final int AGU_IQ_COUNT = 1 ;  // 0x1

 public static final int AGU_IQ_CHUNKS = 2 ;  // 0x2

 public static final int AGU_IQ_UNTHRESH = 3 ;  // 0x3

 public static final int AGU_IQ_CHTHRESH = 4 ;  // 0x4

 public static final int AGU_IQ_FLAVORS = 5 ;  // 0x5

 public static final int AGU_IS_UNTHRESH = 13 ;  // 0xd

 public static final int AGU_IS_CHTHRESH = 14 ;  // 0xe

 public static final int AGU_IA_FLAVORS = 15 ;  // 0xf

 public static final int AGU_ID_FLAVORS = 25 ;  // 0x19

 public static final int AGU_IS_FLAVORS = 35 ;  // 0x23

 public static final int AGU_IQ_ALL = 31 ;  // 0x1f

 public static final String AGU_ANON_NAME = "anon";

 public static final String AGU_NODE_NAME = "node";

 public static final String AGU_LITERAL_NAME = "literal";

 public static final String AGU_LITERAL_LANG_NAME = "literal/lang";

 public static final String AGU_TYPED_LITERAL_NAME = "typed-literal";

 public static final String AGU_TRIPLE_NAME = "triple";

 public static final String AGU_DEFAULT_GRAPH_NAME = "default-graph";

 public static final String AGU_ENCODED_STRING_NAME = "encoded-string";

 public static final String AGU_ENCODED_INTEGER_NAME = "encoded-integer";

 public static final String AGU_ENCODED_FLOAT_NAME = "encoded-float";

 public static final String AGU_ENCODED_TRIPLE_ID_NAME = "encoded-triple-id";

 public static final String AGU_UNKNOWN_NAME = "unknown";

 public static final int AGU_ANON = 1 ;  // 0x1

 public static final int AGU_NODE = 2 ;  // 0x2

 public static final int AGU_LITERAL = 3 ;  // 0x3

 public static final int AGU_LITERAL_LANG = 4 ;  // 0x4

 public static final int AGU_TYPED_LITERAL = 5 ;  // 0x5

 public static final int AGU_TRIPLE = 6 ;  // 0x6

 public static final int AGU_DEFAULT_GRAPH = 7 ;  // 0x7

 public static final int AGU_ENCODED_STRING = 8 ;  // 0x8

 public static final int AGU_ENCODED_INTEGER = 9 ;  // 0x9

 public static final int AGU_ENCODED_FLOAT = 10 ;  // 0xa

 public static final int AGU_ENCODED_TRIPLE_ID = 11 ;  // 0xb

 public static final int AGU_UNKNOWN = 0 ;  // 0x0

 public static final int AGU_PROTOCOL_LEVEL = 7 ;  // 0x7

 // These are the functions called from Java.
 // A few are called in strings passed to evalInServer().

 public static final String AGJ_TRACE_INT = "db.agraph.servers::agj-trace-int" ;

 public static final String AGJ_TRACE_INT_A = "agj-trace-int" ;

 public static final String AG_EXISTS_P = "db.agraph.servers::ag-exists-p" ;

 public static final String AGJ_NAMESPACES_A = "agj-namespaces" ;

 public static final String AGJ_GEOSUBTYPES = "agj-geospatial-subtypes" ;

 public static final String AGJ_DEFAULT_GRAPH = "agj-default-graph" ;

 public static final String AG_SYNC = "ag-sync" ;

 public static final String AG_CLOSE = "ag-close" ;

 public static final String AG_LOAD = "ag-load" ;

 public static final String AG_LOAD_PORTION_OF_FILE = "ag-load-portion-of-file" ;

 public static final String AG_RDF = "ag-rdf" ;

 public static final String AG_NUMBER = "ag-number" ;

 public static final String AG_INDEX = "ag-index" ;

 public static final String AG_ALL = "ag-all" ;

 public static final String AG_GET_TRIPLE_RANGE = "ag-get-triple-range" ;

 public static final String AG_GET_TRIPLES = "ag-get-triples" ;

 public static final String AG_DISCARD_CURSOR = "ag-discard-cursor" ;

 public static final String AG_NEXT_WITH_PARTS = "ag-next-with-parts" ;

 public static final String AG_NEXT = "ag-next" ;

 public static final String AG_ADD_TRIPLE = "ag-add-triple" ;

 public static final String AG_ADD_TRIPLES = "ag-add-triples" ;

 public static final String AG_DELETE = "ag-delete" ;

 public static final String AG_NEW_NODE = "ag-new-node" ;

 public static final String AG_GET_NODE_PARTS = "ag-get-node-parts" ;

 public static final String AG_GET_TRIPLE_PARTS = "ag-get-triple-parts" ;

 public static final String AG_APPLY = "db.agraph.servers::ag-apply" ;

 public static final String AGJ_EVAL = "db.agraph.servers::agj-eval" ;

 public static final String AGJ_EVAL_A = "agj-eval" ;

 public static final String AG_SELECT_TRIPLES = "ag-select-triples" ;

 public static final String AG_SELECT_VALUES = "ag-select-values" ;

 public static final String AGJ_SERVER_OPTIONS = "db.agraph.servers::agj-server-options" ;

 public static final String AG_CLIENT_OPTIONS = "ag-client-options" ;

 public static final String AG_TWINQL_ASK = "ag-twinql-ask" ;

 public static final String AG_TWINQL_SELECT = "ag-twinql-select" ;

 public static final String AG_TWINQL_FIND = "ag-twinql-find" ;

 public static final String AG_TWINQL_CONSTRUCT = "ag-twinql-construct" ;

 public static final String AG_TWINQL_DESCRIBE = "ag-twinql-describe" ;

 public static final String AG_TWINQL_QUERY = "ag-twinql-query" ;

 public static final String AG_INDEXING = "ag-indexing" ;

 public static final String AG_MAPPING = "ag-mapping" ;

 public static final String AG_FREETEXT_PREDICATES = "ag-freetext-predicates" ;

 public static final String AG_FREETEXT_STATEMENTS = "ag-freetext-statements" ;

 public static final String AG_FREETEXT_SUBJECTS = "ag-freetext-subjects" ;

 public static final String AG_FEDERATE = "db.agraph.servers::ag-federate" ;

 public static final String AG_FIND_STORE = "db.agraph.servers::ag-find-store" ;

 public static final String AG_GET_STORES = "ag-get-stores" ;

 public static final String AG_INTERN_RES = "ag-intern-res" ;

 public static final String AG_INTERN_LIT = "ag-intern-lit" ;

 public static final String AG_ACCESS_TRIPLE_STORE = "db.agraph.servers::ag-access-triple-store" ;

 public static final String AG_INFER_TRIPLES = "ag-infer-triples" ;

 public static final String AG_INFER_TRIPLE_RANGE = "ag-infer-triple-range" ;

 public static final String AG_ADD_PART = "ag-add-part" ;

 public static final String AG_SERIALIZE_TRIPLES = "ag-serialize-triples" ;

 public static final String AG_REGISTER_GENERATOR = "ag-register-generator" ;

 public static final String AG_COPY_GENERATORS = "ag-copy-generators" ;

 public static final String AG_BREADTH_FIRST_SEARCH = "ag-breadth-first-search" ;

 public static final String AG_DEPTH_FIRST_SEARCH = "ag-depth-first-search" ;

 public static final String AG_BIDIRECTIONAL_SEARCH = "ag-bidirectional-search" ;

 public static final String AG_BREADTH_FIRST_MAP = "ag-breadth-first-map" ;

 public static final String AG_DEPTH_FIRST_MAP = "ag-depth-first-map" ;

 public static final String AG_BIDIRECTIONAL_MAP = "ag-bidirectional-map" ;

 public static final String AG_BREADTH_FIRST_ALL = "ag-breadth-first-all" ;

 public static final String AG_DEPTH_FIRST_ALL = "ag-depth-first-all" ;

 public static final String AG_BIDIRECTIONAL_ALL = "ag-bidirectional-all" ;

 public static final String AG_BREADTH_FIRST_BIPARTITE = "ag-breadth-first-bipartite" ;

 public static final String AG_DEPTH_FIRST_BIPARTITE = "ag-depth-first-bipartite" ;

 public static final String AG_BIDIRECTIONAL_BIPARTITE = "ag-bidirectional-bipartite" ;

 public static final String AG_NODAL_NEIGHBORS = "ag-nodal-neighbors" ;

 public static final String AG_NODAL_DEGREE = "ag-nodal-degree" ;

 public static final String AG_EGO_GROUP = "ag-ego-group" ;

 public static final String AG_DENSITY = "ag-density" ;

 public static final String AG_DEGREE_CENTRALITY = "ag-degree-centrality" ;

 public static final String AG_CLOSENESS_CENTRALITY = "ag-closeness-centrality" ;

 public static final String AG_BETWEENNESS_CENTRALITY = "ag-betweenness-centrality" ;

 public static final String AG_IS_CLIQUE = "ag-is-clique" ;

 public static final String AG_CLIQUES = "ag-cliques" ;

 public static final String AG_MAP_CLIQUES = "ag-map-cliques" ;

 public static final String AG_REGISTER_STRIPING = "ag-register-striping" ;

 public static final String AG_ADD_SUBTYPE = "ag-add-subtype" ;

 public static final String AG_ENCODE_GEOUPI = "ag-encode-geoupi" ;

 public static final String AG_DECODE_GEOUPI = "ag-decode-geoupi" ;

 public static final String AG_GET_GEO_IN_BOX = "ag-get-geo-in-box" ;

 public static final String AG_GET_GEO_TRIPLES = "ag-get-geo-triples" ;

 public static final String AG_ADD_POLYGON = "ag-add-polygon" ;

 public static final String AG_GET_POLYGON = "ag-get-polygon" ;

 public static final String AG_POINTS_INSIDE_POLY = "ag-points-inside-poly" ;

 public static final String AG_ADD_GEO_MAPPING = "ag-add-geo-mapping" ;

  } 

 //   END GENERATED CODE

