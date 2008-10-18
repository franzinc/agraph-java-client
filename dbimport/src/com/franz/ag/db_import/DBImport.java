package com.franz.ag.db_import;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.franz.ag.db_import.jdbc.ConnectionFactory;
import com.franz.ag.exceptions.NiceException;
import com.franz.ag.exceptions.UnimplementedMethodException;

public class DBImport {
	
	private String[] executionArgs = null;
	private String userName = null;
	private String password = null;
	private String brand = null; // "ORACLE", "MySQL"
	private String jdbcDriverClass = null;
	private String jdbcDSC = null;
	private Connection conn = null;
	private String database = null;
	private String connectedToDatabase = null;
	private String ontologyNamespace = null;
	private String dataNamespace = null;
	private String dbMappingFile = null;
	private String dbDataFile = null;
	private String ontologyPrefix = "ont";
	private String dataPrefix = "data";
	private Collection<String> enumeratedTableNames = null;
    private Collection<String> excludedColumnNames = null;
	private String exportTargetType = "ALLEGRO_GRAPH";
	private String allegroGraphHost = null;
	private String allegroGraphDBName = null;
	private String allegroGraphDBDirectory = null;
	
	/**
	 * Constructor
	 */
	public DBImport (String userName, String password, String[] args) {
		this.userName = userName;
		this.password = password;
		this.executionArgs = args;
	}
	
	private MappingBuilder builder = null;
	
	private enum DBImportAction {CREATE_DEFAULT_MAPPING, LOAD_MAPPING, SAVE_MAPPING, UPLOAD_DATA, PRINT_TABLES}
	
	private DBImportAction parseDBImportAction(String action) {
		action = action.toLowerCase().replace("_", "");
		if ("createdefaultmapping".equals(action)) return DBImportAction.CREATE_DEFAULT_MAPPING;
		else if ("createmapping".equals(action)) return DBImportAction.CREATE_DEFAULT_MAPPING;
		else if ("loadmapping".equals(action)) return DBImportAction.LOAD_MAPPING;
		else if ("savemapping".equals(action)) return DBImportAction.SAVE_MAPPING;
		else if ("uploaddata".equals(action)) return DBImportAction.UPLOAD_DATA;
		else if ("printtables".equals(action)) return DBImportAction.PRINT_TABLES;
		else {
			throw new NiceException("Unrecognized DB Import action.  Options are:\n" +
					"   CREATE_DEFAULT_MAPPING, LOAD_MAPPING, SAVE_MAPPING, UPLOAD_DATA, and PRINT_TABLES");
		}
	}

	/** 
	 * Insure that a JDBC connection is open.  If the database parameter has changed,
	 * close the current connection and open a new one.
	 */
	private void openConnection () throws SQLException {
		if ((this.conn != null) && (this.connectedToDatabase != this.database)) {
			this.conn.close();
			this.conn = null;
		}
		if (this.conn == null) {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setJDBCDriverClass(this.jdbcDriverClass);
			factory.setJDBCDSN(this.jdbcDSC);
			factory.setUserName(userName);
			factory.setPassword(password);	
			this.conn = factory.getConnection();
			if (this.brand.equalsIgnoreCase("MySQL")) {
				Statement stmt = this.conn.createStatement();
				System.out.println("CONNECT STATEMENT:" + "USE " + this.database);
				stmt.execute("USE " + this.database);
			}
			this.connectedToDatabase = this.database;
		}
	}
	
	private void checkForBuilder (String importAction) {
		if (this.builder == null) {
			throw new NiceException("Can't perform action " + importAction + " before executing a\n" +
					"'CREATE_DEFAULT_MAPPING' or 'LOAD_MAPPING' action.");
		}
	}
	
	private MappingBuilder createDefaultMapping () throws SQLException {
		this.openConnection();
		MappingBuilder builder = new MappingBuilder(this.conn, this.database);
		builder.makeDefaultMap(this.ontologyNamespace, this.dataNamespace, this.enumeratedTableNames, this.excludedColumnNames);
		return builder;
	}
	
	private MappingBuilder loadMapping () throws SQLException {
		this.openConnection();
		throw new UnimplementedMethodException("loadMapping");
	}
	
	private void saveMapping () throws SQLException {
		this.openConnection();
		RDFExporter mappingExporter = new RDFExporter(this.exportTargetType, this.executionArgs);
		mappingExporter.addNamespacePrefix(this.ontologyNamespace, "ont");
		mappingExporter.setBaseNamespace(this.ontologyNamespace);
		mappingExporter.setOutputFile(this.dbMappingFile);
		builder.exportDBMapping(mappingExporter);
	}
	
