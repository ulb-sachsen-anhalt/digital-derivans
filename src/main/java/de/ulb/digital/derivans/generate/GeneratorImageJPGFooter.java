package de.ulb.digital.derivans.generate;

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
import java.util.concurrent.atomic.AtomicInteger;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.data.font.FontHandler;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.DigitalFooter;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateStepImageFooter;

/**
 * 
 * Create JPG-Images with Footer Template section
 * 
 * @author hartwig
 *
 */
public class GeneratorImageJPGFooter extends GeneratorImageJPG {

	protected static final Integer DEFAULT_FOOTER_HEIGHT = 120;

	protected static final Integer DEFAULT_FOOTER_WIDTH = 2400;

	protected DigitalFooter footer;

	protected BufferedImage footerBuffer;

	protected FontHandler fontHandler = new FontHandler();

	protected String footerFontFile = FontHandler.DEFAULT_FONT_FILE;

	private Font footerFont;

	private AtomicInteger nGranulars = new AtomicInteger();

	protected void init() {
		this.setFooterBuffer();
		try {
			this.footerFont = this.fontHandler.forGraphics(this.footerFontFile);
		} catch (DigitalDerivansException e) {
			LOGGER.error("fail font for {}:{}", this.footerFontFile, e.getMessage());
		}
	}

	public void setFooter(DigitalFooter footer) {
		this.footer = footer;
		this.init();
	}

	@Override
	public void setStep(DerivateStep step) throws DigitalDerivansException {
		super.setStep(step);
		DerivateStepImageFooter stepFooter = (DerivateStepImageFooter) step;
		String footerLabel = stepFooter.getFooterLabel();
		int quality = stepFooter.getQuality();
		this.setQuality(quality);
		Path pathTemplate = stepFooter.getPathTemplate();
		try {
			DigitalFooter footerUnknown = new DigitalFooter(footerLabel, IDerivans.UNKNOWN, pathTemplate);
			this.setFooter(footerUnknown);
			if (this.derivate.isMetadataPresent()) {
				var derivateMD = (DerivateMD) this.derivate;
				String workIdentifier = derivateMD.getIdentifierURN();
				DigitalFooter footerWithIdent = new DigitalFooter(footerLabel, workIdentifier, pathTemplate);
				this.setFooter(footerWithIdent);
			}
		} catch (DigitalDerivansException exc) {
			throw new DigitalDerivansRuntimeException(exc);
		}
	}

	protected DigitalFooter getDigitalFooter() {
		return this.footer;
	}

	protected void setFooterBuffer() {
		if (this.footer.getBufferedImage() != null) {
			this.footerBuffer = this.footer.getBufferedImage();
		}
	}

	private String renderFooter(DigitalPage page) {
		Path pathIn = page.getFile().withDirname(this.step.getInputDir());
		if (!Files.exists(pathIn)) {
			throw new DigitalDerivansRuntimeException("input '" + pathIn + "' missing!");
		}
		Path pathOut = this.setOutpath(page);
		try {
			BufferedImage thisBuffer = this.imageProcessor.clone(this.footerBuffer);
			String urn = "";
			var optUrn = page.optContentIds();
			if (optUrn.isPresent()) {
				urn = optUrn.get();
				DigitalFooter df = new DigitalFooter(this.footer.getText().get(0), urn, thisBuffer);
				thisBuffer = df.getBufferedImage();
				nGranulars.getAndIncrement();
			}
			BufferedImage textBuffer = this.addTextLayer2Footer(thisBuffer, this.footer);
			int newHeight = this.imageProcessor.writeJPGwithFooter(pathIn, pathOut, textBuffer);
			page.setFooterHeight(newHeight);
		} catch (IOException | DigitalDerivansException e) {
			LOGGER.error("pathIn: {}, footer: {} => {}", pathOut, footer, e.getMessage());
		}
		return pathOut.toString();
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

	public int getNumberOfGranularIdentifiers() {
		return nGranulars.get();
	}
}
