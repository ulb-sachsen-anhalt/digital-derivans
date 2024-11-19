package de.ulb.digital.derivans.model;

import java.awt.geom.Rectangle2D;

/**
 * 
 * Basic visible element
 * 
 * @author hartwig
 * 
 */
public interface IVisualElement {

    /**
     * 
     * Get simplified rectangular shape of token
     * 
     * @return
     */
    Rectangle2D getBox();


    /**
     * 
     * Set new box
     * 
     * @param rectangle
     */
    void setBox(Rectangle2D rectangle);

    /**
     * 
     * Scale box by ratio from upper left
     * 
     * @param ratio
     */
	default void scale(float ratio) {
        Rectangle2D rect = this.getBox();
        var newX = Math.round(rect.getX() * ratio);
        var newY = Math.round(rect.getY() * ratio);
        var newW = Math.round(rect.getWidth() * ratio);
        var newH = Math.round(rect.getHeight() * ratio);
        this.setBox(new Rectangle2D.Float(newX, newY, newW, newH));
    }
}
