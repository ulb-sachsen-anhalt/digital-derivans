package de.ulb.digital.derivans;

/**
 * 
 * Handle any runtime and data related errors which *might* happen
 * 
 * @author hartwig
 *
 */
public class DigitalDerivansRuntimeException extends RuntimeException {

	/**
	 * default serial version
	 */
	private static final long serialVersionUID = 1L;

	public DigitalDerivansRuntimeException() {

	}

	public DigitalDerivansRuntimeException(String message) {
		super(message);
	}

	public DigitalDerivansRuntimeException(Exception e) {
		super(e);
	}
}
