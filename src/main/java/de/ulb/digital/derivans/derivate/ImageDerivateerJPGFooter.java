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
import java.util.List;

import javax.imageio.ImageIO;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalFooter;

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

	protected DigitalFooter footer;

	protected BufferedImage footerBuffer;

	public ImageDerivateerJPGFooter(DerivansData input, DerivansData output, Integer quality, DigitalFooter footer) {
		super(input, output, quality);
		this.footer = footer;
		this.setFooterBuffer();
	}

	public ImageDerivateerJPGFooter(BaseDerivateer base, Integer quality, DigitalFooter footer) {
		super(base.getInput(), base.getOutput(), quality);
		this.footer = footer;
		this.setFooterBuffer();
	}

	protected DigitalFooter getDigitalFooter() {
		return this.footer;
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

	private String renderFooter(Path pathIn) {
		String pathStr = pathIn.toString();
		String fileNameOut = new File(pathStr).getName();
		String target = Path.of(this.outputDir.toString(), fileNameOut).toString();

		// enforce jpg output
		if (target.endsWith(".tif")) {
			target = target.replace(".tif", ".jpg");
		}

		try {
			byte[] bytes = Files.readAllBytes(pathIn);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			if (this.maximal != null) {
				image = handleMaximalDimension(image);
			}
			
			int currentW = image.getWidth();
			
			// only scale footer image if ratio is larger than defined threshold
			float ratio = (float) currentW / (float) footerBuffer.getWidth();
			if (Math.abs(1.0 - ratio) > MAXIMAL_RATIO_DEVIATION) {
				footerBuffer = imageProcessor.scale(footerBuffer, ratio);
				LOGGER.debug("scale footer {}x{} (ratio: {}) for: {}", 
						footerBuffer.getWidth(), footerBuffer.getHeight(), ratio, pathIn);
			}
			BufferedImage currentFooter = imageProcessor.clone(footerBuffer);
			BufferedImage bi = addTextLayer2Footer(currentFooter, footer);
			image = imageProcessor.append(image, bi);
			
			float qualityRatio = ((float) quality) / 100.0f;
			imageProcessor.writeJPGWithQuality(image, target, qualityRatio);
		} catch (IOException e) {
			LOGGER.error("pathIn: {}, footer: {} => {}", pathIn, footer, e.getMessage());
		}

		return target;
	}

	protected BufferedImage addTextLayer2Footer(BufferedImage bufferedImage, DigitalFooter footR) {
		List<String> lines = footR.getText();
		int totalHeight = bufferedImage.getHeight();
		int nLines = lines.size();
		int heightPerLine = totalHeight / nLines;
		int fontSize = (int) (heightPerLine * .5);
		LOGGER.debug("render footer textlayer with totalHeight: '{}', lineHeight: '{}', fontSize: '{}' ({})", totalHeight, heightPerLine, fontSize, footR);
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
		return this.runWithPool(() -> this.inputPaths.parallelStream().forEach(this::renderFooter));
	}

}
