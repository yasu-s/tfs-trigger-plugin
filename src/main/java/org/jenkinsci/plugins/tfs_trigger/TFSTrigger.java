package org.jenkinsci.plugins.tfs_trigger;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Node;

import java.io.File;
import java.util.List;

import org.jenkinsci.lib.xtrigger.AbstractTrigger;
import org.jenkinsci.lib.xtrigger.XTriggerDescriptor;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.kohsuke.stapler.DataBoundConstructor;

import antlr.ANTLRException;

public class TFSTrigger extends AbstractTrigger {

    private final String nativeDirectory;
    private final String serverUrl;
    private final String userName;
    private final String userPassword;
    private ProjectLocation[] locations = new ProjectLocation[0];

    @DataBoundConstructor
    public TFSTrigger(String nativeDirectory, String serverUrl, String userName, String userPassword,
                        List<ProjectLocation> locations, String cronTabSpec) throws ANTLRException {
        super(cronTabSpec);
        this.nativeDirectory = nativeDirectory;
        this.serverUrl       = serverUrl;
        this.userName        = userName;
        this.userPassword    = userPassword;
        this.locations       = locations.toArray(new ProjectLocation[locations.size()]);
    }

    public String getNativeDirectory() {
        return nativeDirectory;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public ProjectLocation[] getLocations() {
        return locations;
    }

    @Override
    protected File getLogFile() {
        return new File(job.getRootDir(), "tfs-trigger-polling.log");
    }

    @Override
    protected boolean requiresWorkspaceForPolling() {
        return false;
    }

    @Override
    protected String getName() {
        return "TFSTrigger";
    }

    @Override
    protected String getCause() {
        return Messages.TFSTrigger_Cause();
    }

    @Override
    protected Action[] getScheduledActions(Node pollingNode, XTriggerLog log) {
        return new Action[0];
    }

    @Override
    protected boolean checkIfModified(Node pollingNode, XTriggerLog log) throws XTriggerException {

        if (locations == null || locations.length == 0) {
            log.info("No Loctation to poll.");
            return false;
        }

        for (ProjectLocation location : locations) {
            if (checkIfModifiedLocation(location, pollingNode, log))
                return true;
        }

        return false;
    }

    private boolean checkIfModifiedLocation(ProjectLocation location, Node pollingNode, XTriggerLog log) throws XTriggerException {
        return false;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends XTriggerDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.TFSTrigger_DisplayName();
        }
    }
}
