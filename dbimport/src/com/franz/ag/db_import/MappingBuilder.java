package com.franz.ag.db_import;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.franz.ag.db_import.RDFConstants.MAP;
import com.franz.ag.db_import.RDFConstants.RDF;
import com.franz.ag.db_import.RDFConstants.RDFS;
import com.franz.ag.db_import.RDFConstants.XSD;
import com.franz.ag.exceptions.NiceException;

public class MappingBuilder {
	
	private static Logger logger = Logger.getLogger(RDFExporter.class);
	
	private Connection connection;
	private String databaseName;
	private String catalogName = null;
	private String schemaName = null;
	private DatabaseMetaData metadata = null;
	private List<DBTable> tables = new ArrayList<DBTable>();
	private Set<String> excludedColumnNames = null;
	private String ontologyNamespace = null;
	private String dataNamespace = null;
	
	/**
	 * Constructor.  Takes a JDBC connection and a database name as input.
	 * @param conn
	 * @param database
	 */
	public MappingBuilder (Connection conn, String database) {
		this.connection = conn;
		this.databaseName = database;
		this.initialize();
	}

	private void initialize() {
		try {
			this.metadata = connection.getMetaData();
			validateDatabaseName();
		} catch (SQLException e) {
			throw new NiceException(e);
		}		
	}
	
	//----------------------------------------------------------------------------
	// Accessors
	//----------------------------------------------------------------------------
	
	/** 
	 * Return a list of tables belonging to this mapping.  Exclude any tables
	 * marked as 'excluded' unless 'includeExcludedTables' is set.
	 */
	public Collection<DBTable> getTables (boolean includeExcludedTables) {
		Collection<DBTable> tables = new ArrayList<DBTable>();
		for (DBTable t : this.tables) {
			if (!t.excludeTable || includeExcludedTables) {
				tables.add(t);
			}
		}
		return tables;
	}
	
	/** Return the open JDBC connection for this mapper. */ 
	public Connection getConnection () {return this.connection;}
	
	public String getDataNamespace () {return this.dataNamespace;}
	public String getOntologyNamespace () {return this.ontologyNamespace;}

	//----------------------------------------------------------------------------
	// 
	//----------------------------------------------------------------------------

	private void validateDatabaseName () throws SQLException {
		boolean foundDatabase = false;
		int count = 0;
		String catalogNames = "";
		ResultSet rs = null;
		try {
			rs = this.metadata.getCatalogs();
			while (rs.next()) {			
				String catalog = rs.getString(1);
				if (catalog.equals(this.databaseName)) {
					foundDatabase = true;
					this.catalogName = this.databaseName;
					break;
				}
				if (count < 10) {
					catalogNames += "\n   " + catalog;
				}
			}
		} finally {rs.close();}
		if (foundDatabase) return;
		String schemaNames = "";
		try {
			rs = this.metadata.getSchemas();
			while (rs.next()) {
				String schema = rs.getString(1);
				if (schema.equals(this.databaseName)) {
					foundDatabase = true;
					this.schemaName = this.databaseName;
					break;
				}
				if (count < 10) {
					schemaNames += "\n   " + schema;
				}
			}
		} finally {rs.close();}
		if (foundDatabase) return;
		if (catalogNames.length() > 0) {
			String catalogTerm = this.metadata.getCatalogTerm();
			throw new NiceException("Can't find a " + catalogTerm + " named '" + this.databaseName + "' at this connection.\n" +
					"Here are some of the possibilities: " + catalogNames);
		} else if (schemaNames.length() > 0) {
			System.out.println("SCHEMA NAMES: " + schemaNames);
			String schemaTerm = this.metadata.getSchemaTerm();
			throw new NiceException("Can't find a " + schemaTerm + " named '" + this.databaseName + "' at this connection.\n" +
					"Here are some of the possibilities: " + catalogNames);			
		} else {
			// this should never happen, unless the user has no access to any thing
			throw new NiceException("Can't find any catalogs or schemas at this connection."); 
		}
	
	}
	
