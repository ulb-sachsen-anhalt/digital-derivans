package de.ulb.digital.derivans;

/**
 * 
 * Handle all domain specific Exceptions
 * 
 * @author hartwig
 *
 */
public class DigitalDerivansException extends Exception {

	/**
	 * default serial version
	 */
	private static final long serialVersionUID = 1L;

	public DigitalDerivansException() {

	}

	public DigitalDerivansException(String message) {
		super(message);
	}

	public DigitalDerivansException(Exception e) {
		super(e);
	}
}
