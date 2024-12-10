package de.ulb.digital.derivans.model;

/**
 * Basic textual/character token
 * 
 * @author hartwig
 * 
 */
public interface ITextElement {

    /**
     * Get textual UTF-8 representation
     * 
     * @return
     */
    String getText();

    /**
     * 
     * Distinguish between western european left (LTR)
     * and arabic/persian/hebrew right (RTL) text orientation
     * by inspecting the very *FIRST* character.
     * 
     * Please note:
     * Proper recognition only with UTF-16 chars,
     * therefore *not possible* for
     * arabic math (U+1ee00 ARABIC MATHEMATICAL ALEF)
     * pahlavi (U+10b60 INSCRIPTIONAL PAHLAVI LETTER ALEPH)
     * 
     * @return boolean
     */
    default boolean isRTL() {
        if (!this.getText().isEmpty()) {
            var code = getText().codePointAt(0);
            // arabic basic
            if (code >= 0x600 && code <= 0x6ff) {
                return true;
            }
            // arabic extension
            if (code >= 0x750 && code <= 0x77f) {
                return true;
            }
            // arabic extension A
            if (code >= 0x8a0 && code <= 0x8ff) {
                return true;
            }
            // hebrew
            if (code >= 0x590 && code <= 0x5ff) {
                return true;
            }
        }
        return false;
    }

}
