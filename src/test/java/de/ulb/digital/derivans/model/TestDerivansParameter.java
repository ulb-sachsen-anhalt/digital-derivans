package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import de.ulb.digital.derivans.DerivansParameter;
import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * @author hartwig
 *
 */
public class TestDerivansParameter {

	@Test
	void testConfigurationDefaults() throws CmdLineException, DigitalDerivansException {

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
	void testConfigurationInvalid() throws CmdLineException {

		// arrange
		DerivansParameter dp = new DerivansParameter();
		CmdLineParser parser = new CmdLineParser(dp, dp.getProperties());
		String[] args = {};

		// act
		assertThrows(CmdLineException.class, () -> parser.parseArgument(args));

	}
}
