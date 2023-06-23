package de.ulb.digital.derivans;

import java.nio.file.Path;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;

/**
 * 
 * Digital Derivans Main Application Entry Point
 * 
 * @author hartwig
 *
 */
public class App {

	@java.lang.SuppressWarnings("java:S106") 
	public static void main(String[] args) throws Exception {
		DerivansParameter dp = new DerivansParameter();
		CmdLineParser parser = new CmdLineParser(dp, dp.getProperties());

		Path pathConfig = null;
		try {
			parser.parseArgument(args);
			pathConfig = dp.getPathConfig();
		} catch (CmdLineException exc) {
			Derivans.LOGGER.error(exc.getLocalizedMessage());
			// sonarlint hint disabled by method annotation above
			parser.printUsage(System.out);
			System.exit(-1);
		}

		if (pathConfig != null) {
			dp.setPathConfig(pathConfig);
		}
		
		// evaluate configuration and start derivans
		try {
			DerivansConfiguration conf = new DerivansConfiguration(dp);
			Derivans derivans = new Derivans(conf);
			derivans.create();
		} catch (DigitalDerivansException e) {
			Derivans.LOGGER.error(e.getLocalizedMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
}
