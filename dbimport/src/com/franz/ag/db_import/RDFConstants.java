package com.franz.ag.db_import;

public interface RDFConstants {
	
	public interface RDF {
		public static final String NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
		public static final String PROPERTY = NS + "Property";
		public static final String TYPE = NS + "type";
		
	}
	
	public interface RDFS {
		public static final String NS = "http://www.w3.org/2000/01/rdf-schema#";	
		public static final String CLASS = NS + "Class";
		public static final String LABEL = NS + "label";
		public static final String SUB_CLASS_OF = NS + "subClassOf";
		public static final String SUB_PROPERTY_OF = NS + "subPropertyOf";				
	}
	
	public interface XSD {
		public static final String NS = "http://www.w3.org/2001/XMLSchema#";
		public static final String STRING = NS + "string";
		public static final String INT = NS + "int";
		public static final String INTEGER = NS + "integer";
		public static final String NUMBER = NS + "number";		
		public static final String LONG = NS + "long";
		public static final String FLOAT = NS + "float";
		public static final String BOOLEAN = NS + "boolean";
		public static final String DATE = NS + "date";
		public static final String TIME = NS + "time";
		public static final String BASE64BINARY = NS + "base64Binary";		
	}
	
	public interface OWL {
		public static final String NS = "http://www.w3.org/2002/07/owl#";
		public static final String TRANSITIVE_PROPERTY = NS + "TransitiveProperty";
	}

	public interface DC_TERMS {
		public static final String NS = "http://purl.org/dc/terms/";
		public static final String COVERAGE = NS + "coverage";
	}

	public interface MAP {
		
		public static final String NS = "www.franz.com/allegrograph/dbimport#";
		// Mapping terms:
		public static final String DB_TO_RDF_MAPPING = NS + "DbToRDFMapping";
		public static final String JDBC_STRING = NS + "jdbcString";
		public static final String DATABASE_NAME = NS + "databaseName";
		public static final String HAS_TABLE = NS + "hasTable";		
		// Table terms:
		public static final String DB_TABLE = NS + "DbTable";
		public static final String TABLE_NAME = NS + "tableName";
		public static final String TABLE_TO_CLASS = NS + "tableToClass";
		public static final String PRIMARY_KEY_COLUMN = NS + "primaryKeyColumn";
		public static final String HAS_COLUMN = NS + "hasColumn";
		public static final String HAS_FOREIGN_KEY = NS + "foreignKey";
		// Column terms:
		public static final String DB_COLUMN = NS + "DbColumn";
		public static final String COLUMN_NAME = NS + "columnName";
		public static final String COLUMN_SQL_TYPE = NS + "columnSQLType";
		public static final String COLUMN_XSD_TYPE = NS + "columnXSDType";
		public static final String COLUMN_TO_PROPERTY = NS + "columnToProperty";
		// Foreign Key terms:
		public static final String FOREIGN_KEY = NS + "ForeignKey";
		public static final String JOIN_TO_TABLE = NS + "joinToTable";
		// MAYBE REPLACE BY ORDERED LIST OF COL NAMES???:
		public static final String FOREIGN_KEY_COLUMN = NS + "foreignKeyColumn";
		public static final String FOREIGN_KEY_TO_PROPERTY = NS + "foreignKeyToProperty";
		// Many-to-Many
		public static final String MANY_TO_MANY_JOIN_TABLE = NS + "ManyToManyJoinTable";
		public static final String FROM_TABLE = NS + "fromTable";
		public static final String TO_TABLE = NS + "toTable";
		public static final String FROM_FOREIGN_KEY = NS + "fromForeignKey";
		public static final String TO_FOREIGN_KEY = NS + "toForeignKey";
		public static final String JOIN_TABLE_TO_PROPERTY = NS + "joinTableToProperty";

	}

}
