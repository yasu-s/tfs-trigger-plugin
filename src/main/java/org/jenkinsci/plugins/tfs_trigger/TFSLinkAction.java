package org.jenkinsci.plugins.tfs_trigger;

import hudson.model.Action;

public class TFSLinkAction implements Action {

    private final String link;

    public TFSLinkAction(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }
}
