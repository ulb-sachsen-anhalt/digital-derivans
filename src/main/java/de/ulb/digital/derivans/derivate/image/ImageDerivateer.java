package de.ulb.digital.derivans.derivate.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.derivate.BaseDerivateer;
import de.ulb.digital.derivans.model.DerivansData;

/**
 * 
 * Basic Abstract Image Derivate Creation
 * 
 * @author hartwig
 *
 */
public abstract class ImageDerivateer extends BaseDerivateer {

	public static final Integer DEFAULT_QUALITY = 80;

	public static final int DEFAULT_POOLSIZE = 2;

	public static final int MIN_FREE_CORES = 1;

	protected Boolean insertIntoMets;

	protected final Path inputDir;

	protected final Path outputDir;

	protected static final Logger LOGGER = LogManager.getLogger(ImageDerivateer.class);

	protected int poolSize;

	protected Integer maximal;

	protected ImageProcessor imageProcessor;

	protected ImageDerivateer(DerivansData input, DerivansData output) {
		super(input, output);
		this.insertIntoMets = false;
		this.poolSize = DEFAULT_POOLSIZE;
		this.inputDir = input.getPath();
		this.outputDir = output.getPath();
		this.imageProcessor = new ImageProcessor();
	}

	public void setImageProcessor(ImageProcessor processor) {
		this.imageProcessor = processor;
	}

	public void setPoolsize(Integer poolSize) {
		int cores = Runtime.getRuntime().availableProcessors();
		int limit = cores - MIN_FREE_CORES;
		if (poolSize != null && poolSize > 0) {
			if (poolSize >= limit) {
				this.poolSize = limit;
			} else {
				this.poolSize = poolSize;
			}
		} else {
			this.poolSize = MIN_FREE_CORES;
			LOGGER.warn("invalid poolsize provided:'{}', fallback to '{}'", poolSize, this.poolSize);
		}
	}

	protected int getPoolSize() {
		return this.poolSize;
	}

	public void setMaximal(Integer maximal) {
		this.maximal = maximal;
		this.imageProcessor.setMaximal(maximal);
	}

	protected boolean runWithPool(Runnable runnable) throws DigitalDerivansException {
		try {
			ForkJoinPool threadPool = new ForkJoinPool(poolSize);
			threadPool.submit(runnable).get();
			threadPool.shutdown();
			return true;
		} catch (ExecutionException e) {
			LOGGER.error(e);
			throw new DigitalDerivansException(e);
		} catch (InterruptedException e) {
			LOGGER.error(e);
			Thread.currentThread().interrupt();
		}
		return false;
	}

	@Override
	public int create() throws DigitalDerivansException {

		// basic precondition: output directory shall exist
		if (!Files.exists(this.getOutput().getPath())) {
			try {
				Files.createDirectory(this.getOutput().getPath());
			} catch (IOException e) {
				LOGGER.error(e);
				throw new DigitalDerivansException(e);
			}
		}

		String msg = String.format("process '%02d' images in %s with quality %.2f in %02d threads",
				this.digitalPages.size(), inputDir, this.imageProcessor.getQuality(), this.poolSize);
		LOGGER.info(msg);

		// forward to actual image creation implementation
		// subject to each concrete subclass
		try {
			boolean isSuccess = forward();
			if (isSuccess) {
				String msg2 = String.format("created '%02d' images at '%s'", digitalPages.size(), outputDir);
				LOGGER.info(msg2);
			}
		} catch (RuntimeException e) {
			throw new DigitalDerivansException(e.getMessage());
		}

		return this.digitalPages.size();
	}

	/**
	 * 
	 * Forward actual Image generation to Subclasses
	 * 
	 * @return
	 * @throws DigitalDerivansException
	 */
	public abstract boolean forward() throws DigitalDerivansException;

}
