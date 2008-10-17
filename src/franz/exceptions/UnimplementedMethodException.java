package franz.exceptions;

public class UnimplementedMethodException extends RuntimeException {
	
	private String message = null;

	public UnimplementedMethodException(String message) {
		this.message = message;
	}

	public String getMessage() {
		return "The method '" + this.message + "' is not implemented.";
	}

}
