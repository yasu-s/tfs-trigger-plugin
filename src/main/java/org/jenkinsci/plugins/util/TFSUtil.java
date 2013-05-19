package org.jenkinsci.plugins.util;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jenkinsci.plugins.service.TFSService;
import org.jenkinsci.plugins.tfs_trigger.Messages;
import org.jenkinsci.plugins.tfs_trigger.TFSLinkAction;

public abstract class TFSUtil {

    public static String getChangeSetUrl(String version, String serverUrl, String projectCollection, String project, int changeSetID) {
        if (Constants.VERSION_2012_2.equals(version))
            return String.format("%s%s/%s/_versionControl/changesets#cs=%d", serverUrl, projectCollection, project, changeSetID);
        else
            return String.format("%s%s/%s/_versionControl/changeset/%d", serverUrl, projectCollection, project, changeSetID);
    }

    public static String getWorkItemUrl(String serverUrl, String projectCollection, String project, int workItemID) {
        return String.format("%s%s/%s/_workitems#id=%d&_a=edit", serverUrl, projectCollection, project, workItemID);
    }

    public static Map<String, Integer> parseChangeSetFile(File file, ProjectLocation[] locations) throws IOException {
        Map<String, Integer> changeSets = new HashMap<String, Integer>();

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
                    boolean check = false;
                    for (ProjectLocation location : locations) {
                        if (location.getProjectPath().equals(path)) {
                            check = true;
                            break;
                        }
                    }
                    if (!check) continue;
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

    public static void saveChangeSetFile(File file, Map<String, Integer> changeSets) throws IOException, InterruptedException  {
        PrintWriter w = null;
        try {
            w = new PrintWriter(new FileOutputStream(file));
            for (Entry<String, Integer> entry : changeSets.entrySet()) {
                w.println(entry.getKey() + "/" + entry.getValue());
            }
        } finally {
            if (w != null) w.close();
        }
    }

    public static String createChangeSetLink(String version, String serverUrl, String projectCollection, String project,
            String userName, String userPassword, ProjectLocation[] locations, File changeSetFile) {
        StringBuilder sb = new StringBuilder();
        int cnt = 0;
        try {
            Map<String, Integer> changeSets = parseChangeSetFile(changeSetFile, locations);
            TFSService service = new TFSService(serverUrl, userName, userPassword);

            for (Entry<String, Integer> entry : changeSets.entrySet()) {
                sb.append(String.format("%1$d. %2$s ", ++cnt, entry.getKey()));

                String url = getChangeSetUrl(version, serverUrl, projectCollection, project, entry.getValue());
                sb.append(String.format("(%s: <a href=\"%s\">%d</a>)", Messages.ChangeSet(), url, entry.getValue()));

                List<Integer> workItemIDs = service.getWorkItemIDs(entry.getValue());
                if (workItemIDs != null && workItemIDs.size() > 0) {
                    sb.append(" (" + Messages.WorkItem() + ": ");
                    boolean first = true;
                    for (int workItemID : workItemIDs) {
                        if (!first) sb.append(", ");
                        sb.append(String.format("<a href=\"%s\">%d</a>", getWorkItemUrl(serverUrl, projectCollection, project, workItemID), workItemID));
                        first = false;
                    }
                    sb.append(")");
                }

                sb.append("<br />\n");
            }
        } catch (Exception e) {
        }
        return sb.toString();
    }

    public static String getLastChangeSetLink(AbstractProject<?, ?> project) {
        AbstractBuild<?, ?> build = project.getLastBuild();
        if (build == null)
            return "";

        TFSLinkAction action = build.getAction(TFSLinkAction.class);
        while (action == null) {
            build = build.getPreviousBuild();
            if (build == null) break;
            action = build.getAction(TFSLinkAction.class);
        }

        return action == null ? "" : action.getLink();
    }
}
