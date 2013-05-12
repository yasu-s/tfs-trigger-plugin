package org.jenkinsci.plugins.tfs_trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    private final String projectCollection;
    private final String userName;
    private final String userPassword;
    private ProjectLocation[] locations = new ProjectLocation[0];

    @DataBoundConstructor
    public TFSTrigger(String nativeDirectory, String serverUrl, String projectCollection, String userName, String userPassword,
                        List<ProjectLocation> locations, String cronTabSpec) throws ANTLRException {
        super(cronTabSpec);
        this.nativeDirectory   = nativeDirectory;
        this.serverUrl         = serverUrl;
        this.projectCollection = projectCollection;
        this.userName          = userName;
        this.userPassword      = userPassword;
        this.locations         = locations.toArray(new ProjectLocation[locations.size()]);
    }

    public String getNativeDirectory() {
        return nativeDirectory;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getProjectCollection() {
        return projectCollection;
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
        StringBuilder sb = new StringBuilder();
        int cnt = 0;
        try {
            Map<String, Integer> changeSets = parseChangeSetFile();
            for (Entry<String, Integer> entry : changeSets.entrySet()) {
                sb.append(String.format("%1$d. %2$s ", ++cnt, entry.getKey()));
                sb.append(String.format("(<a href=\"%1$s%2$s/_versionControl/changeset/%3$d\">%4$s: %3$d</a>)", serverUrl, projectCollection, entry.getValue(), Messages.ChangeSet()));
                sb.append("<br />");
            }
        } catch (Exception e) {
        }
        return Messages.TFSTrigger_Cause(sb.toString());
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

        boolean modified = false;

        try {
            Map<String, Integer> changeSets = parseChangeSetFile();

            TFSTriggerService service = new TFSTriggerService();
            service.setNativeDirectory(nativeDirectory);
            service.setServerUrl(serverUrl);
            service.setUserName(userName);
            service.setUserPassword(userPassword);
            service.init();

            for (ProjectLocation location : locations) {
                if (checkIfModifiedLocation(service, location.getProjectPath(), log, changeSets)) {
                    modified = true;
                }
            }

            saveChangeSetFile(changeSets);
        } catch (Exception ex) {
            throw new XTriggerException(ex);
        }

        return modified;
    }

    private boolean checkIfModifiedLocation(TFSTriggerService service, String path, XTriggerLog log, Map<String, Integer> changeSets) throws XTriggerException {
        int changeSetID = service.getChangeSetID(path);
        int lastChangeSetID = 0;

        if (changeSets.containsKey(path))
            lastChangeSetID = changeSets.get(path);

        if (lastChangeSetID < changeSetID) {
            if (lastChangeSetID > 0)
                log.info(path + ": " + lastChangeSetID + " -> " + changeSetID);
            else
                log.info(path + ": " + changeSetID);

            changeSets.put(path, changeSetID);
            return true;
        } else {
            log.info(path + ": " + changeSetID);
            return false;
        }
    }

    public File getChangeSetFile() {
        return new File(job.getRootDir(), "changeSet.txt");
    }

    private Map<String, Integer> parseChangeSetFile() throws IOException {
        Map<String, Integer> changeSets = new HashMap<String, Integer>();
        File file = getChangeSetFile();
        if (!file.exists())
            return changeSets;

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));

            String line;
            while ((line = br.readLine()) != null) {
                int index = line.lastIndexOf('/');

                if (index < 0)
                    continue;

                try {
                    String path = line.substring(0, index);
                    int changeSetID = Integer.parseInt(line.substring(index + 1));
                    changeSets.put(path, changeSetID);
                } catch (NumberFormatException ex) {
                }
            }
        } finally {
            if (br != null) br.close();
        }

        return changeSets;
    }

    private void saveChangeSetFile(Map<String, Integer> changeSets) throws IOException, InterruptedException  {
        PrintWriter w = null;
        try {
            w = new PrintWriter(new FileOutputStream(getChangeSetFile()));
            for (Entry<String, Integer> entry : changeSets.entrySet()) {
                w.println(entry.getKey() + "/" + entry.getValue());
            }
        } finally {
            if (w != null) w.close();
        }
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
