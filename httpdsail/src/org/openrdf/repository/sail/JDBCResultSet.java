package org.openrdf.repository.sail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openrdf.model.Value;

import franz.exceptions.JDBCException;
import franz.exceptions.UnimplementedMethodException;

public class JDBCResultSet {
	
	private List<String> columnNames;
	private List<List<String>> stringTuples;
	private int cursor = 0;
    private int tuple_width = -1;
    private int low_index = -1;
    private int high_index = -1;
    private List<String> current_strings_row = null;
    private List<Value> current_terms_row = null;
    private boolean isQuads = false;

    public static int COUNTING_STARTS_AT = 0;   // real JDBC starts at one; we could change this to 1 if users want
    public static List<String> STATEMENT_COLUMN_NAMES = new ArrayList<String>();
    
    static {
    	STATEMENT_COLUMN_NAMES.addAll(Arrays.asList(new String[]{"subject", "predicate", "object", "context"}));
    }
    
    /** Constructor */
	public JDBCResultSet(List<String> columnNames, List<List<String>> stringTuples, boolean isQuads) {
		this.columnNames = new ArrayList<String>();
		// strip questions marks from column names (not sure why we call 'toLowerCase':
		for (String name : columnNames) this.columnNames.add(name.substring(1).toLowerCase());
		this.stringTuples = stringTuples;
		this.isQuads = isQuads;
	}
    
    private void initialize_tuple_width() {
        this.tuple_width = this.current_strings_row.size();
        this.low_index = JDBCResultSet.COUNTING_STARTS_AT;
        this.high_index = JDBCResultSet.COUNTING_STARTS_AT + this.tuple_width - 1;
        // allow for null context:
        if (this.isQuads && (this.high_index - this.low_index) == 2) this.high_index++;
    }
        
    /**
     * Verify that 'index' is in bounds.  Convert from column name to integer
     *  if necessary.  Return valid index.
     */
    private int _get_valid_cursor_index(int index) {
        if (this.tuple_width == -1) {
            this.initialize_tuple_width();
        }
        if (index >= this.low_index && index <= this.high_index) {
            return index - JDBCResultSet.COUNTING_STARTS_AT;
        } else {
             throw new JDBCException("'index' argument should be between " + this.low_index + " and " + this.high_index + ", inclusive.");
        }
     }

    private int _get_valid_cursor_index(String index) {
        if (this.tuple_width == -1) {
            this.initialize_tuple_width();
        }
        if (index == null) {
             throw new JDBCException("Failed to include 'index' argument to JDBC Statements iterator");
        }
        int i = 0;
        String name = index.toLowerCase();
        while (i < this.columnNames.size()) {
            if (name == this.columnNames.get(i)) {
                return i;
            }
            i++;
        }
        throw new JDBCException("Unmatched column name '" + index + "'");
    }

    /**
     * Return a terms row (a list) for 'this'.  It may or may not be partially or completely
     * filled in with terms.  Call 'getRow' to return a fully-filled-in row.
     */
    private List<Value> _get_terms_row() {
        List<Value >row = this.current_terms_row;
        if (row == null) {
            if (this.tuple_width == -1) {
                this.initialize_tuple_width();
            }
            row = new ArrayList<Value>(this.tuple_width);
            for (int i = 0; i < this.tuple_width; i++) row.add(null);
            this.current_terms_row = row;
        }
        return row;
    }
 
    /**
     * Return an integer value for the column denoted by 'index'.
     * Throw an exception if the column value is not either an integer, or
     * something readily convertible to an integer (e.g., a float).
     * 'index' may be an integer or a column name.
     * If the tuple denotes a Statement (a quad), then 0 = subject, 1 = predicate
     *  2 = object, and 3 = context. 
     * Note: Column numbering begins at zero.  True JDBC-style numbering begins at
     *  one (1).  Evaluate:
     *      JDBCResultSet.COUNTING_STARTS_AT = 1
     *   to make counting start at one.
     */
    public int getInt(int index) {
        index = this._get_valid_cursor_index(index);
        try {
            int value =  Integer.parseInt(AllegroStatement.ntriplesStringToStringValue(this.current_strings_row.get(index)));
            return value;
        } catch (Exception ex) {
        	String badVal = AllegroStatement.ntriplesStringToStringValue(this.current_strings_row.get(index));
            throw new JDBCException("Cannot convert value '" + badVal + "' to an integer.");
        }
    }
    
