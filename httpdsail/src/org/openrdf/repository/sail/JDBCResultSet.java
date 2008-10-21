package org.openrdf.repository.sail;

import java.util.List;

public class JDBCResultSet {
	
	private List<String> columnNames;
	private List<List<String>> stringTuples;
	
	public JDBCResultSet(List<String> columnNames, List<List<String>> stringTuples) {
		this.columnNames = columnNames;
		this.stringTuples = stringTuples;
	}

	// NOT YET IMPLEMENTED

}
