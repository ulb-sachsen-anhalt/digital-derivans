package de.ulb.digital.derivans;

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

import javax.imageio.ImageIO;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;


import de.ulb.digital.derivans.data.IMetadataStore;

/**
 * 
 * Some little test helpers assembled adhere
 * 
 * @author u.hartwig
 */
public class TestHelper {

	public static void generateJpgs(Path imageDir, int width, int height, int number) throws IOException {
		if (Files.exists(imageDir)) {
			Files.delete(imageDir);
		}
		Files.createDirectory(imageDir);
		for (int i = 1; i <= number; i++) {
			String imagePath = String.format("%04d.jpg", i);
			Path jpgFile = imageDir.resolve(imagePath);
			writeImage(jpgFile, width, height, BufferedImage.TYPE_3BYTE_BGR, "JPG");
		}
	}

	public static void writeImage(Path imagePath, int width, int height, int type, String format) throws IOException {
		BufferedImage bi2 = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Font theFont = new Font("Helvetica", Font.BOLD, 84);
		Graphics2D g2d = bi2.createGraphics();
		g2d.setColor(Color.WHITE);
		g2d.setFont(theFont);
		FontMetrics fontMetrics = g2d.getFontMetrics();
		String imageName = imagePath.getFileName().toString();
		Rectangle2D rect = fontMetrics.getStringBounds(imageName, g2d);
		int centerX = (bi2.getWidth() - (int) rect.getWidth()) / 2;
		g2d.drawString(imageName, centerX, 500);
		g2d.dispose();
		ImageIO.write(bi2, format, imagePath.toFile());
	}

	public static void generateJpgsFromList(Path imageDir, int width, int height, List<String> labels)
			throws IOException {
		if (Files.exists(imageDir)) {
			Files.delete(imageDir);
		}
		Files.createDirectory(imageDir);
		for (int i = 0; i < labels.size(); i++) {
			Path jpgFile = imageDir.resolve(labels.get(i) + ".jpg");
			BufferedImage bi2 = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
		}
	}

	public static void copyTree(Path pathSource, Path pathTarget) {
		try {
			Files.walk(pathSource).forEach(s -> {
				try {
					Path d = pathTarget.resolve(pathSource.relativize(s));
					if (Files.isDirectory(s)) {
						if (!Files.exists(d))
							Files.createDirectory(d);
						return;
					}
					Files.copy(s, d);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Document readXMLDocument(Path filePath) throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		return builder.build(filePath.toFile());
	}

	public static XPathExpression<Element> generateXpression(String xpathStr) {
		XPathBuilder<Element> builder = new XPathBuilder<Element>(xpathStr, Filters.element());
		builder.setNamespace(IMetadataStore.NS_METS);
		return builder.compileWith(XPathFactory.instance());
	}

}
