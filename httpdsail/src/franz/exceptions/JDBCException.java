package franz.exceptions;

public class JDBCException extends RuntimeException {
	private String message = null;

	public JDBCException(String message) {
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}

}
