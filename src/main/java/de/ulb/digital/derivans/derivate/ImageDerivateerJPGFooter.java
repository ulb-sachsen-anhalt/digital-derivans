package de.ulb.digital.derivans.derivate;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
public class ImageDerivateerJPGFooter extends ImageDerivateerJPG {

	protected static final Integer DEFAULT_FOOTER_HEIGHT = 120;

	protected static final Integer DEFAULT_FOOTER_WIDTH = 2400;

	protected DigitalFooter footer;

	protected BufferedImage footerBuffer;

	protected FontHandler fontHandler = new FontHandler();

	protected String footerFontFile = FontHandler.DEFAULT_FONT_FILE;
	
	private Font footerFont;

	public ImageDerivateerJPGFooter(DerivansData input, DerivansData output, DigitalFooter footer, List<DigitalPage> pages, Integer quality) {
		super(input, output, quality);
		this.footer = footer;
		this.digitalPages = pages;
		this.init();
	}
	
	public ImageDerivateerJPGFooter(ImageDerivateerJPGFooter jpgFooter, int quality) {
		super(jpgFooter.getInput(), jpgFooter.getOutput(), quality);
		this.footer = jpgFooter.getDigitalFooter();
		this.digitalPages = jpgFooter.getDigitalPages();
		this.init();
	}

	/**
	 * 
	 * Super type constructor
	 * 
	 * @param base
	 * @param quality
	 * @param footer
	 * @param pages
	 */
	public ImageDerivateerJPGFooter(BaseDerivateer base, Integer quality, DigitalFooter footer) {
		super(base.getInput(), base.getOutput(), quality);
		this.digitalPages = base.getDigitalPages();
		this.resolver = base.getResolver();
		this.footer = footer;
		this.init();
	}

	protected void init() {
		this.setFooterBuffer();
		try {
			this.footerFont = this.fontHandler.forGraphics(this.footerFontFile);
		} catch (DigitalDerivansException e) {
			LOGGER.error("fail font for {}:{}", this.footerFontFile, e.getMessage());
		}
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

	private String renderFooter(DigitalPage page) {
		Path sourcePath = page.getImagePath();
		if (!Files.exists(sourcePath)) {
			throw new RuntimeException("input '"+ sourcePath + "' missing!");
		}
		this.resolver.setImagePath(page, this);
		Path targetPath = page.getImagePath();

		try {
			BufferedImage currentFooter = imageProcessor.clone(footerBuffer);
			BufferedImage textFooterBuffer = addTextLayer2Footer(currentFooter, footer);
			int newHeight = imageProcessor.writeJPGwithFooter(sourcePath, targetPath, textFooterBuffer);
			page.setFooterHeight(newHeight);
		} catch (IOException | DigitalDerivansException e) {
			LOGGER.error("pathIn: {}, footer: {} => {}", sourcePath, footer, e.getMessage());
		}

		return targetPath.toString();
	}

	protected BufferedImage addTextLayer2Footer(BufferedImage bufferedImage, DigitalFooter footR) {
		List<String> lines = footR.getText();
		int totalHeight = bufferedImage.getHeight();
		int nLines = lines.size();
		int heightPerLine = totalHeight / nLines;
		int fontSize = (int) (heightPerLine * .5);
		LOGGER.trace("textlayer total:{}/ line:{}/ fontsize:{} ({})", totalHeight, heightPerLine, fontSize, footR);
		Graphics2D g2d = bufferedImage.createGraphics();
		g2d.setColor(Color.WHITE);
		
		// we want bold font with specific actual fontsize
		String footerFontName = this.footerFont.getFontName();
		Font theFont = new Font(footerFontName, Font.BOLD, fontSize); 
		g2d.setFont(theFont);

		int lineHeightRedux = heightPerLine - (int) (heightPerLine * 0.2);
		int y = lineHeightRedux;
		FontMetrics fontMetrics = g2d.getFontMetrics();
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
		return this.runWithPool(() -> this.getDigitalPages().parallelStream().forEach(this::renderFooter));
	}

}
