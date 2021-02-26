package de.ulb.digital.derivans;

import java.nio.file.Path;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import de.ulb.digital.derivans.config.DerivansConfiguration;

/**
 * 
 * Digital Derivans Main Application Entry Point
 * 
 * @author hartwig
 *
 */
public class App {

	public static void main(String[] args) throws Exception {
		DerivansParameter dp = new DerivansParameter();
		CmdLineParser parser = new CmdLineParser(dp, dp.getProperties());

		Path pathConfig = null;
		try {
			parser.parseArgument(args);
			pathConfig = dp.getPathConfig();
		} catch (CmdLineException | DigitalDerivansException exc) {
			Derivans.LOGGER.error(exc.getLocalizedMessage());
			parser.printUsage(System.out);
			System.exit(-1);
		}

		if (pathConfig != null) {
			dp.setPathConfig(pathConfig);
		}
		DerivansConfiguration conf = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(conf);
		derivans.create();
	}

}
