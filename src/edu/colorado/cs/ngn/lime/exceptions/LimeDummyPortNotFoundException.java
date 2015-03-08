package edu.colorado.cs.ngn.lime.exceptions;

/**
 * Exception thrown when a dummy port for a switch is not found
 * 
 * @author Michael Coughlin
 *
 */
public class LimeDummyPortNotFoundException extends Exception {

	/**
	 * Default serial ID
	 */
	private static final long serialVersionUID = 1L;

	public LimeDummyPortNotFoundException() {
		// TODO Auto-generated constructor stub
	}

	public LimeDummyPortNotFoundException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public LimeDummyPortNotFoundException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public LimeDummyPortNotFoundException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public LimeDummyPortNotFoundException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
