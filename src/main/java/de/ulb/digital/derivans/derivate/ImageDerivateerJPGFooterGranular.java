package de.ulb.digital.derivans.derivate;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.ulb.digital.derivans.DigitalDerivansException;
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
	public ImageDerivateerJPGFooterGranular(DerivansData input, DerivansData output, Integer quality,
			DigitalFooter footer, List<DigitalPage> pages) {
		super(input, output, footer, pages, quality);
		this.digitalPages = pages;
	}
	
	/**
	 * 
	 * Forward to super constructor and set also pages
	 * 
	 * @param d
	 */
	public ImageDerivateerJPGFooterGranular(ImageDerivateerJPGFooter d, int quality) {
		super(d, quality);
		this.digitalPages = d.getDigitalPages();
		this.poolSize = d.getPoolSize();
	}
	
	public int getNumberOfGranularIdentifiers() {
		return nGranulars.get();
	}
	
	private void renderFooterGranular(DigitalPage page) {
		Path pathIn = page.getImagePath();
		if (!Files.exists(pathIn)) {
			throw new RuntimeException("input '"+ pathIn + "' missing!");
		}
		this.resolver.setImagePath(page, this);
		Path pathOut = page.getImagePath();
		LOGGER.trace("read '{}' write '{}'", pathIn, pathOut);
		try {
			// keep track of granularity
			String urn = "";
			var optUrn = page.optIdentifier();
			if (optUrn.isPresent()) {
				urn = optUrn.get();
			} else {
				throw new DigitalDerivansException("No granular URN: " + page.getImagePath()+ "!");
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
			LOGGER.error("pathIn: {}, footer: {} => {}", page.getImagePath(), footer, e.getMessage());
		}
	}

	@Override
	public boolean forward() throws DigitalDerivansException {
		return this.runWithPool(() -> this.getDigitalPages().parallelStream().forEach(this::renderFooterGranular));
	}
}
