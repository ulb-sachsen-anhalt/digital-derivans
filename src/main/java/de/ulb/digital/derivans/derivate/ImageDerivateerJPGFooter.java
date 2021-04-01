package de.ulb.digital.derivans.derivate;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalFooter;
import de.ulb.digital.derivans.model.DigitalPage;

/**
 * 
 * Create JPG-Images with Footer Template section
 * 
 * @author hartwig
 *
 */
public class ImageDerivateerJPGFooter extends ImageDerivateerToJPG {

	protected static final String DEFAULT_FONT = "Helvetica";

	protected static final Integer DEFAULT_FOOTER_HEIGHT = 120;

	protected static final Integer DEFAULT_FOOTER_WIDTH = 2400;

	protected static final Float MAXIMAL_RATIO_DEVIATION = 0.02f;
	
	/**
	 * Error marker, if a large number of subsequent down scales 
	 * make the footer disappear after all 
	 */
	protected static final Integer EXPECTED_MINIMAL_HEIGHT = 25;

	protected DigitalFooter footer;

	protected BufferedImage footerBuffer;
	
	protected List<DigitalPage> pages = new ArrayList<>();

	public ImageDerivateerJPGFooter(DerivansData input, DerivansData output, Integer quality, DigitalFooter footer) {
		super(input, output, quality);
		this.footer = footer;
		this.setFooterBuffer();
		for(Path path : this.inputPaths) {
			this.pages.add(new DigitalPage(path));
		}
	}

	public ImageDerivateerJPGFooter(BaseDerivateer base, Integer quality, DigitalFooter footer, List<DigitalPage> pages) {
		super(base.getInput(), base.getOutput(), quality);
		this.footer = footer;
		if(pages != null && ! pages.isEmpty()) {
			this.pages = enrichPhysicalPath(input.getPath(), pages);
		} else {
			try {
				this.inputPaths = getFilePaths(inputDir, imageFilter);
				for(Path path : this.inputPaths) {
					this.pages.add(new DigitalPage(path));
				}
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
		this.setFooterBuffer();
	}

	protected DigitalFooter getDigitalFooter() {
		return this.footer;
	}
	
	protected List<DigitalPage> getPages() {
		return this.pages;
	}
	
	protected void setFooterBuffer() {
		if (this.footer.getBufferedImage() != null) {
			this.footerBuffer = this.footer.getBufferedImage();
		} else if (this.inputDir != null) {
			LOGGER.warn("no footer template, create with {}x{}", DEFAULT_FOOTER_WIDTH, DEFAULT_FOOTER_HEIGHT);
			BufferedImage bi = new BufferedImage(DEFAULT_FOOTER_WIDTH, DEFAULT_FOOTER_HEIGHT,
					BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = bi.createGraphics();
			g2d.setColor(Color.DARK_GRAY);
			g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
			g2d.dispose();
			this.footerBuffer = bi;
		}
	}

	private String renderFooter(DigitalPage page) {
		String source = page.getImagePath().toString();
		String fileNameOut = new File(source).getName();
		String target = Path.of(this.outputDir.toString(), fileNameOut).toString();

		// enforce jpg output
		if (target.endsWith(".tif")) {
			target = target.replace(".tif", ".jpg");
		}

		try {
			byte[] bytes = Files.readAllBytes(Path.of(source));
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			if (this.maximal != null) {
				image = handleMaximalDimension(image);
			}
			
			int currentW = image.getWidth();
			BufferedImage currentFooter = imageProcessor.clone(footerBuffer);
			
			// only scale footer image if ratio is larger than defined threshold
			float ratio = (float) currentW / (float) currentFooter.getWidth();
			if (Math.abs(1.0 - ratio) > MAXIMAL_RATIO_DEVIATION) {
				currentFooter = imageProcessor.scale(currentFooter, ratio);
				String msg = String.format("scale footer %dx%d (ratio: %.3f) for %s", currentFooter.getWidth(), currentFooter.getHeight(), ratio, source);
				LOGGER.debug(msg);
				if(currentFooter.getHeight() < EXPECTED_MINIMAL_HEIGHT) {
					String msg2 = String.format("scale problem: heigth dropped beneath '%d'", footerBuffer.getHeight());
					LOGGER.error(msg2);
					throw new DigitalDerivansException(msg2);
				}
			}
			BufferedImage bi = addTextLayer2Footer(currentFooter, footer);
			image = imageProcessor.append(image, bi);
			float qualityRatio = ((float) quality) / 100.0f;
			imageProcessor.writeJPGWithQuality(image, target, qualityRatio);
			page.setFooterHeight(currentFooter.getHeight());
		} catch (IOException e) {
			LOGGER.error("pathIn: {}, footer: {} => {}", source, footer, e.getMessage());
		} catch(Exception e) {
			LOGGER.error("pathIn: {}, footer: {} => {}", source, footer, e.getMessage());
		}

		return target;
	}

	protected BufferedImage addTextLayer2Footer(BufferedImage bufferedImage, DigitalFooter footR) {
		List<String> lines = footR.getText();
		int totalHeight = bufferedImage.getHeight();
		int nLines = lines.size();
		int heightPerLine = totalHeight / nLines;
		int fontSize = (int) (heightPerLine * .5);
		LOGGER.debug("textlayer total:{}/ line:{}/ fontsize:{} ({})", totalHeight, heightPerLine, fontSize, footR);
		Font theFont = new Font(DEFAULT_FONT, Font.BOLD, fontSize);
		Graphics2D g2d = bufferedImage.createGraphics();
		g2d.setColor(Color.WHITE);
		g2d.setFont(theFont);
		FontMetrics fontMetrics = g2d.getFontMetrics();
		int lineHeightRedux = heightPerLine - (int) (heightPerLine * 0.2);
		int y = lineHeightRedux;
		for (String line : lines) {
			Rectangle2D rect = fontMetrics.getStringBounds(line, g2d);
			int centerX = (bufferedImage.getWidth() - (int) rect.getWidth()) / 2;
			g2d.drawString(line, centerX, y);
			y += lineHeightRedux;
		}
		g2d.dispose();
		return bufferedImage;
	}

	@Override
	public boolean forward() throws DigitalDerivansException {
		return this.runWithPool(() -> this.pages.parallelStream().forEach(this::renderFooter));
	}

	/**
	 * 
	 * Determine each single Path since each page gets mapped to it's own identifier
	 * 
	 * @param inputPath
	 * @param pages
	 * @return
	 */
	protected List<DigitalPage> enrichPhysicalPath(Path inputPath, List<DigitalPage> pages) {
		for (DigitalPage page : pages) {
			Path digitalPath = inputPath.resolve(page.getImagePath());
			page.setImagePath(digitalPath);
		}
		return pages;
	}

}