    public int getInt(String columnName) {
    	return this.getInt(this._get_valid_cursor_index(columnName));     
    }

    /**
     * Return a ResultSetMetaData object that provides the name and datatype of each
     * column in the ResultSet.
     */
    public Object getMetaData() {
        throw new UnimplementedMethodException("getMetaData");
    }

    /**
     * Return a Python tuple of OpenRDF Value objects.  For a SELECT query, the 
     * row contains a Value for each SELECT column.  For a getStatements query,
     * the row contains subject, predicate, object, and context values.
     * Note: This call does NOT advance the iterator to the next row.
     */
    public List<Value> getRow() {
    	this._get_valid_cursor_index(0); // force initialization of 'tuple_width'
        for (int i = 0; i < this.tuple_width; i++) {
            this.getValue(i);
        }
        return this._get_terms_row();
    }
      
    /**
     * Return a string value for the column denoted by 'index'.
     * For a resource-valued column, this is a URI label.  For a literal-valued
     * column, this is the string representation of the literal value.
     * If the tuple denotes a Statement (a quad), then 0 = subject, 1 = predicate
     *  2 = object, and 3 = context. 
     * Note: Column numbering begins at zero.  True JDBC-style numbering begins at
     *  one (1).  Evaluate:
     *      JDBCResultSet.COUNTING_STARTS_AT = 1
     *   to make counting start at one.
     */
    public String getString(int index) {
        index = this._get_valid_cursor_index(index);
        try {
        	return AllegroStatement.ntriplesStringToStringValue(this.current_strings_row.get(index));
        } catch (RuntimeException ex) {
        	// possibly return the null context:
        	if (this.isQuads && index == this.COUNTING_STARTS_AT + 3) return null;
        	throw ex;
        }
    }
    
    public String getString(String columnName) {
        return this.getString(this._get_valid_cursor_index(columnName));      
    }

    
    /**
     * Return an OpenRDF Value (a Resource or Literal) for the column denoted by 'index'
     * 'index' is an integer greater than zero.
     * If the tuple denotes a Statement (a quad), then 0 = subject, 1 = predicate
     *  2 = object, and 3 = context. 
     * Note: Column numbering begins at zero.  True JDBC-style numbering begins at
     *  one (1).  Evaluate:
     *      JDBCResultSet.COUNTING_STARTS_AT = 1
     *   to make counting start at one.
     */
    public Value getValue(int index) {
        List<Value> row = this._get_terms_row();
        index = this._get_valid_cursor_index(index);
        Value term = row.get(index);
        if (term == null) {
            term = AllegroStatement.stringTermToTerm(this.current_strings_row.get(index));
            row.set(index, term);
        }
        return term;
    }

    /**
     * Return an OpenRDF Value (a Resource or Literal) for the column named 'columnName'.
     * 'columnName' is a string matching a variable name.
     * If the tuple denotes a Statement (a quad), then 0 = subject, 1 = predicate
     *  2 = object, and 3 = context. 
     * Note: Column numbering begins at zero.  True JDBC-style numbering begins at
     *  one (1).  Evaluate:
     *      JDBCResultSet.COUNTING_STARTS_AT = 1
     *   to make counting start at one.
     */
    public Value getValue(String columnName) {
        return this.getValue(this._get_valid_cursor_index(columnName));      
    }

    /**
     * Advance to the next tuple.  Return True if there is one, and False
     * if the iterator has been exhausted.
     */
    public boolean next() {
        if (this.stringTuples == null) {
             throw new JDBCException("Failed to properly initialize JDBC ResultSet");
        }
        if (this.cursor < this.stringTuples.size()) {
            this.current_strings_row = this.stringTuples.get(this.cursor);
            this.current_terms_row = null;
            this.cursor++;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Currently a no-op.  When we do streaming, this may do something
     */
    public void close() {
    }
  

}
