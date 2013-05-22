package org.jenkinsci.plugins.tfs_trigger;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.xtrigger.AbstractTrigger;
import org.jenkinsci.lib.xtrigger.XTriggerDescriptor;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.service.TFSService;
import org.jenkinsci.plugins.util.Constants;
import org.jenkinsci.plugins.util.ProjectLocation;
import org.jenkinsci.plugins.util.TFSUtil;
import org.kohsuke.stapler.DataBoundConstructor;

import antlr.ANTLRException;

public class TFSTrigger extends AbstractTrigger {

    private final String version;
    private final String serverUrl;
    private final String projectCollection;
    private final String project;
    private final String userName;
    private final String userPassword;
    private ProjectLocation[] locations = new ProjectLocation[0];
    private final String excludedRegions;
    private final String includedRegions;
    private List<TFSLinkAction> actions = new ArrayList<TFSLinkAction>();

    @DataBoundConstructor
    public TFSTrigger(String version, String serverUrl, String projectCollection, String project,
                        String userName, String userPassword, List<ProjectLocation> locations,
                        String cronTabSpec, String excludedRegions, String includedRegions) throws ANTLRException {
        super(cronTabSpec);
        this.version           = StringUtils.isBlank(version) ? Constants.VERSION_2012_2 : version;
        this.serverUrl         = serverUrl;
        this.projectCollection = projectCollection;
        this.project           = project;
        this.userName          = userName;
        this.userPassword      = userPassword;
        this.locations         = locations.toArray(new ProjectLocation[locations.size()]);
        this.excludedRegions   = excludedRegions;
        this.includedRegions   = includedRegions;
    }

    public String getVersion() {
        return version;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getProjectCollection() {
        return projectCollection;
    }

    public String getProject() {
        return project;
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

    public String getExcludedRegions() {
        return excludedRegions;
    }

    public String[] getExcludedRegionsNormalized() {
        return StringUtils.isBlank(excludedRegions) ? null : excludedRegions.split("[\\r\\n]+");
    }

    public Pattern[] getExcludedRegionsPatterns() {
        String[] excluded = getExcludedRegionsNormalized();
        if (excluded != null) {
            Pattern[] patterns = new Pattern[excluded.length];

            int i = 0;
            for (String excludedRegion : excluded) {
                patterns[i++] = Pattern.compile(excludedRegion);
            }

            return patterns;
        }

        return new Pattern[0];
    }

    public String getIncludedRegions() {
        return includedRegions;
    }

    public String[] getIncludedRegionsNormalized() {
        return StringUtils.isBlank(includedRegions) ? null : includedRegions.split("[\\r\\n]+");
    }

    public Pattern[] getIncludedRegionsPatterns() {
        String[] included = getIncludedRegionsNormalized();
        if (included != null) {
            Pattern[] patterns = new Pattern[included.length];

            int i = 0;
            for (String includedRegion : included) {
                patterns[i++] = Pattern.compile(includedRegion);
            }

            return patterns;
        }

        return new Pattern[0];
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
        return Messages.TFSTrigger_Cause(actions.size() == 0 ? "" : actions.get(0).getLink());
    }

    @Override
    protected Action[] getScheduledActions(Node pollingNode, XTriggerLog log) {
        return actions.toArray(new TFSLinkAction[actions.size()]);
    }

    @Override
    protected boolean checkIfModified(Node pollingNode, XTriggerLog log) throws XTriggerException {
        if (actions == null)
            actions = new ArrayList<TFSLinkAction>();
        else
            actions.clear();

        if (locations == null || locations.length == 0) {
            log.info("No Loctation to poll.");
            return false;
        }

        boolean modified = false;
        TFSService service =null;

        try {
            Map<String, Integer> changeSets = TFSUtil.parseChangeSetFile(getChangeSetFilePath(), locations);
            Pattern[] excludedPatterns = getExcludedRegionsPatterns();
            Pattern[] includedPatterns = getIncludedRegionsPatterns();
            service = createTFSService();

            for (ProjectLocation location : locations) {
                if (checkIfModifiedLocation(service, location.getProjectPath(), log, changeSets, excludedPatterns, includedPatterns)) {
                    modified = true;
                }
            }

            TFSUtil.saveChangeSetFile(getChangeSetFilePath(), changeSets);

            if (modified) {
                String link = TFSUtil.createChangeSetLink(version, serverUrl, projectCollection, project, userName, userPassword, locations, getChangeSetFilePath());
                TFSLinkAction action = new TFSLinkAction(link);
                actions.add(action);
            }
        } catch (Exception ex) {
            throw new XTriggerException(ex);
        } finally {
            if (service != null) service.close();
        }

        return modified;
    }

    private TFSService createTFSService() {
        TFSService service = new TFSService(serverUrl, userName, userPassword);
        return service;
    }

    private boolean checkIfModifiedLocation(TFSService service, String path, XTriggerLog log, Map<String, Integer> changeSets,
            Pattern[] excludedPatterns, Pattern[] includedPatterns) throws XTriggerException {
        int changeSetID = service.getChangeSetID(path, excludedPatterns, includedPatterns);
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

    public File getChangeSetFilePath() {
        return new File(job.getRootDir(), "changeSet.txt");
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
