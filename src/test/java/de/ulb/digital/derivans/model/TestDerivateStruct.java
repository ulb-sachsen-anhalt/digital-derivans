package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 
 * Specification for {@link DerivateStruct}
 * 
 * @author hartwig
 *
 */
class TestDerivateStruct {

	@ParameterizedTest
	@CsvSource({ "6,[Seite 8],6,[Seite 8]",
			"7,[Tabelle],7,[Tabelle]" })
	void testInstancesEquals(int order1, String label1, int order2, String label2) {

		// arrange
		var s1 = new DerivateStruct(order1, label1);
		var s2 = new DerivateStruct(order2, label2);

		// act
		assertEquals(s1, s2);
	}

	@ParameterizedTest
	@CsvSource({ "6,[Seite 8],7,[Seite 8]",
			"6,[Seite 8],6,[Tabelle]",
			"6,[Seite 8],7,[Tabelle]",
			"7,[Tabelle],7,[TAbelle]",
	})
	void testInstancesDiffer(int order1, String label1, int order2, String label2) {

		// arrange
		var s1 = new DerivateStruct(order1, label1);
		var s2 = new DerivateStruct(order2, label2);

		// act
		assertNotEquals(s1, s2);
	}
}