	//------------------------------------------------------------------------------------------
	// Map objects
	//------------------------------------------------------------------------------------------
	
	public interface MapObject {
		public String getName ();
	}
	
	public class DBTable implements MapObject {
		private String catalog;
		private String schema;
		private String tableName;
		private String selfReferencingColumnName;
		private List<DBColumn> columns = new ArrayList<DBColumn>();
		private List<DBColumn> primaryKeyColumns = new ArrayList<DBColumn>();
		private List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
		private boolean isManyToManyTable = false;
		private ManyToManyRelationship manyToManyRelationship = null;
		// mapping attributes:
		private String rdfClass = null;
		private String classLabel = null;
		private boolean excludeTable = false;
		
		public DBTable(String cat, String schema, String name, String selfReferencingColumnName) {
			this.catalog = cat;
			this.schema = schema;
			this.tableName = name;
			this.selfReferencingColumnName = selfReferencingColumnName;
		}
		
		public String getName () {return this.tableName;}
		
		public String getRDFClass() {return this.rdfClass;}
		public void setRDFClass(String cls) {this.rdfClass = cls;}
		public String getClassLabel() {return this.classLabel;}
		public void setClassLabel(String label) {this.classLabel = label;} 
		public boolean includeTable () {return !this.excludeTable;}
		public void setIncludeTable (boolean setting) {this.excludeTable = !setting;}
		public boolean isManyToManyTable () {return this.isManyToManyTable;}
		public void setIsManyToManyTable (boolean setting) {
			this.isManyToManyTable = setting;
			if (!setting) this.manyToManyRelationship = null;
		}

		public List<DBColumn> getColumns () {return this.columns;}
		public List<DBColumn> getPrimaryKeyColumns () {return this.primaryKeyColumns;}
		public List<ForeignKey> getForeignKeys () {return this.foreignKeys;}
		
		public String toString() {return "|DBTable|" + this.tableName;}
		
		public DBColumn getColumn (String colName) {
			for (DBColumn col : this.getColumns()) {
				if (col.columnName.equals(colName)) return col;
			}
			throw new NiceException("Couldn't find column named '" + colName + "' in table " + this.getName());
		}
		
		private void detectManyToMany() {
			if (this.foreignKeys.size() != 2) return;
			int colCount = this.columns.size();
			int count = 0;
			for (ForeignKey fk : this.foreignKeys) count += fk.foreignKeyColumns.size();
			// if all of the columns correspond to foreign key columns, or all
			// but one does (that one presumably is the ID column), then guess that
			// this is a many-to-many relationship
			if ((count == colCount) || (count + 1 == colCount))
				this.isManyToManyTable = true;
		}
		
		/**
		 * If 'table' is a many-to-many joint table, retrieve its ManyToManyRelationship
		 * object.
		 */
		public ManyToManyRelationship getManyToManyRelationship () {
			if (this.isManyToManyTable) {
				if (this.manyToManyRelationship == null) {
					this.manyToManyRelationship = new ManyToManyRelationship(this);
				}
				return this.manyToManyRelationship;
			} else {
				return null;
			}
		}
	}
	
	public class DBColumn implements MapObject {
		private String columnName;
		private int typeInt;
		private String typeName;
		private String xsdDatatype;
		private int offset = -1;  // offset within database table
		// mapping attributes:
		private String rdfProperty = null;
		private String propertyLabel = null;
		private boolean excludeColumn = false;
		
		public DBColumn(String name, int sqlTypeInt, String sqlTypeName) {
			this.columnName = name;
			this.typeInt = sqlTypeInt;
			this.typeName = sqlTypeName;
			this.xsdDatatype = sqlTypeNameToXSDDatatype(sqlTypeName);
		}
		
		public String getName () {return this.columnName;}
		
