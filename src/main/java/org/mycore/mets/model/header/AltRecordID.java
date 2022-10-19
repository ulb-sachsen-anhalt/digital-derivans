package org.mycore.mets.model.header;

import org.jdom2.Element;
import org.mycore.mets.model.IMetsElement;

/**
 * 
 * The alternative record identifier element &lt;altRecordID&gt; 
 * allows one to use alternative record identifier values 
 * for the digital object represented by the METS document;
 * the primary record identifier is stored in the OBJID 
 * attribute in the root &lt;mets&gt; element.
 * 
 * @see <a href=
 *      "http://www.loc.gov/standards/mets/docs/mets.v1-8.html#altRecordID">altRecordID</a>
 * 
 * @author Uwe Hartwig (M3ssman)
 *
 */
public class AltRecordID implements IMetsElement {

    private String id;

    private String type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.mets.model.IMetsElement#asElement()
     */
    @Override
    public Element asElement() {
        Element altRecord = new Element("altRecordID", IMetsElement.METS);
        if (getId() != null) {
            altRecord.setAttribute("ID", getId());
        }
        if (getType() != null) {
            altRecord.setAttribute("TYPE", this.getType());
        }
        return altRecord;
    }
}
