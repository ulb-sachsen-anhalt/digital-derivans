package de.ulb.digital.derivans.data.ocr;

import java.util.function.Predicate;

/**
 * 
 * A text is considered to contain at least a single 
 * alpha numerical char.
 * 
 * Therefore, tokens like "/", "⸗" or " " <b>do not 
 * form a valid text token</b>.
 * 
 * @author u.hartwig
 *
 */
public class ValidTextPredicate implements Predicate<String> {

	@Override
	public boolean test(String t) {
		if ((t == null) || (t.isBlank())) {
			return false;
		}
		
		for(char c : t.toCharArray()) {
			if(Character.isAlphabetic(c) || Character.isDigit(c)) {
				return true;
			}
		}
		
		return false;
	}
	
}