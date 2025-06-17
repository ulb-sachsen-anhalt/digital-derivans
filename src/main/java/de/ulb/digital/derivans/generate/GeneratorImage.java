package de.ulb.digital.derivans.generate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.generate.image.ImageProcessor;

/**
 * 
 * Basic Image Generation
 * 
 * @author hartwig
 *
 */
public abstract class GeneratorImage extends Generator {

	public static final Integer DEFAULT_QUALITY = 80;

	public static final int DEFAULT_POOLSIZE = 2;

	public static final int MIN_FREE_CORES = 1;

	protected static final Logger LOGGER = LogManager.getLogger(GeneratorImage.class);

	protected int poolSize = DEFAULT_POOLSIZE;

	protected ImageProcessor imageProcessor = new ImageProcessor();

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
		this.imageProcessor.setMaximal(maximal);
	}

	public void setQuality(int quality) {
		this.imageProcessor.setQuality(quality);
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
		if (this.step.getOutputDir() == null) {
			throw new DigitalDerivansRuntimeException("No outputDir: null!");
		}
		Path targetDir = this.rootDir.resolve(this.step.getOutputDir());
		if (!Files.exists(targetDir)) {
			try {
				Files.createDirectory(targetDir);
			} catch (IOException e) {
				LOGGER.error(e);
				throw new DigitalDerivansRuntimeException(e);
			}
		}
		if (this.step.getInputDir() == null) {
			throw new DigitalDerivansRuntimeException("No inputDir: null!");
		}
		if (this.digitalPages == null) {
			throw new DigitalDerivansRuntimeException("Invalid digitalPage: null!");
		}
		if(this.digitalPages.isEmpty()) {
			String msg = String.format("No digitalPages in %s/%s!",
				this.rootDir, this.step.getInputDir());
			throw new DigitalDerivansRuntimeException(msg);
		}

		String msg = String.format("process '%02d' images in %s/%s with quality %.2f and max %03d in %02d threads",
				this.digitalPages.size(), this.rootDir, this.step.getInputDir(),
				this.imageProcessor.getQuality(),
				this.imageProcessor.getMaximal(), this.poolSize);
		LOGGER.info(msg);

		// forward to actual image creation implementation
		// subject to each concrete subclass
		try {
			boolean isSuccess = forward();
			if (isSuccess) {
				String msg2 = String.format("created '%02d' images at '%s'", digitalPages.size(), this.step.getInputDir());
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
