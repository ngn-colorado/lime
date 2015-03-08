package edu.colorado.cs.ngn.lime.exceptions;

/**
 * Thrown if a state is detected where a switch is listed as a clone and an original
 * switch at the same time
 * 
 * @author Michael Coughlin
 *
 */
public class SwitchOriginalAndCloneException extends Exception {

	/**
	 * Default serial ID
	 */
	private static final long serialVersionUID = 1L;

	public SwitchOriginalAndCloneException() {
		// TODO Auto-generated constructor stub
	}

	public SwitchOriginalAndCloneException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public SwitchOriginalAndCloneException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public SwitchOriginalAndCloneException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public SwitchOriginalAndCloneException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
