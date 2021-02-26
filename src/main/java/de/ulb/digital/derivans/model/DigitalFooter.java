package de.ulb.digital.derivans.model;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Footer Text Data
 * 
 * @author hartwig
 *
 */
public class DigitalFooter {

	private List<String> text = new ArrayList<>();

	private Path pathTemplate;

	private BufferedImage footerImage;

	/**
	 * 
	 * Footer consists of:
	 * 
	 * <ul>
	 * <li>Line with Creator's Mark</li>
	 * <li>line with identifier like URN, DOI, ...</li>
	 * <li>Background Template Image</li>
	 * </ul>
	 * 
	 * Footer must be appended below each single {@link DigitalPage}.<br />
	 * If invalid Image Template Path provided, throws
	 * {@link DigitalDerivansException}
	 * 
	 * @param lineName
	 * @param urn
	 * @param template
	 * @throws DigitalDerivansException
	 */
	public DigitalFooter(String lineName, String urn, Path template) throws DigitalDerivansException {
		if (!lineName.isBlank()) {
			this.text.add(lineName);
		}
		if (urn != null && !urn.isBlank()) {
			this.text.add(urn);
		}
		if (template != null && Files.exists(template)) {
			this.pathTemplate = template;
			try {
				this.footerImage = ImageIO.read(template.toFile());
			} catch (IOException e) {
				throw new DigitalDerivansException(e);
			}
		}
	}

	/**
	 * 
	 * Used internally to process fine-grained URNs
	 * 
	 * @param lineName
	 * @param urn
	 * @param image
	 * @throws DigitalDerivansException
	 */
	public DigitalFooter(String lineName, String urn, BufferedImage image) {
		this.text.add(lineName);
		this.text.add(urn);
		this.footerImage = image;
	}

	public List<String> getText() {
		return this.text;
	}

	public Path getTemplate() {
		return this.pathTemplate;
	}

	public BufferedImage getBufferedImage() {
		return this.footerImage;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (pathTemplate != null)
			builder.append(pathTemplate).append(", ");
		if (text != null)
			builder.append(text);
		return builder.toString();
	}

}