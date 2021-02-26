package de.ulb.digital.derivans.derivate;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
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

	private List<DigitalPage> pages;

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
		super(input, output, quality, footer);
		this.pages = enrichPhysicalPath(input.getPath(), pages);
	}
	
	/**
	 * 
	 * Forward to super constructor and set also pages
	 * 
	 * @param d
	 */
	public ImageDerivateerJPGFooterGranular(ImageDerivateerJPGFooter d, List<DigitalPage> pages) {
		super(d.getInput(), d.getOutput(), d.getQuality(), d.getDigitalFooter());
		this.pages = enrichPhysicalPath(input.getPath(), pages);
	}
	
	/**
	 * 
	 * Determine each single Path since each page gets mapped to it's own identifier
	 * 
	 * @param inputPath
	 * @param pages
	 * @return
	 */
	private List<DigitalPage> enrichPhysicalPath(Path inputPath, List<DigitalPage> pages) {
		for (DigitalPage page : pages) {
			Path digitalPath = inputPath.resolve(Path.of(page.getFilePointer()));
			page.setPath(digitalPath);
		}
		return pages;
	}

	public int getNumberOfGranularIdentifiers() {
		return nGranulars.get();
	}
	
	private void renderFooterGranular(DigitalPage page) {
		String source = page.getPath().toString();
		String fileNameOut = new File(source).getName();
		String target = Path.of(outputDir.toString(), fileNameOut).toString();

		// sanitize input: mark image file extension if ..
		// .. no jpg extension and also no tif
		if (!source.endsWith(".jpg") && !source.endsWith(".tif")) {
			source = source + ".jpg";
		}

		// enforce jpg as default output format
		if (!target.endsWith(".jpg")) {
			target = target + ".jpg";
		}

		// granularity
		String urn = "";
		var optUrn = page.getIdentifier();
		if (optUrn.isPresent()) {
			urn = optUrn.get();
			nGranulars.getAndIncrement();
		} else {
			LOGGER.warn("No granular URN for {}!", page);
		}
		
		// do the actual rendering
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
					LOGGER.debug("scale footer {}x{} (ratio: {}) for: {}", 
							footerBuffer.getWidth(), footerBuffer.getHeight(), ratio, page.getPath());
				}
				
				BufferedImage textBuffer = addTextLayer2Footer(footerBuffer, footer);
				BufferedImage image = imageProcessor.append(originalImage, textBuffer);
			
				float compressionRatio = ((float) quality) / 100.0f;
				imageProcessor.writeJPGWithQuality(image, target, compressionRatio);
			}
		} catch (IOException e) {
			LOGGER.error("pathIn: {}, footer: {} => {}", page.getPath(), footer, e.getMessage());
		}
	}

	@Override
	public boolean forward() throws DigitalDerivansException {
		return this.runWithPool(() -> this.pages.parallelStream().forEach(this::renderFooterGranular));
	}
}
