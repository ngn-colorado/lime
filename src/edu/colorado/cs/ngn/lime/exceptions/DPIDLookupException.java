package edu.colorado.cs.ngn.lime.exceptions;
/**
 * Thrown when a dpid lookup fails
 * 
 * @author Michael Coughlin
 *
 */
public class DPIDLookupException extends Exception {

	/**
	 * Default serial ID
	 */
	private static final long serialVersionUID = 1L;

	public DPIDLookupException() {
		// TODO Auto-generated constructor stub
	}

	public DPIDLookupException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public DPIDLookupException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public DPIDLookupException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public DPIDLookupException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
