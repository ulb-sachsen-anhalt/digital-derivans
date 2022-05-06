package org.mycore.mets.model.header;

import org.jdom2.Element;
import org.mycore.mets.model.IMetsElement;

/**
 * The &lt;note&gt; element can be used to record any 
 * additional information regarding the agent's activities 
 * with respect to the METS document. 
 * 
 * @see <a href=
 *      "http://www.loc.gov/standards/mets/docs/mets.v1-8.html#note">metsHdr</a>
 * 
 * element can be used to
 * @author Uwe Hartwig (M3ssman)
 *
 */
public class Note implements IMetsElement {

    private String text;

    public Note() {
    }

    public Note(String text) {
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
    @SuppressWarnings("exports")
    @Override
    public Element asElement() {
        Element note = new Element("note", IMetsElement.METS);
        if (getText() != null) {
            note.setText(getText());
        }
        return note;
    }

}
