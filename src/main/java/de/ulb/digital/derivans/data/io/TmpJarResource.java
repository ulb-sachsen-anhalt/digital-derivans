package de.ulb.digital.derivans.data.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Exract resource from Jar
 * 
 * @author hartwig
 * 
 */
public class TmpJarResource {

	private String resPath;

	private Path tmpPath;

	public TmpJarResource(String resPath) {
		this.resPath = resPath;
	}

	public Path getTmpPath() {
		return this.tmpPath;
	}
		/**
	 * 
	 * Create temporary file from font in JAR
	 * 
	 * @param resPath
	 * @return
	 * @throws DigitalDerivansException
	 */
	public String extract(String prefix, String suffix) throws DigitalDerivansException {
		ClassLoader cl = this.getClass().getClassLoader();
		if (cl.getResource(this.resPath) != null) {
			try (InputStream input = cl.getResourceAsStream(resPath)) {
				File file = File.createTempFile(prefix, suffix);
				this.tmpPath = file.toPath();
				try(OutputStream out = new FileOutputStream(file)) {
					int read;
					byte[] bytes = new byte[1024];
					while ((read = input.read(bytes)) != -1) {
						out.write(bytes, 0, read);
					}
				}
				file.deleteOnExit();
				return file.toString();
			} catch (IOException e) {
				throw new DigitalDerivansException(e);
			}
		} else {
			return resPath;
		}
	}
}