		public String getRDFProperty() {return this.rdfProperty;}
		public void setRDFProperty(String property) {this.rdfProperty = property;}
		public String getPropertyLabel() {return this.propertyLabel;}
		public void setPropertyLabel(String label) {this.propertyLabel = label;} 
		public boolean includeColumn () {return !this.excludeColumn;}
		public void setIncludeColumn (boolean setting) {this.excludeColumn = !setting;}
		public int getTypeInt() {return this.typeInt;}
		public String getSQLType() {return this.typeName;}
		public String getXSDDatatype () {return this.xsdDatatype;}
		
		public String toString() {return "|DBColumn|" + this.columnName;}
	}
	
	public class ForeignKey implements MapObject {
		private DBTable foreignKeyTable = null;
		private DBTable primaryKeyTable = null;
		private List<DBColumn> foreignKeyColumns = new ArrayList<DBColumn>();
		private List<DBColumn> primaryKeyColumns = new ArrayList<DBColumn>();
		private String keyName = null;
		// mapping attributes:
		private String rdfProperty = null;
		private String propertyLabel = null;
		private boolean excludeColumn = false;
		
		public ForeignKey(String keyName) {
			this.keyName = keyName;			
		}
		
		public String getName () {return this.keyName;}
		
		public boolean includeColumn () {return !this.excludeColumn;}
		public void setIncludeColumn (boolean setting) {this.excludeColumn = !setting;}
		public String getRDFProperty() {return this.rdfProperty;}
		public void setRDFProperty(String uri) {this.rdfProperty = uri;}

		public DBTable getForeignKeyTable() {return this.foreignKeyTable;}
		public void setForeignKeyTable(DBTable table) {this.foreignKeyTable = table;}
		public DBTable getPrimaryKeyTable() {return this.primaryKeyTable;}
		public void setPrimaryKeyTable(DBTable table) {this.primaryKeyTable = table;}
		public void addColumnPair(DBColumn fk, DBColumn pk) {
			this.foreignKeyColumns.add(fk);
			this.primaryKeyColumns.add(pk);
		}
				
		public String toString () {return this.keyName;}
		
		public List<DBColumn> getForeignKeyColumns () {return this.foreignKeyColumns;}
	}
	
	public class ManyToManyRelationship {
		
		private DBTable joinTable;
		private DBTable fromTable;
		private ForeignKey fromKey;
		private DBTable toTable;
		private ForeignKey toKey;
		private String manyToManyRDFProperty = null;
		private String manyToManyPropertyLabel = null;
		
		/**
		 * Constructor.
		 * Must be called after the foreign keys for 'table' exist.
		 */
		public ManyToManyRelationship (DBTable table) {
			this.joinTable = table;
			// arbitrarily choose one of the two foreign keys on 'table'
			// to indicate the 'from' table, and the other to indicate the 'to'table:
			this.fromKey = table.getForeignKeys().get(0);
			this.toKey = table.getForeignKeys().get(1);
			this.fromTable = this.fromKey.getPrimaryKeyTable();
			this.toTable = this.toKey.getPrimaryKeyTable();
		}

		public ForeignKey getFromKey () {return this.fromKey;}
		public ForeignKey getToKey () {return this.toKey;}
		public DBTable getFromTable () {return this.fromTable;}
		public DBTable getToTable () {return this.toTable;}
		public String getManyToManyRDFProperty() {return this.manyToManyRDFProperty;}
		public void setManyToManyRDFProperty(String property) {this.manyToManyRDFProperty = property;}
		public String getManyToManyPropertyLabel() {return this.manyToManyPropertyLabel;}
		public void setManyToManyPropertyLabel(String label) {this.manyToManyPropertyLabel = label;} 
		
	}
	
	//-----------------------------------------------------------------------------------
	// End of Inner Classes
	//-----------------------------------------------------------------------------------
	
	private DBTable getTable (String tableName) {
		for (DBTable t : this.tables) {
			if (t.tableName.equals(tableName)) return t;
		}
		throw new NiceException("Couldn't find table named " + tableName);
	}
	