	private void uploadData () throws SQLException {
		this.openConnection();
		RDFExporter dataExporter = new RDFExporter(this.exportTargetType, this.executionArgs);
		dataExporter.addNamespacePrefix(this.ontologyNamespace, this.ontologyPrefix);
		dataExporter.addNamespacePrefix(this.dataNamespace, this.dataPrefix);
		dataExporter.setBaseNamespace(this.dataNamespace);
		dataExporter.setOutputFile(this.dbDataFile);		
		dataExporter.setAllegroGraphHost(this.allegroGraphHost);
		dataExporter.setAllegroGraphDBName(this.allegroGraphDBName);
		dataExporter.setAllegroGraphDBDirectory(this.allegroGraphDBDirectory);		
		UploadTriples.uploadTriples(builder, dataExporter);
	}
	
	/**
	 * Print tables in a format which can easily be pasted into
	 * Java code as a list
	 */
	private void printTables (boolean includeDoubleQuotesAndCommas) throws SQLException {
		this.openConnection();
		MappingBuilder builder = new MappingBuilder(this.conn, this.database);
		String output = "";
		int count = 0;
		for (String t : builder.retrieveTableNames(null)) {
			if (includeDoubleQuotesAndCommas) t = "\"" + t + "\", ";
			output += t;
			count++;
			if (count >= 1) {
				output += "\n";
				count = 0;
			}
		}
		System.out.println("TABLES:\n" + output);
	}
	
	public void doImportAction(String importAction) {
		DBImportAction act = parseDBImportAction(importAction);
		try {
			switch (act) {
			case CREATE_DEFAULT_MAPPING:
				this.builder = this.createDefaultMapping();
				break;
			case LOAD_MAPPING:
				this.builder = this.loadMapping();
				break;
			case SAVE_MAPPING:
				this.checkForBuilder(importAction);
				this.saveMapping();
				break;
			case UPLOAD_DATA:
				this.checkForBuilder(importAction);
				this.uploadData();
				break;
			case PRINT_TABLES:
				this.printTables(true);
				break;
			}
		} catch (SQLException ex) {
			throw new NiceException("Failure executing DB Iimport action " + importAction, ex);
		}
	}


	/**
	 * Parse the configuration arguments, start up the
	 * @param hostName
	 * @param databaseName
	 * @param dbDirectory
	 * @param args
	 * @return  a running AllegroGraph server
	 * @throws AllegroGraphException
	 * @throws IOException
	 */
	// NOT YET IMPLEMENTED
	public void parseInputArguments (String[] args) {
	
//		// Scan startup parameters
//		for (int i = 0; i < args.length;) {
//			String flag = (args[i]);
//			if (flag.equals("-p"))
//				port = Integer.parseInt(args[++i]);
//			else if (flag.equals("-h"))
//				host = args[++i];
//			else if (flag.equals("-d"))
//				dbDirectory = args[++i];
//			else if (flag.equals("-n"))
//				dbName = args[++i];
//		//	else if (flag.equals("-r"))
//		//		rdfFile = args[++i];
//			else if (flag.equals("-t"))
//				tripleFile = args[++i];
//			else if (flag.equals("-w"))
//				exitWait = Integer.parseInt(args[++i]);
//			else if (flag.equals("-z"))
//				this.debug = 1;
//			else if (flag.equals("-zz"))
//				this.debug = 2;
//			else if (flag.equals("-x"))
//				startServer = true;
//			else if (flag.equals("-q"))
//				this.quiet = true;
//			else if (flag.equals("-l"))  // 'l' for launch server
//				agJavaServerPath = args[++i];
//			i++;
		}

	
	/**
	 * 
	 * @param args
	 * 
	 * Sample call to invoke:
	 * 
	 * java DBImport -j "jdbc:mysql:localhost:root:root" -d MyDatabase
	 */
	public static void test1 (String[] args) throws Exception {
		
		String userName = "root";
		String password = "root";
		
		DBImport imp = new DBImport(userName, password, args);
		imp.brand = "MySQL";
		imp.jdbcDriverClass = "com.mysql.jdbc.Driver";
		imp.jdbcDSC = "jdbc:mysql://localhost:3306/mysql";
		imp.database = "iswc";
		imp.ontologyNamespace = "www.franz.com/allegrograph/my_ontology#";
		imp.ontologyPrefix = "ont";
		imp.dataPrefix = "data";
		// now do some actions:
		imp.doImportAction("PRINT_TABLES");
		imp.doImportAction("CREATE_DEFAULT_MAPPING");
		imp.dbMappingFile = "/Users/bmacgregor/Desktop/db_import_dump/test1/mapping.n3";
		imp.exportTargetType = "N3";
		imp.doImportAction("SAVE_MAPPING");
		imp.dbDataFile = "/Users/bmacgregor/Desktop/db_import_dump/test1/data.n3";
		imp.dataNamespace = "www.franz.com/allegrograph/my_data#";
		imp.doImportAction("UPLOAD_DATA");
		imp.exportTargetType = "ALLEGRO_GRAPH";
		imp.allegroGraphHost = "localhost";
		imp.allegroGraphDBName = "import_test";
		imp.allegroGraphDBDirectory = "/Users/bmacgregor/Desktop/AGFolder";
		imp.doImportAction("UPLOAD_DATA");
	}
	
