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
import java.util.Map;
import java.util.Map.Entry;
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

    private final String nativeDirectory;
    private final String version;
    private final String serverUrl;
    private final String projectCollection;
    private final String project;
    private final String userName;
    private final String userPassword;
    private ProjectLocation[] locations = new ProjectLocation[0];
    private final String excludedRegions;
    private final String includedRegions;

    @DataBoundConstructor
    public TFSTrigger(String nativeDirectory, String version, String serverUrl, String projectCollection, String project,
                        String userName, String userPassword, List<ProjectLocation> locations,
                        String cronTabSpec, String excludedRegions, String includedRegions) throws ANTLRException {
        super(cronTabSpec);
        this.nativeDirectory   = nativeDirectory;
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

    public String getNativeDirectory() {
        return nativeDirectory;
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
        StringBuilder sb = new StringBuilder();
        int cnt = 0;
        try {
            Map<String, Integer> changeSets = TFSUtil.parseChangeSetFile(getChangeSetFilePath(), locations);
            TFSService service = createTFSService();

            for (Entry<String, Integer> entry : changeSets.entrySet()) {
                sb.append(String.format("%1$d. %2$s ", ++cnt, entry.getKey()));

                String url = TFSUtil.getChangeSetUrl(version, serverUrl, projectCollection, project, entry.getValue());
                sb.append(String.format("(%s: <a href=\"%s\">%d</a>)", Messages.ChangeSet(), url, entry.getValue()));

                List<Integer> workItemIDs = service.getWorkItemIDs(entry.getValue());
                if (workItemIDs != null && workItemIDs.size() > 0) {
                    sb.append(" (" + Messages.WorkItem() + ": ");
                    boolean first = true;
                    for (int workItemID : workItemIDs) {
                        if (!first) sb.append(", ");
                        sb.append(String.format("<a href=\"%s\">%d</a>", TFSUtil.getWorkItemUrl(serverUrl, projectCollection, project, workItemID), workItemID));
                        first = false;
                    }
                    sb.append(")");
                }

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
            Map<String, Integer> changeSets = TFSUtil.parseChangeSetFile(getChangeSetFilePath(), locations);
            Pattern[] excludedPatterns = getExcludedRegionsPatterns();
            Pattern[] includedPatterns = getIncludedRegionsPatterns();
            TFSService service = createTFSService();

            for (ProjectLocation location : locations) {
                if (checkIfModifiedLocation(service, location.getProjectPath(), log, changeSets, excludedPatterns, includedPatterns)) {
                    modified = true;
                }
            }

            TFSUtil.saveChangeSetFile(getChangeSetFilePath(), changeSets);
        } catch (Exception ex) {
            throw new XTriggerException(ex);
        }

        return modified;
    }

    private TFSService createTFSService() {
        TFSUtil.setNativeDirectory(nativeDirectory);
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
