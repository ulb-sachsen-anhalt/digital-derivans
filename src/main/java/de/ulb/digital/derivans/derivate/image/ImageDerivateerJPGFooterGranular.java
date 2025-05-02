package de.ulb.digital.derivans.derivate.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalFooter;
import de.ulb.digital.derivans.model.DigitalPage;

/**
 * 
 * Process pages with granular URNs
 * 
 * @author hartwig
 *
 */
public class ImageDerivateerJPGFooterGranular extends ImageDerivateerJPGFooter {

	private AtomicInteger nGranulars = new AtomicInteger();

	public ImageDerivateerJPGFooterGranular() {
		super();
	}

	/**
	 * 
	 * Create new Instance in rather isolated manner
	 * 
	 * @param input
	 * @param output
	 * @param quality
	 * @param footer
	 * @param pages
	 */
	public ImageDerivateerJPGFooterGranular(DerivansData input, DerivansData output,
			DigitalFooter footer, List<DigitalPage> pages, Integer quality) {
		super(input, output, footer, pages, quality);
		this.digitalPages = pages;
	}

	/**
	 * 
	 * Forward to super constructor and set also pages
	 * 
	 * @param d
	 */
	public ImageDerivateerJPGFooterGranular(ImageDerivateerJPGFooter d) {
		super(d);
	}

	public int getNumberOfGranularIdentifiers() {
		return nGranulars.get();
	}

	private void renderFooterGranular(DigitalPage page) {
		// Path pathIn = page.getFile().withDirname(this.input.getSubDir());
		Path pathIn = this.setInpath(page);
		if (!Files.exists(pathIn)) {
			throw new DigitalDerivansRuntimeException("input '" + pathIn + "' missing!");
		}
		Path pathOut = this.setOutpath(page);
		LOGGER.trace("read '{}' write '{}'", pathIn, pathOut);
		try {
			// keep track of granularity
			String urn = "";
			var optUrn = page.optContentIds();
			if (optUrn.isPresent()) {
				urn = optUrn.get();
			} else {
				throw new DigitalDerivansException("No granular URN: " + page + "!");
			}

			// do actual rendering
			BufferedImage bi = imageProcessor.clone(this.footerBuffer);
			DigitalFooter footer = new DigitalFooter(this.footer.getText().get(0), urn, bi);
			BufferedImage footerBuffer = footer.getBufferedImage();
			BufferedImage textBuffer = addTextLayer2Footer(footerBuffer, footer);
			int newHeight = imageProcessor.writeJPGwithFooter(pathIn, pathOut, textBuffer);
			page.setFooterHeight(newHeight);
			// keep track of rendered granular URNs with respect to concurrency
			nGranulars.getAndIncrement();
		} catch (IOException | DigitalDerivansException e) {
			LOGGER.error("pathIn: {}, footer: {} => {}", page, footer, e.getMessage());
		}
	}

	@Override
	public boolean forward() throws DigitalDerivansException {
		return this.runWithPool(() -> this.getDigitalPages().parallelStream().forEach(this::renderFooterGranular));
	}
}
