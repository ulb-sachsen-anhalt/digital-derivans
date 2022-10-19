package org.mycore.mets.model.header;

import org.jdom2.Element;
import org.mycore.mets.model.IMetsElement;

/**
 * The &lt;name&gt; element can be used to record the 
 * full name of the document agent.
 * 
 * @see <a href=
 *      "http://www.loc.gov/standards/mets/docs/mets.v1-8.html#name">name</a>
 * 
 * @author Uwe Hartwig (M3ssman)
 *
 */
public class Name implements IMetsElement {

    private String text;

    public Name() {
    }

    public Name(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.mets.model.IMetsElement#asElement()
     */
    @Override
    public Element asElement() {
        Element name = new Element("name", IMetsElement.METS);
        if (getText() != null) {
            name.setText(getText());
        }
        return name;
    }

}
