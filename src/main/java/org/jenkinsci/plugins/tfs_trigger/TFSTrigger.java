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

    private final String serverUrl;
    private final String userName;
    private final String userPassword;
    private ProjectLocation[] locations = new ProjectLocation[0];

    @DataBoundConstructor
    public TFSTrigger(String serverUrl, String userName, String userPassword, List<ProjectLocation> locations,
                        String cronTabSpec) throws ANTLRException {
        super(cronTabSpec);
        this.serverUrl    = serverUrl;
        this.userName     = userName;
        this.userPassword = userPassword;
        this.locations    = locations.toArray(new ProjectLocation[locations.size()]);
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
        // TODO 自動生成されたメソッド・スタブ
        return null;
    }

    @Override
    protected boolean requiresWorkspaceForPolling() {
        // TODO 自動生成されたメソッド・スタブ
        return false;
    }

    @Override
    protected String getName() {
        // TODO 自動生成されたメソッド・スタブ
        return null;
    }

    @Override
    protected Action[] getScheduledActions(Node pollingNode, XTriggerLog log) {
        // TODO 自動生成されたメソッド・スタブ
        return null;
    }

    @Override
    protected boolean checkIfModified(Node pollingNode, XTriggerLog log)
            throws XTriggerException {
        // TODO 自動生成されたメソッド・スタブ
        return false;
    }

    @Override
    protected String getCause() {
        // TODO 自動生成されたメソッド・スタブ
        return null;
    }

    @Extension
    public static class DescriptorImpl extends XTriggerDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.TFSTrigger_DisplayName();
        }
    }
}
