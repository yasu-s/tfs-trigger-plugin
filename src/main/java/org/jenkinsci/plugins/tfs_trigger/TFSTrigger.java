package org.jenkinsci.plugins.tfs_trigger;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Node;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jenkinsci.lib.xtrigger.AbstractTrigger;
import org.jenkinsci.lib.xtrigger.XTriggerDescriptor;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.tfs_trigger.service.TFSTriggerService;
import org.kohsuke.stapler.DataBoundConstructor;

import antlr.ANTLRException;

public class TFSTrigger extends AbstractTrigger {

    private final String nativeDirectory;
    private final String serverUrl;
    private final String userName;
    private final String userPassword;
    private ProjectLocation[] locations = new ProjectLocation[0];

    private int[] lastChangeSets = new int[0];

    @DataBoundConstructor
    public TFSTrigger(String nativeDirectory, String serverUrl, String userName, String userPassword,
                        List<ProjectLocation> locations, String cronTabSpec) throws ANTLRException {
        super(cronTabSpec);
        this.nativeDirectory = nativeDirectory;
        this.serverUrl       = serverUrl;
        this.userName        = userName;
        this.userPassword    = userPassword;
        this.locations       = locations.toArray(new ProjectLocation[locations.size()]);
        this.lastChangeSets  = new int[locations.size()];
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

        try {
            TFSTriggerService service = new TFSTriggerService();
            service.setNativeDirectory(nativeDirectory);
            service.setServerUrl(serverUrl);
            service.setUserName(userName);
            service.setUserPassword(userPassword);
            service.init();

            for (int i = 0; i < locations.length; i++) {
                if (checkIfModifiedLocation(service, i, log))
                    return true;
            }
        } catch (Exception ex) {
            throw new XTriggerException(ex);
        }

        return false;
    }

    private boolean checkIfModifiedLocation(TFSTriggerService service, int index, XTriggerLog log) throws XTriggerException {
        int changeSetId = service.getChangeSetID(locations[index].getProjectPath());
        log.info(locations[index].getProjectPath() + ":" + changeSetId);

        if (lastChangeSets[index] < changeSetId) {
            lastChangeSets[index] = changeSetId;
            return true;
        } else
            return false;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singleton(new TFSTriggerAction((AbstractProject<?,?>)job, getLogFile()));
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    @Extension
    public static class DescriptorImpl extends XTriggerDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.TFSTrigger_DisplayName();
        }
    }
}
