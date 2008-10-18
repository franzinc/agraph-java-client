package com.franz.ag.db_import;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.franz.ag.db_import.MappingBuilder.DBColumn;
import com.franz.ag.db_import.MappingBuilder.DBTable;
import com.franz.ag.db_import.MappingBuilder.ForeignKey;
import com.franz.ag.db_import.MappingBuilder.ManyToManyRelationship;
import com.franz.ag.exceptions.UnimplementedMethodException;

public class UploadTriples {
	
	private static Logger logger = Logger.getLogger(UploadTriples.class);
	
	private MappingBuilder mapper;
	RDFExporter exporter;
	
	public static final String DELIMITER_IN_KEYS = "_"; 
	
	private UploadTriples(MappingBuilder mapper, RDFExporter exporter) {
		this.mapper = mapper;
		this.exporter = exporter;
	}
	
	/**
	 * Return a list of columns for 'table' that either not marked as excluded or that are
	 * referenced in the primary key or in a non-excluded foreign key.
	 */
	private List<DBColumn> selectQueryColumns (DBTable table) {
		List<DBColumn> columns = new ArrayList<DBColumn>();
		for (DBColumn col : table.getColumns()) {
			if (col.includeColumn()) columns.add(col);		
		}
		for (DBColumn col : table.getPrimaryKeyColumns()) {
			if (!(columns.contains(col))) columns.add(col);
		}
		for (ForeignKey fk : table.getForeignKeys()) {
			if (fk.includeColumn()) {
				for (DBColumn col : fk.getForeignKeyColumns()) {
					if (!(columns.contains(col))) columns.add(col);
				}
			}
		}
		return columns;
	}
	
