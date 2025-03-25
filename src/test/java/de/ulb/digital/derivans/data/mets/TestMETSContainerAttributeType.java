package de.ulb.digital.derivans.data.mets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansRuntimeException;

/**
 * 
 * @author u.hartwig
 * 
 */
class TestMETSContainerAttributeType {

	@Test
	void ensureMatch() {
		var match = METSContainerAttributeType.valueOf("TYPE");
		assertEquals(METSContainerAttributeType.TYPE, match);
	}

	@Test
	void ensureMissmatch() {
		var exc = assertThrows(DigitalDerivansRuntimeException.class, () -> METSContainerAttributeType.get("FOO"));
		assertEquals("unknown mets:div@TYPE FOO", exc.getMessage());
	}
}
