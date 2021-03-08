package de.ulb.digital.derivans;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.data.MetadataStore;
import de.ulb.digital.derivans.derivate.BaseDerivateer;
import de.ulb.digital.derivans.derivate.IDerivateer;
import de.ulb.digital.derivans.derivate.ImageDerivateer;
import de.ulb.digital.derivans.derivate.ImageDerivateerJPGFooter;
import de.ulb.digital.derivans.derivate.ImageDerivateerJPGFooterGranular;
import de.ulb.digital.derivans.derivate.ImageDerivateerToJPG;
import de.ulb.digital.derivans.derivate.PDFDerivateer;
import de.ulb.digital.derivans.model.CommonConfiguration;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateStep;
import de.ulb.digital.derivans.model.DerivateType;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalFooter;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;

/**
 * 
 * Derive digital entities like pages with footer information and PDF from given
 * dataset with images and Metadata in METS/MODS-format
 * 
 * @author hartwig
 */
public class Derivans {

	public static final Logger LOGGER = LogManager.getLogger(Derivans.class);

	public static final String LABEL = "DigitalDerivans";

	private List<DerivateStep> steps;

	private Path processDir;

	private Path pathMetsFile;

	private IMetadataStore metadataStore;

	private CommonConfiguration commonConfiguration;

	boolean footerDerivatesRendered;

	boolean footerDerivatesForPDFRendered;