	/**
	 * If 'name' contains a blank, wrap it in whatever kind of quotes are
	 * needed to make a reference to it legal within a SQL query.
	 */
	private String legalSQLColumnName (String name) {
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			// TODO: WE DON'T RECALL THE ESCAPE FOR 
			if (c == ' ') {
				throw new UnimplementedMethodException("legalSQLColumnName.  Need to wrap escape characters around the column name '" + name +
						"' to embed it in a SQL query");
			}
		}
		return name;
	}
	
	/**
	 * Construct an SQL query to retrieve the columns in 'table' needed
	 * to synthesize column values, and primary key and foreign key URIs
	 */
	private String formulateTableQuery (DBTable table, List<DBColumn> queryColumns) {
		String query = "SELECT ";
		boolean firstTime = true;
		for (DBColumn col : queryColumns) {
			if (!firstTime) query += ", ";
			query += "t." + legalSQLColumnName(col.getName());
			firstTime = false;
		}
		// NOT SURE WHY WE NEED TO CONCATENATE THE DATABASE NAME HERE: MAYBE
		// NEED TO EXECUTE A 'USE' COMMAND FIRST:
		//query += "\nFROM " + databasename + "." + table.getName() + " t";
		query += "\nFROM " + table.getName() + " t";
		return query;
	}
	
	private String getLocalNameForPoundSign (String uri) {
		int pos = uri.indexOf('#');
		return uri.substring(pos + 1, uri.length());
	}
	
	private class KeyTemplate {
		String tableURI;
		String predicateURI = null; // used by foreign keys; not by primary key
		int[] columnOffsets;
		
		/** Constructor */
		private KeyTemplate(DBTable table, int[] columnOffsets) {
			String className = getLocalNameForPoundSign(table.getRDFClass());
			this.tableURI = mapper.getDataNamespace() + className;
			this.columnOffsets = columnOffsets;
		}
		
		private String getPredicateURI () {return this.predicateURI;}
		private void setPredicateURI(String uri) {this.predicateURI = uri;}
		
		public String toString () {
			String offsets = "[";
			for (int off : this.columnOffsets) offsets += "" + off + ",";
			offsets += "]";
			return "|KeyTemplate|" + this.tableURI + offsets + this.predicateURI;
		}
	}
	
	
	private int[] makeColumnOffsets (List<DBColumn> keyCols, DBTable table, List<DBColumn> queryColumns) {
		int[] columnOffsets = new int[keyCols.size()];
		for (int i = 0; i < keyCols.size(); i++) {
			DBColumn kc = keyCols.get(i);
			for (int j = 0; j < queryColumns.size(); j++) {
				if (kc == queryColumns.get(j)) {
					columnOffsets[i] = j;
				}
			}
		}
		return columnOffsets;
	}
	
	private KeyTemplate makeForeignKeyTemplate(List<DBColumn> keyCols, DBTable table, DBTable primaryKeyTable, List<DBColumn> queryColumns) {
		int[] columnOffsets = makeColumnOffsets(keyCols, table, queryColumns);
		KeyTemplate template = new KeyTemplate(primaryKeyTable , columnOffsets);
		return template;
	}
	
	private KeyTemplate makePrimaryKeyTemplate(DBTable table, List<DBColumn> queryColumns) {
		int[] columnOffsets = makeColumnOffsets(table.getPrimaryKeyColumns(), table, queryColumns);
		KeyTemplate template = new KeyTemplate(table, columnOffsets);
		return template;
	}
	
	private String synthesizeURIForKey (KeyTemplate keyTemplate, String[] rowStrings) {
		String uri = keyTemplate.tableURI;
		for (int i = 0; i < keyTemplate.columnOffsets.length; i++) {
			int offset = keyTemplate.columnOffsets[i];
			String keyVal = rowStrings[offset];
//			// TEMPORARY
//			String vals = "";
//			for (String s : rowStrings) vals += s + ",";
//			System.out.println("OFFSET " + offset + " " + rowStrings[offset] + "  " + vals);
//			// END TEMPORARY
			if (keyVal != null) {
				// foreign key is not missing:
				uri += DELIMITER_IN_KEYS + MappingBuilder.legalize(keyVal.trim());
			}
		}
		return uri;
	}
	
	private void importManyToManyTriples(ManyToManyRelationship m2m, List<KeyTemplate> fkTemplates, ResultSet rs) throws SQLException {
		KeyTemplate fromTemplate = null;
		KeyTemplate toTemplate = null;
		for (KeyTemplate template : fkTemplates) {
			if (template.tableURI == m2m.getFromTable().getRDFClass())
				fromTemplate = template;
			else
				toTemplate = template;
		}
		// logic borrowed from 'importTriplesInTable':
		int colCount = rs.getMetaData().getColumnCount();		
		String[] rowStrings = new String[colCount];
		try {
			while(rs.next()) {
				// record all row values in the 'rowStrings' array; this compensates for inexcusable
				// stupidities like MSAccess which can't access resultset columns out of order:
				for (int i = 0; i < colCount; i++) {
					rowStrings[i] = rs.getString(i + 1);
				}
				// resume virgin logic:
				String fromURI = synthesizeURIForKey(fromTemplate, rowStrings);
				String predicateURI = m2m.getManyToManyRDFProperty();
				String toURI = synthesizeURIForKey(toTemplate, rowStrings);
				this.exporter.exportTriple(fromURI, predicateURI, toURI, null);
			}
		} finally {rs.close();}
	}
	
	private void importTriplesInTable(DBTable table) throws SQLException {
		System.out.println("Importing data from table " + table.getName());
		List<DBColumn> queryColumns = selectQueryColumns(table);		
		String sqlQuery = formulateTableQuery(table, queryColumns);
		Statement stmt = mapper.getConnection().createStatement();
		ResultSet rs = stmt.executeQuery(sqlQuery);
		KeyTemplate pkTemplate = makePrimaryKeyTemplate(table, queryColumns);
		List<DBColumn> includedColumnsInOrder = new ArrayList<DBColumn>();
		// record the initial columns in 'queryColumns' that are plain old columns:
		for (DBColumn col : table.getColumns()) {
			if (col.includeColumn()) includedColumnsInOrder.add(col);
		}
		List<KeyTemplate> fkTemplates = new ArrayList<KeyTemplate>();
		for (ForeignKey fk : table.getForeignKeys()) {
			if (fk.includeColumn()) {
				KeyTemplate template = makeForeignKeyTemplate(fk.getForeignKeyColumns(), 
						table, fk.getPrimaryKeyTable(), queryColumns);
				template.setPredicateURI(fk.getRDFProperty());
				fkTemplates.add(template);
			}
		}
		if (table.isManyToManyTable()) {
			this.importManyToManyTriples(table.getManyToManyRelationship(), fkTemplates, rs);
		} else try {
			//System.out.println("INCLUDED COLUMNS: " + includedColumnsInOrder);////
			int colCount = rs.getMetaData().getColumnCount();
			String[] rowStrings = new String[colCount];
			while(rs.next()) {
				// record all row values in the 'rowStrings' array; this compensates for inexcusable
				// stupidities like MSAccess which can't access resultset columns out of order:
				for (int i = 0; i < colCount; i++) {
					rowStrings[i] = rs.getString(i + 1);
				}
				// generate a triple for each non-excluded column:
				String rowURI = synthesizeURIForKey(pkTemplate, rowStrings);
				int i = 0;
				for (DBColumn col : includedColumnsInOrder) {
					String predicateURI = col.getRDFProperty();
					String value = rowStrings[i++];
					if (value == null) continue; // skip null values in table
					this.exporter.exportTriple(rowURI, predicateURI, value, col.getXSDDatatype());
				}
				// next generate triples for foreign keys; the templates
				// record all information needed:
				for (KeyTemplate template : fkTemplates) {
					String predicateURI = template.getPredicateURI();
					String valueURI = synthesizeURIForKey(template, rowStrings);
					if (valueURI != null) {
						// foreign key is not missing:
						this.exporter.exportTriple(rowURI, predicateURI, valueURI, null);
					}
				}
			}
		} finally {rs.close();}
	}
	
	//----------------------------------------------------------------------------------------
	// Entry Points
	//----------------------------------------------------------------------------------------
	
	/**
	 * Import data from the database connected to via 'mapper',
	 * and write it to the target specified in 'exporter'.
	 * @param mapper
	 * @param exporter
	 * 
	 * Strategy:
	 *    For each table in 'mapper.getTables()'
	 *    For each row in table
	 */
	public static void uploadTriples (MappingBuilder mapper, RDFExporter exporter) throws SQLException {
		UploadTriples uploader = new UploadTriples(mapper, exporter);
		for (DBTable table : mapper.getTables(false)) {
			try {
				uploader.importTriplesInTable(table);
			} catch (SQLException e) {
				String errmsg = Utils.isNullString(e.getMessage()) ? e.toString() : e.getMessage();
				System.out.println("Failed to upload triples from table '" + table.getName() + "' because " + e.getMessage());
			}
		}
		exporter.writeTriples();
	}

}
