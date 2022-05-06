package org.mycore.mets.model.header;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.mycore.mets.model.IMetsElement;

/**
 * 
 * The &lt;agent&gt; element provides for various parties and 
 * their roles with respect to the METS record to be documented.
 * 
 * @see <a href=
 *      "http://www.loc.gov/standards/mets/docs/mets.v1-8.html#agent">agent</a>
 * 
 * @author Uwe Hartwig (M3ssman)
 *
 */
public class Agent implements IMetsElement {

    private String id;

    private String role;

    private String otherRole;

    private String type;

    private String otherType;

    private Name name;

    private List<Note> notes;

    /**
     * 
     * Set required Attribute "ROLE" per default to "OTHER"
     * 
     */
    public Agent() {
        this.role = "OTHER";
        this.notes = new ArrayList<>();
    }

    /**
     * 
     * Attribute "ROLE" is required only
     * 
     * @param role
     * @param name
     */
    public Agent(String role, Name name) {
        this.role = role;
        this.name = name;
        this.notes = new ArrayList<>();
    }

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOtherRole() {
        return otherRole;
    }

    public void setOtherRole(String otherRole) {
        this.otherRole = otherRole;
    }

    public String getOtherType() {
        return otherType;
    }

    public void setOtherType(String otherType) {
        this.otherType = otherType;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.mets.model.IMetsElement#asElement()
     */
    @SuppressWarnings("exports")
    @Override
    public Element asElement() {
        Element agent = new Element("agent", IMetsElement.METS);
        if (getRole() != null) {
            agent.setAttribute("ROLE", getRole());
        }
        if (getOtherRole() != null) {
            agent.setAttribute("OTHERROLE", getOtherRole());
        }
        if (this.getType() != null) {
            agent.setAttribute("TYPE", this.getType());
        }
        if (getOtherType() != null) {
            agent.setAttribute("OTHERTYPE", getOtherType());
        }
        if (getId() != null) {
            agent.setAttribute("ID", getId());
        }
        if (getName() != null) {
            agent.addContent(getName().asElement());
        }
        if (getNotes() != null && !getNotes().isEmpty()) {
            for (Note note : getNotes()) {
                agent.addContent(note.asElement());
            }
        }
        return agent;
    }

}