	private DBColumn getColumn (String colName, String tableName) {
		DBTable table = this.getTable(tableName);
		return table.getColumn(colName);
	}
	
	/**
	 * Find a foreign key in 'foreignTable' with name 'name'.  If not found,
	 * create one.
	 * Slightly tricky: If a table has two foreign keys, both of which reference
	 * the same primary key table, and if the keys do not have names, then their
	 * columns will be merged in this scheme.  Not sure how that could be avoided.
	 */
	private ForeignKey findOrCreateForeignKey(DBTable foreignTable, String name) {
		for (ForeignKey fk : foreignTable.foreignKeys) {
			if (name.equals(fk.keyName)) return fk;
		}
		ForeignKey fk = new ForeignKey(name);
		foreignTable.foreignKeys.add(fk);
		return fk;
	}


	//------------------------------------------------------------------------------------------
	// Creating the map objects
	//------------------------------------------------------------------------------------------
	
	private void importColumnMetadata ()  throws SQLException {
		ResultSet rs = null;
		for (DBTable tbl : this.tables) {
			System.out.println("Importing column data for table '" + tbl.tableName + "'");
			// foreach table, find the columns:
			try {
				rs = this.metadata.getColumns(tbl.catalog, tbl.schema, tbl.tableName, null);
				while (rs.next()) {
					String colName = rs.getString(4);
					if (this.excludedColumnNames.contains(colName)) continue;
					DBColumn col = new DBColumn(colName, rs.getInt(5), rs.getString(6));
					try { // we probably don't need the offset, but give it a try:
						col.offset = rs.getInt(17);
					} catch (Exception ex) {}
					tbl.columns.add(col);
				}
			} finally {rs.close();}
		}
	}
	
	/**
	 * Import all column metadata using a single cursor.
	 * Early results seem to show that this isn't any faster than the old way.
	 * If the tables are enumerated, and if they are much fewer than the total
	 * number of tables, then this method can be excruciatingly slow!!!
	 */
	private void fastimportColumnMetadata () throws SQLException {
		ResultSet rs = null;
		Map<String, DBTable> tableMap = new HashMap<String, DBTable>();
		for (DBTable t : tables) tableMap.put(t.tableName, t);
		// First, find all column information
		int dotCounter = 0;
		try {			
			rs = this.metadata.getColumns(this.catalogName, this.schemaName, null, null);
			while (rs.next()) {
				String tblName = rs.getString(3);
				DBTable tbl = tableMap.get(tblName);
				if (tbl == null) continue;
				DBColumn col = new DBColumn(rs.getString(4), rs.getInt(5), rs.getString(6));
				try { // we probably don't need the offset, but give it a try:
					col.offset = rs.getInt(17);
				} catch (Exception ex) {}
				tbl.columns.add(col);
				System.out.print(".");
				if (++dotCounter > 50) {System.out.println(); dotCounter = 0;}
			}
		} finally {rs.close();}
	}

