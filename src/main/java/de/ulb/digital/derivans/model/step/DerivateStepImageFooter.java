package de.ulb.digital.derivans.model.step;

import java.nio.file.Path;

/**
 * 
 * Specific derivates which extend common 
 * image derivates by appending image footer
 * with optional metadata information
 * 
 * @author hartwig
 *
 */
public class DerivateStepImageFooter extends DerivateStepImage {

	protected Path pathTemplate;
	protected String footerLabel;

	public DerivateStepImageFooter() {
		super();
		this.setOutputType(DerivateType.JPG_FOOTER);
	}

	/**
	 * 
	 * Copy information from base image step
	 * 
	 * @param baseImageStep
	 */
	public DerivateStepImageFooter(DerivateStepImage baseImageStep) {
		super(baseImageStep);
		this.setOutputType(DerivateType.JPG_FOOTER);
	}

	public Path getPathTemplate() {
		return pathTemplate;
	}

	public void setPathTemplate(Path pathTemplate) {
		this.pathTemplate = pathTemplate;
	}

	public String getFooterLabel() {
		return footerLabel;
	}

	public void setFooterLabel(String footerLabel) {
		this.footerLabel = footerLabel;
	}

}
