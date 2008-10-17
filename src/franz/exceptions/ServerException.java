package franz.exceptions;

public class ServerException extends RuntimeException {
	private String message = null;

	public ServerException(String message) {
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}

}