	/**
	 * Read the metadata via the JDBC connection, and create class instances
	 * representing the tables, columns, and foreign keys. The mapping does
	 * not yet link to any classes or properties. 
	 */
	private void makeSkeletalMap () throws SQLException {
		ResultSet rs = null;
		ResultSet fkrs = null;
		// first, find the tables
		System.out.println("Found tables: " + this.tables);
		// First, find all column information:
		importColumnMetadata();
		// next, find the primary key columns:
		for (DBTable tbl : this.tables) {
			// find the primary key columns:
			try {
				rs = this.metadata.getPrimaryKeys(this.catalogName, this.schemaName, tbl.tableName);
				while (rs.next()) {
					String colName = rs.getString(4);
					DBColumn col = tbl.getColumn(colName);
					tbl.primaryKeyColumns.add(col);
				}
			} finally {
				rs.close();
			}
		}
		// we need to find foreign keys AFTER all table information about
		// columns has been retrieved:
		for (DBTable tbl : this.tables) {
			System.out.println("Importing foreign key data for table '" + tbl.tableName + "'");
			// next, find the foreign keys:
			try {
				fkrs = this.metadata.getImportedKeys(this.catalogName, this.schemaName, tbl.tableName);	
				while (fkrs.next()) {
					String pkTable = fkrs.getString(3);
					String pkColumn = fkrs.getString(4);
					String fkTable = fkrs.getString(7);
					String fkColumn = fkrs.getString(8);
					String keyName = fkrs.getString(12);
					if (Utils.isNullString(keyName))
						keyName = fkrs.getString(13);
					if (Utils.isNullString(keyName)) 
						keyName = fkTable + pkTable;
					// a key name is essential to make the columns get added correctly:
					ForeignKey fk = this.findOrCreateForeignKey(this.getTable(fkTable), keyName);
					fk.setForeignKeyTable(this.getTable(fkTable));
					fk.setPrimaryKeyTable(this.getTable(pkTable));
					fk.addColumnPair(this.getColumn(fkColumn, fkTable), this.getColumn(pkColumn, pkTable));
				}
			} finally {
				fkrs.close();
			}
			// next, see if its a many-to-many table:
			tbl.detectManyToMany();
		}
	}

	//------------------------------------------------------------------------------------------
	// Filling in the mapping to classes and properties
	//------------------------------------------------------------------------------------------
	
	public static String TABLE_COLUMN_SEPARATOR = "_";  // tried "." first; it didn't look that good - RMM
	public static String MANY_TO_MANY_SEPARATOR = "_to_";
	
	/**
	 * Return an encoding of 'name' that's legal within the local name of a URI.
	 */
	public static String legalize (String name) {
		name = name.replace(" ", "_");
		return name;
	}
	
	/**
	 * Add numeric suffixes to any foreign key URIs that conflict with other
	 * foreign key URIs. 
	 */
	private void uniquifyForeignKeyPredicates (DBTable table) {
		Set<String> taken = new HashSet<String>();
		for (ForeignKey fk : table.getForeignKeys()) {
			String uri = fk.getRDFProperty();
			String originalURI = uri;
			int counter = 2;
			while (taken.contains(uri)) {
				uri = originalURI + "_" + Integer.toString(counter++);
			}
			fk.setRDFProperty(uri);
			taken.add(uri);
		}
	}
	
	/**
	 * Compute default types and predicates (URIs) for classes and
	 * properties.  Also, compute default rdfs labels.
	 * Improvements can be made by overwriting them using a DBIimport wizard.
	 */
	private void makeMappingToRDF () {
		for (DBTable t : this.tables) {
			if (t.isManyToManyTable) {
				ManyToManyRelationship m2m = t.getManyToManyRelationship();
				String label = m2m.fromTable.getName() + MANY_TO_MANY_SEPARATOR + m2m.toTable.getName();
				String propertyURI = this.ontologyNamespace + legalize(label);
				m2m.setManyToManyRDFProperty(propertyURI);
				m2m.setManyToManyPropertyLabel(label);
				continue;
			}
			String legalTableName = legalize(t.tableName);
			String className = this.ontologyNamespace + legalTableName;
			t.rdfClass = className;
			t.classLabel = legalTableName;
			String columnPrefix = this.ontologyNamespace + legalTableName.toLowerCase();
			for (DBColumn col : t.getColumns()) {
				String legalColumnName = legalize(col.columnName);
				String propertyName = columnPrefix + TABLE_COLUMN_SEPARATOR + legalColumnName;				
				col.rdfProperty = propertyName;
				col.propertyLabel = legalColumnName;
			}
			for (ForeignKey fk : t.getForeignKeys()) {
				// default name for foreign key concatenates the table name with
				// the table name of the table that it points to:
				String columnName = fk.getPrimaryKeyTable().getName();
				String legalColumnName = legalize(columnName);
				String propertyName = columnPrefix + TABLE_COLUMN_SEPARATOR + legalColumnName;				
				fk.rdfProperty = propertyName;
				fk.propertyLabel = legalColumnName;
			}
			uniquifyForeignKeyPredicates(t);
		}
		
	}
	
	
	//------------------------------------------------------------------------------------------
	// Generating RDF triples that define a mapping
	//------------------------------------------------------------------------------------------
	
