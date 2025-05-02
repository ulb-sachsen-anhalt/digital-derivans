package de.ulb.digital.derivans.model;

import java.util.ArrayList;
import java.util.List;

/**
* Describe structural part of Derivate
* with the very first page set = 1  
*
* @author u.hartwig
*/
public class DerivateStruct {

    private String label;

    private int order = 1;

    private List<DerivateStruct> children = new ArrayList<>();

    private List<DigitalPage> pages = new ArrayList<>();

    public DerivateStruct(int order, String label) {
        this.order = order;
        this.label = label;
    }

    public DerivateStruct(int order, String label, List<DerivateStruct> subStructs) {
        this.order = order;
        this.label = label;
        this.children = subStructs;
    }

    public String getLabel() {
        return this.label;
    }

    public int getOrder() {
        return this.order;
    }

    public List<DigitalPage> getPages(){
        return this.pages;
    }

    public List<DerivateStruct> getChildren() {
        return this.children;
    }

    @Override
    public boolean equals(Object other) {
        if(! (other instanceof DerivateStruct)) {
            return false;
        } else {
            DerivateStruct otherStruct = (DerivateStruct) other;
            if (otherStruct.getOrder() != this.getOrder()) {
                return false;
            }
            return otherStruct.getLabel().equals(this.label);
        }
    }

}