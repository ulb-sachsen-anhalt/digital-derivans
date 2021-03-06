package de.ulb.digital.derivans.derivate;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

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
		super(input, output, quality, footer, pages);
		this.digitalPages = pages;
	}
	
	/**
	 * 
	 * Forward to super constructor and set also pages
	 * 
	 * @param d
	 */
	public ImageDerivateerJPGFooterGranular(ImageDerivateerJPGFooter d) {
		super(d.getInput(), d.getOutput(), d.getQuality(), d.getDigitalFooter(), d.digitalPages);
		this.digitalPages = d.getDigitalPages();
		this.poolSize = d.getPoolSize();
	}
	
	public int getNumberOfGranularIdentifiers() {
		return nGranulars.get();
	}
	
	private void renderFooterGranular(DigitalPage page) {
		String source = page.getImagePath().toString();
		this.resolver.setImagePath(page, this);
		String target = page.getImagePath().toString();
		LOGGER.debug("read '{}' write '{}'", source, target);

		// keep track of granularity
		String urn = "";
		var optUrn = page.getIdentifier();
		if (optUrn.isPresent()) {
			urn = optUrn.get();
		} else {
			LOGGER.warn("No granular URN for {}!", page);
		}
		
		// do actual rendering
		BufferedImage bi = imageProcessor.clone(this.footerBuffer);
		DigitalFooter footer = new DigitalFooter(this.footer.getText().get(0), urn, bi);
		try {
			byte[] bytes = Files.readAllBytes(Path.of(source));
			BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(bytes));
			if (this.maximal != null) {
				originalImage = handleMaximalDimension(originalImage);
			}
			
			// forward actual footer handling
			if (footer.getBufferedImage() != null) {
				BufferedImage footerBuffer = footer.getBufferedImage();
				int currentImageWidth = originalImage.getWidth();

				// only scale footer image if ratio is larger than defined threshold
				float ratio = (float) currentImageWidth / (float) footerBuffer.getWidth();
				if (Math.abs(1.0 - ratio) > MAXIMAL_RATIO_DEVIATION) {
					footerBuffer = imageProcessor.scale(footerBuffer, ratio);
					LOGGER.trace("scale footer {}x{} (ratio: {}) for: {}", 
							footerBuffer.getWidth(), footerBuffer.getHeight(), ratio, page.getImagePath());
				}
				
				BufferedImage textBuffer = addTextLayer2Footer(footerBuffer, footer);
				BufferedImage image = imageProcessor.append(originalImage, textBuffer);
			
				float compressionRatio = ((float) quality) / 100.0f;
				imageProcessor.writeJPGWithQuality(image, target, compressionRatio);
				page.setFooterHeight(footerBuffer.getHeight());
				
				// keep track of granularity if 
				// and only if optional granular urn present
				if(optUrn.isPresent()) {
					nGranulars.getAndIncrement();
				}
			}
		} catch (IOException e) {
			LOGGER.error("pathIn: {}, footer: {} => {}", page.getImagePath(), footer, e.getMessage());
		}
	}

	@Override
	public boolean forward() throws DigitalDerivansException {
		return this.runWithPool(() -> this.getDigitalPages().parallelStream().forEach(this::renderFooterGranular));
	}
}
