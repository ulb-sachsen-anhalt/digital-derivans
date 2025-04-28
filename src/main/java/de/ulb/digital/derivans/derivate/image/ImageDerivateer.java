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

	protected static final Logger LOGGER = LogManager.getLogger(ImageDerivateer.class);

	protected int poolSize = DEFAULT_POOLSIZE;

	protected int maximal;

	protected int quality;

	protected ImageProcessor imageProcessor = new ImageProcessor();

	protected ImageDerivateer() {
		super();
	}

	protected ImageDerivateer(DerivansData input, DerivansData output) {
		super(input, output);
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

	public int getMaximal() {
		return this.maximal;
	}

	public void setQuality(int quality) {
		this.quality = quality;
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
		Path targetDir = this.getOutput().getRootDir().resolve(this.getOutput().getSubDir());
		if (!Files.exists(targetDir)) {
			try {
				Files.createDirectory(targetDir);
			} catch (IOException e) {
				LOGGER.error(e);
				throw new DigitalDerivansException(e);
			}
		}
		if (this.digitalPages == null) {
			throw new DigitalDerivansException("Invalid digitalPage input: null!");
		}
		if(this.digitalPages.isEmpty()) {
			String msg = String.format("No digitalPages in %s/%s",
				this.input.getRootDir(), this.input.getSubDir());
			throw new DigitalDerivansException(msg);
		}

		String msg = String.format("process '%02d' images in %s/%s with quality %.2f in %02d threads",
				this.digitalPages.size(), this.input.getRootDir(), this.input.getSubDir(),
				this.imageProcessor.getQuality(), this.poolSize);
		LOGGER.info(msg);

		// forward to actual image creation implementation
		// subject to each concrete subclass
		try {
			boolean isSuccess = forward();
			if (isSuccess) {
				String msg2 = String.format("created '%02d' images at '%s'", digitalPages.size(), this.input.getSubDir());
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
