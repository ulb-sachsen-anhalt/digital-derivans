package de.ulb.digital.derivans.model.ocr;

import java.awt.Rectangle;
import java.awt.geom.Path2D.Float;

/**
 * Mark character tokens with planar representation
 */
public interface OCRToken {
    
    /**
     * Get textual representation
     * @return
     */
    String getText();


    /**
     * Get complete planar representation
     */
    // Float getShape();

    /**
     * 
     * Get simplified rectangular shape of token
     * 
     * @return
     */
    Rectangle getBox();

    /**
     * 
     * Descritive Label
     * 
     * @return
     */
    String getLabel();
}
