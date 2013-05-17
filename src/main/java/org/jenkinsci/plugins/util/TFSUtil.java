package org.jenkinsci.plugins.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class TFSUtil {

    public static void setNativeDirectory(String nativeDirectory) {
        System.setProperty(Constants.PROPERTY_NAME_NATIVE_DIRECTORY, nativeDirectory);
    }

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
}
