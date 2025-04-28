package de.ulb.digital.derivans.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


/**
 * 
 * @author hartwig
 *
 */
class TestParameterCLI {

	@Test
	void testConfigurationDefaults() throws CmdLineException {

		// arrange
		DerivansParameter dp = new DerivansParameter();
		CmdLineParser parser = new CmdLineParser(dp, dp.getProperties());
		String[] args = { "/path/to/metsmods.xml" };

		// act
		parser.parseArgument(args);

		assertEquals("/path/to/metsmods.xml", dp.getPathInput().toString());
		assertNull(dp.getPathConfig());
	}

	@Test
	void testConfigurationInvalid() {

		// arrange
		DerivansParameter dp = new DerivansParameter();
		CmdLineParser parser = new CmdLineParser(dp, dp.getProperties());
		String[] args = {};

		// act
		assertThrows(CmdLineException.class, () -> parser.parseArgument(args));

	}
}