	/**
	 * 
	 * Create new Derivans-Instance from -at least- a directory with one sort of
	 * images
	 * 
	 * @param path - Path to Digitalization-Data-of-Interest
	 * 
	 * @throws DigitalDerivansException
	 */
	public Derivans(Path path) throws DigitalDerivansException {
		if (!Files.exists(path)) {
			String message = String.format("workdir not existing: '%s'!", path);
			LOGGER.error(message);
			throw new DigitalDerivansException(message);
		} else if (!(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))) {
			String message = String.format("workdir must be dir: '%s'!", path);
			LOGGER.error(message);
			throw new DigitalDerivansException(message);
		}
		this.processDir = path;
	}

	public Derivans(DerivansConfiguration conf) throws DigitalDerivansException {
		this(conf.getPathDir());

		// common configuration
		this.commonConfiguration = conf.getCommon();

		// handle Derivate Steps
		var confSteps = conf.getDerivateSteps();
		if (confSteps == null || confSteps.isEmpty()) {
			String msg = "DerivateSteps missing!";
			LOGGER.error(msg);
			throw new DigitalDerivansException(msg);
		}
		this.steps = new ArrayList<>(conf.getDerivateSteps());

		// handle optional METS-file
		var optConf = conf.getPathFile();
		if (optConf.isPresent()) {
			this.pathMetsFile = optConf.get();
			LOGGER.info("set derivates pathDir: '{}', metsFile: '{}'", processDir, pathMetsFile);
			this.metadataStore = new MetadataStore(pathMetsFile);
		} else {
			LOGGER.info("set derivates pathDir: '{}' without METS/MODS", processDir);
			this.metadataStore = new MetadataStore();
		}
	}

	public void create() throws DigitalDerivansException {

		// set data
		DescriptiveData dd = this.metadataStore.getDescriptiveData();
		var pages = this.metadataStore.getDigitalPagesInOrder();

		// store optional pdf path
		Optional<Path> optPDFPath = Optional.empty();

		// map configured derivateers
		List<IDerivateer> derivateers = new ArrayList<>();

		// process all given steps
		for (DerivateStep step : steps) {

			// create default base derivateer
			DerivansData input = new DerivansData(step.getInputPath(), DerivateType.IMAGE);
			DerivansData output = new DerivansData(step.getOutputPath(), DerivateType.JPG);
			BaseDerivateer base = new BaseDerivateer(input, output);

			// respect type
			DerivateType type = step.getDerivateType();
			if (type == DerivateType.JPG) {
				derivateers.add(transformToJPG(base, step));

			} else if (type == DerivateType.JPG_FOOTER) {
				ImageDerivateerJPGFooter d = transformToJPGFooter(base, step);
				boolean containsGranularUrns = inspect(pages);
				if (containsGranularUrns) {
					d = new ImageDerivateerJPGFooterGranular(d, pages);
				}
				derivateers.add(d);

			} else if (type == DerivateType.PDF) {
				DigitalStructureTree structure = this.metadataStore.getStructure();

				// merge configuration and metadata
				enrichConfigurationData(dd);

				// calculate final PDF path for post processing of metadata
				Path pdfPath = calculatePDFPath(dd, step);
				derivateers.add(new PDFDerivateer(base, pdfPath, structure, dd, pages));
				optPDFPath = Optional.of(pdfPath);
			}
		}

		// run all collected derivateers
		for (IDerivateer derivateer : derivateers) {
			boolean isSuccess = derivateer.create();
			LOGGER.info("finished derivate step '{}': '{}'", derivateer.getClass().getSimpleName(), isSuccess);
		}

		// post processing: enrich PDF in metadata if available
		if (optPDFPath.isPresent()) {
			Path pdfPath = optPDFPath.get();
			if (Files.exists(pdfPath)) {
				LOGGER.info("enrich created pdf '{}'", pdfPath);
				String metsIdentifier = dd.getIdentifier();
				this.metadataStore.enrichPDF(metsIdentifier);
			} else {
				String msg = "Missing pdf " + pdfPath.toString() + "!";
				LOGGER.error(msg);
				throw new DigitalDerivansException(msg);
			}
		} else {
			String msg = "Missing final PDF path!";
			LOGGER.error(msg);
			throw new DigitalDerivansException(msg);
		}
		LOGGER.info("finished generating '{}' derivates at '{}'", derivateers.size(), processDir);
	}

	private Path calculatePDFPath(DescriptiveData dd, DerivateStep step) throws DigitalDerivansException {
		Path pdfPath = step.getOutputPath();
		if (!Files.isDirectory(pdfPath, LinkOption.NOFOLLOW_LINKS)) {
			pdfPath = step.getOutputPath().getParent().resolve(pdfPath);
			try {
				Files.createDirectory(pdfPath);
			} catch (IOException e) {
				throw new DigitalDerivansException(e);
			}
		}
		if (Files.isDirectory(pdfPath)) {
			String identifier = dd.getIdentifier();
			if (identifier == null) {
				identifier = pdfPath.getFileName().toString();
			}
			String fileName = identifier + ".pdf";
			String prefix = step.getOutputPrefix();
			if (prefix != null && (!prefix.isBlank())) {
				fileName = prefix.concat(fileName);
			}
			return pdfPath.resolve(fileName).normalize();
		}
		throw new DigitalDerivansException("Can't create PDF: '" + pdfPath + "' invalid!");
	}

	/**
	 * 
	 * Enrich information from configuration in workflow.
	 * 
	 * Attention: overrides license from Metadata (if any present)
	 * 
	 * @param dd
	 */
	private void enrichConfigurationData(DescriptiveData dd) {
		Optional<String> optConfLicense = commonConfiguration.getLicense();
		if (optConfLicense.isPresent()) {
			Optional<String> optMetaLicense = dd.getLicense();
			if (optMetaLicense.isPresent()) {
				LOGGER.warn("replace '{}'(METS) with '{}'", optMetaLicense.get(), optConfLicense.get());
			}
			dd.setLicense(optConfLicense);
		}
		dd.setCreator(commonConfiguration.getCreator());
		dd.setKeywords(commonConfiguration.getKeywords());
	}

	private IDerivateer transformToJPG(BaseDerivateer base, DerivateStep step) {
		Integer quality = step.getQuality();
		ImageDerivateer d = new ImageDerivateerToJPG(base, quality);
		d.setPoolsize(step.getPoolsize());
		d.setMaximal(step.getMaximal());
		d.setOutputPrefix(step.getOutputPrefix());
		return d;
	}

	private ImageDerivateerJPGFooter transformToJPGFooter(BaseDerivateer base, DerivateStep step)
			throws DigitalDerivansException {
		String footerMetadata = getIdentifier();
		String footerLabel = step.getFooterLabel();
		Path pathTemplate = step.getPathTemplate();
		DigitalFooter footer = new DigitalFooter(footerLabel, footerMetadata, pathTemplate);
		Integer quality = step.getQuality();
		ImageDerivateerJPGFooter d = new ImageDerivateerJPGFooter(base, quality, footer);
		d.setPoolsize(step.getPoolsize());
		d.setMaximal(step.getMaximal());
		return d;
	}

	private boolean inspect(List<DigitalPage> pages) {
		return pages.stream().filter(page -> page.getIdentifier().isPresent()).map(page -> page.getIdentifier().get())
				.findAny().isPresent();
	}

	private String getIdentifier() {
		return this.metadataStore.getDescriptiveData().getUrn();
	}
}
