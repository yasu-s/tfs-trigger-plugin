package org.jenkinsci.plugins.tfs_notifier;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jenkinsci.plugins.service.TFSService;
import org.jenkinsci.plugins.tfs_trigger.TFSTrigger;
import org.jenkinsci.plugins.util.ProjectLocation;
import org.jenkinsci.plugins.util.TFSUtil;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

public class TFSNotifier extends Notifier {

    @DataBoundConstructor
    public TFSNotifier() {

    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        try {
            TFSTrigger trigger = (TFSTrigger)build.getProject().getTrigger(TFSTrigger.class);
            TFSService service= new TFSService(trigger.getServerUrl(), trigger.getUserName(), trigger.getUserPassword());

            Map<String, Integer> changeSets = TFSUtil.parseChangeSetFile(getChangeSetFile(build), trigger.getLocations());
            Pattern[] excludedPatterns = trigger.getExcludedRegionsPatterns();
            Pattern[] includedPatterns = trigger.getIncludedRegionsPatterns();

            boolean modified = false;
            int currentChangeSetID = 0;
            for (ProjectLocation location : trigger.getLocations()) {
                int changeSetID = service.getChangeSetID(location.getProjectPath(), excludedPatterns, includedPatterns);

                if (!changeSets.containsKey(location.getProjectPath()) || changeSets.get(location.getProjectPath()) < changeSetID) {
                    currentChangeSetID = changeSetID;
                    modified = true;
                }

                changeSets.put(location.getProjectPath(), changeSetID);
            }

            listener.getLogger().println("ChangeSet: " + currentChangeSetID);

            if (modified) {
                List<Integer> workItemIDs = service.getWorkItemIDs(currentChangeSetID);
                if (workItemIDs != null && workItemIDs.size() > 0) {
                    for (int workItemID : workItemIDs) {
                        listener.getLogger().println("WorkItem: " + workItemID);

                        String color = "black";
                        if (build.getResult() == Result.SUCCESS)
                            color = "blue";
                        else if (build.getResult() == Result.UNSTABLE)
                            color = "orange";
                        else if (build.getResult() == Result.FAILURE)
                            color = "red";

                        String url = hudson.tasks.Mailer.descriptor().getUrl() + build.getUrl();
                        String history = String.format("Jenkins-CI <a href=\"%s\">%s #%d</a> <font style=\"color:%s; font-weight: bold;\">%s</font>", url, build.getProject().getDisplayName(), build.getNumber(), color, build.getResult());
                        String comment = String.format("Jenkins-CI %s #%d %s", build.getProject().getDisplayName(), build.getNumber(), build.getResult());

                        service.addHyperlink(workItemID, url, comment, history);
                    }
                }

                TFSUtil.saveChangeSetFile(getChangeSetFile(build), changeSets);
            }
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
        }
        return true;
    }

    public File getChangeSetFile(AbstractBuild<?, ?> build) {
        return new File(build.getProject().getRootDir(), "changeSet_Notify.txt");
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
         return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(TFSNotifier.class);
        }

        @Override
        public String getDisplayName() {
            return Messages.TFSNotifier_Descriptor_DisplayName();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