	private void exportColumn (DBColumn column, RDFExporter exp) {
		exp.exportTriple(column, RDF.TYPE, MAP.DB_COLUMN, null);
		exp.exportTriple(column, MAP.COLUMN_TO_PROPERTY, column.getRDFProperty(), null);
		exp.exportTriple(column.getRDFProperty(), RDF.TYPE, RDF.PROPERTY, null);
		exp.exportTriple(column.getRDFProperty(), RDFS.LABEL, column.getPropertyLabel(), XSD.STRING);
	}

	private void exportTable (DBTable table, RDFExporter exp) {
		exp.exportTriple(table, RDF.TYPE, MAP.DB_TABLE, null);
		exp.exportTriple(table, MAP.TABLE_TO_CLASS, table.getRDFClass(), null);
		exp.exportTriple(table.getRDFClass(), RDF.TYPE, RDFS.CLASS, null);
		exp.exportTriple(table.getRDFClass(), RDFS.LABEL, table.getClassLabel(), XSD.STRING);
		for (DBColumn col : table.getColumns()) {
			exp.exportTriple(table, MAP.HAS_COLUMN, col, null);
			exportColumn(col, exp);
		}
		for (DBColumn col : table.getPrimaryKeyColumns()) {
			exp.exportTriple(table, MAP.PRIMARY_KEY_COLUMN, col, null);
		}
		// TODO: FIGURE OUT THE FORMAT FOR FOREIGN KEYS:
		for (ForeignKey fkey : table.getForeignKeys()) {
			// NOT CLEAR IF WE NEED THIS, OR MAYBE ITS REDUNDANT???
			DBTable fkTable = fkey.getForeignKeyTable();
			DBTable pkTable = fkey.getPrimaryKeyTable();
			if (fkTable != table) { 
				//continue; // avoid printing out the key twice
				System.out.println("FOREIGN KEY NOT BACKPOINTING TO FOREIGN TABLE");
			}
			exp.exportTriple(table, MAP.HAS_FOREIGN_KEY, fkey, null);
			exp.exportTriple(fkey, RDF.TYPE, MAP.FOREIGN_KEY, null);
			exp.exportTriple(fkey, MAP.JOIN_TO_TABLE, pkTable, null);
			for (DBColumn col : fkey.getForeignKeyColumns()) {
				// THESE ARE UNORDERED; NOT CLEAR IF THAT WORKS WHEN ASSEMBLING KEYS:
				exp.exportTriple(fkey, MAP.FOREIGN_KEY_COLUMN, col, null);
			}
			if (fkey.getRDFProperty() != null) {
				exp.exportTriple(fkey, MAP.FOREIGN_KEY_TO_PROPERTY, fkey.getRDFProperty(), XSD.STRING);
			}
		}
		
//		FOREIGN_KEY = NS + "ForeignKey";
//		public static final String JOIN_TO_TABLE = NS + "joinToTable";
//		public static final String FOREIGN_KEY_TO_PROPER
	}
	

