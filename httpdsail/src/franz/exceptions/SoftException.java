package franz.exceptions;

import java.io.PrintStream;
import java.io.PrintWriter;

public class SoftException extends RuntimeException {
	
	private Exception embeddedException = null;
	private String message = null;
	
	public SoftException(Exception ex) {
		this.embeddedException = ex;
	}
	
	public SoftException(String message, Exception ex) {
		this.message = message;
		this.embeddedException = ex;
	}

	
	public SoftException(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		String msg = "";
		if (this.message != null) msg += this.message;
		if (this.embeddedException != null) {
			// compensate for extreme idiocy of some Java programmer:
			if (this.embeddedException.getMessage() != null && this.embeddedException.getMessage().length() > 0)
				msg += this.embeddedException.getMessage();
			else
				msg += this.embeddedException.toString();
		}
		return msg;
	}
	
	public Throwable getCause() {
		if (this.embeddedException != null)
			return this.embeddedException.getCause();
		else
			return super.getCause();
	}

	public StackTraceElement[] getStackTrace() {
		if (this.embeddedException != null)
			return this.embeddedException.getStackTrace();
		else
			return super.getStackTrace();
	}	

	public void printStackTrace(PrintStream s) {
		if (this.embeddedException != null)
			this.embeddedException.printStackTrace(s);
		else
			super.printStackTrace(s);
	}

	public void printStackTrace(PrintWriter s) {
		if (this.embeddedException != null)
			this.embeddedException.printStackTrace(s);
		else
			super.printStackTrace(s);
	}

}
