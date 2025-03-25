package de.ulb.digital.derivans.data.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Extract resource from Jar file
 * 
 * @author hartwig
 * 
 */
public class JarResource {

	private String resPath;

	private Path tmpPath;

	public JarResource() {
	}

	public JarResource(String resPath) {
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
			try (InputStream input = cl.getResourceAsStream(this.resPath)) {
				File file = File.createTempFile(prefix, suffix);
				this.tmpPath = file.toPath();
				try (OutputStream out = new FileOutputStream(file)) {
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

	public Optional<String> derivansVersion(String fileName) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(fileName);
		StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.strip().length() > 3) {
					resultStringBuilder.append(line);
				}
			}
			String contents = resultStringBuilder.toString();
			String[] parts = contents.split(":");
			if (parts.length > 1) {
				String version = parts[1].replace('"', ' ').strip();
				return Optional.of(version);
			}
		} catch (IOException e) {
			Derivans.LOGGER.warn("cannot read {}", fileName);
		}
		return Optional.empty();
	}
}