	//------------------------------------------------------------------------------------------
	// External Entry
	//------------------------------------------------------------------------------------------
	/**
	 * Return a list of all table names.
	 * Side-effect: Fill the field 'this.tables' with DBTables
	 */
	public List<String> retrieveTableNames (Collection<String> enumeratedTableNames) throws SQLException {
		List<String> tableNames = new ArrayList<String>();
		if (this.tables.size() > 0) return tableNames;
		// if table names are enumerated, compile into a dictionary:
		Set<String> enumeratedTablesDict = null;
		if (enumeratedTableNames != null) {
			enumeratedTablesDict = new HashSet<String>(); 
			if (enumeratedTableNames != null)
				enumeratedTablesDict.addAll(enumeratedTableNames);
		}
		ResultSet rs = null;
		String[] tableTypes = {"TABLE"};
		try {
			rs = this.metadata.getTables(this.catalogName, this.schemaName, null, tableTypes);
			while (rs.next()) {
				ResultSetMetaData md = rs.getMetaData();			
				String cat = rs.getString(1);
				String schema = rs.getString(2);
				String table = rs.getString(3);
				if ((enumeratedTablesDict != null) && ! enumeratedTablesDict.contains(table)) continue;				
				String identityColumn = null;
				if (md.getColumnCount() >= 9) {
					identityColumn = rs.getString(9);
				}
				DBTable tbl = new DBTable(cat, schema, table, identityColumn);
				this.tables.add(tbl);
				tableNames.add(tbl.getName());
			}
		} finally {rs.close();}
		if (this.tables.size() == 0) {
			throw new NiceException("Could not find any tables for the database " + this.databaseName);
		}
		return tableNames;
	}

	/**
	 * Create internal mapping data structures for the connected database.
	 * These structures can be edited by the Wizard, saved in RDF format,
	 * or used to generate RDF triples from the database.
	 */
	public void makeDefaultMap (String ontologyNamespace, String dataNamespace, Collection<String> enumeratedTableNames,
			Collection<String> excludedColumnNames) throws SQLException {
		this.ontologyNamespace = ontologyNamespace;
		this.dataNamespace = dataNamespace;
		this.excludedColumnNames = new HashSet<String>();
		if (excludedColumnNames != null)
			this.excludedColumnNames.addAll(excludedColumnNames);
		this.retrieveTableNames(enumeratedTableNames);
		this.makeSkeletalMap();
		this.makeMappingToRDF();
	}
	
	public void exportDBMapping(RDFExporter exporter) {
		for (DBTable t : this.tables) {
			this.exportTable(t, exporter);
		}
		exporter.writeTriples();
	}
	
	//-----------------------------------------------------------------------------------------------------------
	// Type conversion
	//-----------------------------------------------------------------------------------------------------------
	
	private static String[] SQL_TO_XSD_PAIRS = {
		"CHAR", XSD.STRING, 
		"VARCHAR", XSD.STRING, 
		"VARCHAR2", XSD.STRING, 
		"LONGVAR", XSD.STRING, 
		"CLOB", XSD.STRING, 		
		"TEXT", XSD.STRING, 		
		"MEDIUMTEXT", XSD.STRING, 		
		"NUMBER", XSD.NUMBER, 
		"DECIMAL", XSD.NUMBER, 
		"TINYINT", XSD.INTEGER, 
		"SMALLINT", XSD.INTEGER, 
		"INT", XSD.INT, 		
		"INTEGER", XSD.INTEGER, 		
		"BIGINT", XSD.LONG,
		"FLOAT", XSD.FLOAT,
		"DATE", XSD.DATE,
		"TIME", XSD.TIME,
		"BOOLEAN", XSD.BOOLEAN,
		"BLOB", XSD.BASE64BINARY
	};
	
	private static Map<String, String> SQL_TO_XSD = new HashMap<String, String>();
	static {
		// Compute the SQL_TO_XSD mapping:
		for (int i = 0; i < SQL_TO_XSD_PAIRS.length;) {
			SQL_TO_XSD.put(SQL_TO_XSD_PAIRS[i++], SQL_TO_XSD_PAIRS[i++]);
		}
	}

	/**
	 * Translate an integer JDBC/SQL type into an XSD URI.
	 */
	public static String sqlTypeNameToXSDDatatype (String sqlType) {
		String xsdType = SQL_TO_XSD.get(sqlType);
		if (xsdType == null) {
			logger.warn("Failed to convert SQL type " + sqlType + " during database import");
			return XSD.STRING;
		}
		return xsdType;
	}

	
}