	private static List<String> listIncludedTables (int limit) {
		String[] tableNames = {
			"C_483_CITIATION", 
			"C_ADDRESS", 
			"C_COMPOUND", 
			"C_COMPOUND_CATEGORY", 
			"C_COMPOUND_CAT_CLASS", 
			"C_COMPOUND_PATENT", 
			"C_COMPOUND_TO_FORMULA", 
			"C_DISEASE", 
			"C_DIS_UNSTRUCT_DETAILS", 
			"C_DOCUMENTS_DICT", 
			"C_EVENT", 
			"C_FORMULATION", 
			"C_FORMULA_PATENT", 
			"C_FORMULA_TO_PRODUCT", 
			"C_LOCATION", 
			"C_LU_COUNTRIES", 
			"C_PARTY", 
			"C_PATENT", 
			"C_PATENT_SUBJ", 
			"C_PRD_UNSTRUCT_DETAILS", 
			"C_PRODUCT", 
			"C_PRODUCT_CATEGORY", 
			"C_PRODUCT_CAT_CLASS", 
			"C_PRODUCT_TO_DISEASE", 
			"C_PTY_UNSTRUCT_DETAILS", 
			"C_RECALL", 
			"C_REGULATORY_FINE", 
			"C_REL_COMP_CAT_HIER", 
			"C_REL_PARTY_ADDRESS", 
			"C_REL_PROD_CAT_HIER", 
			"C_REL_PROD_FORMULA_MFG", 
			"C_REL_USER_PREFERENCES", 
			"C_SHIPMENT", 
			"C_SYMPTOM", 
			"C_SYMPTOM_TO_DISEASE", 
			"C_USER", 
			"C_USER_PREFERENCE", 
			"UNSTR_SOURCE_NAMES", 
			"V_ALTNAMES_PARTY_PERSON", 
			"V_CITATION_PARTY_PERSON", 
			"V_PARTY_ENTITY_PERSON",
		};
		List<String> result = Arrays.asList(tableNames);
		if ((limit > 0) && (limit < result.size())) {
			//return result.subList(0, limit);
			System.out.println("Can't place limit, because foreign keys may reference non-imported tables.");
			return result;
		}
		else
			return result;
	}
	
	private static List<String> listExcludedColumns () {
		String[] colNames = {
				"CREATOR", "CREATE_DATE", "UPDATED_BY", "LAST_UPDATE_DATE", "CONSOLIDATION_IND", "LAST_ROWID_SYSTEM", "DIRTY_IND",
				"DELETED_DATE", "DELETED-BY",
		};	
		return Arrays.asList(colNames);
	}
	
	public static void test2 (String[] args) throws Exception {
		
		String userName = "semantic_ors";
		String password = "!!cmx!!";
		DBImport imp = new DBImport(userName, password, args);
		imp.brand = "ORACLE";
		imp.jdbcDriverClass = "oracle.jdbc.OracleDriver";
		imp.jdbcDSC = "jdbc:oracle:thin:@octo01:1521:orcl";
		imp.database = "semantic_ors".toUpperCase();
		imp.ontologyNamespace = "www.siperian.com/map#";
		imp.dataNamespace = "www.siperian.com/smh#";		
		imp.ontologyPrefix = "ont";
		imp.dataPrefix = "data";
		// now do some actions:
		//imp.doImportAction("PRINT_TABLES");
		imp.enumeratedTableNames = listIncludedTables(0);
		imp.excludedColumnNames = listExcludedColumns();
		imp.doImportAction("CREATE_DEFAULT_MAPPING");
		imp.dbMappingFile = "/Users/bmacgregor/Desktop/db_import_dump/hub/mapping.nt";
		imp.exportTargetType = "NTRIPLES";
		imp.doImportAction("SAVE_MAPPING");
		imp.dbDataFile = "/Users/bmacgregor/Desktop/db_import_dump/hub/data.nt";
		imp.doImportAction("UPLOAD_DATA");
		imp.exportTargetType = "ALLEGRO_GRAPH";
		imp.allegroGraphHost = "localhost";
		imp.allegroGraphDBName = "import_hub";
		imp.allegroGraphDBDirectory = "/Users/bmacgregor/Desktop/AGFolder";
		imp.doImportAction("UPLOAD_DATA");
	}
	
	
	/**
	 * 
	 * @param args
	 * 
	 * Sample call to invoke:
	 * 
	 * java DBImport -j "jdbc:mysql:localhost:root:root" -d MyDatabase
	 */
	public static void main (String[] args) throws Exception {
		int choice = 2;
		switch(choice) {
		case 1: test1(args); break;
		case 2: test2(args); break;
		}
	}
	
//	private String allegroGraphHost = null;
//	private String allegroGraphDBName = null;
//	private String allegroGraphDBDirectory = null;

}
