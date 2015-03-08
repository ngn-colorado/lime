package edu.colorado.cs.ngn.lime.exceptions;

/**
 * Exception thrown when a mac lookup fails
 * 
 * @author Michael Coughlin
 *
 */
public class MacLookupException extends Exception {

	/**
	 * Default serial id
	 */
	private static final long serialVersionUID = 1L;

	public MacLookupException() {
		// TODO Auto-generated constructor stub
	}

	public MacLookupException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public MacLookupException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public MacLookupException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public MacLookupException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
