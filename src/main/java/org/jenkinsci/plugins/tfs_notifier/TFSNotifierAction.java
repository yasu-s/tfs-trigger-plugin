package org.jenkinsci.plugins.tfs_notifier;

import hudson.model.Action;

public class TFSNotifierAction implements Action {

    public final int changeSetID;
    public final String url;

    public TFSNotifierAction(int changeSetID, String url) {
        this.changeSetID = changeSetID;
        this.url         = url;
    }

    public int getChangeSetID() {
        return changeSetID;
    }

    public String getUrl() {
        return url;
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
