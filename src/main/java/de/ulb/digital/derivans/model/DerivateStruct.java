package de.ulb.digital.derivans.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author u.hartwig
 */
class DerivateStruct {

    String label;

    int order;

    List<DerivateStruct> children = new ArrayList<>();

    List<DigitalPage> pages = new ArrayList<>();

}