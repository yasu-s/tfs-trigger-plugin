package org.jenkinsci.plugins.tfs_trigger.service;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.link.Hyperlink;
import com.microsoft.tfs.core.clients.workitem.link.LinkFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.URIUtils;

public class TFSTriggerService {

    private static final String PROPERTY_NAME_NATIVE_DIRECTORY = "com.microsoft.tfs.jni.native.base-directory";
    private static final String WORK_ITEM_FIELDS_NAME_HISTORY = "System.History";

    private String nativeDirectory;
    private String serverUrl;
    private String userName;
    private String userPassword;

    private TFSTeamProjectCollection tfsCollection;
    private VersionControlClient versionClient;
    private WorkItemClient workItemClient;

    public String getNativeDirectory() {
        return nativeDirectory;
    }

    public void setNativeDirectory(String nativeDirectory) {
        this.nativeDirectory = nativeDirectory;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }


    public void init() {
        System.setProperty(PROPERTY_NAME_NATIVE_DIRECTORY, nativeDirectory);
        Credentials credentials = new UsernamePasswordCredentials(userName, userPassword);
        tfsCollection  = new TFSTeamProjectCollection(URIUtils.newURI(serverUrl), credentials);
        versionClient  = tfsCollection.getVersionControlClient();
        workItemClient = tfsCollection.getWorkItemClient();
    }

    public int getChangeSetID(String path) {
        Item item = versionClient.getItem(path);
        int changeSetID = item.getChangeSetID();

        if (item.getItemType() == ItemType.FOLDER) {
            for (Item i : versionClient.getItems(path, RecursionType.FULL).getItems()) {
                if (changeSetID < i.getChangeSetID())
                    changeSetID = i.getChangeSetID();
            }
        }

        return changeSetID;
    }

    public List<Integer> getWorkItemIDs(int changeSetID) {
        Changeset changeSet = versionClient.getChangeset(changeSetID);
        List<Integer> ids = new ArrayList<Integer>();
        for (WorkItem workItem : changeSet.getWorkItems(workItemClient)) {
            ids.add(workItem.getID());
        }
        return ids;
    }

    public void addHyperlink(int workItemID, String url, String comment, String history) {
        WorkItem item = workItemClient.getWorkItemByID(workItemID);
        item.open();
        Hyperlink link = LinkFactory.newHyperlink(url, comment, true);
        item.getLinks().add(link);
        item.getFields().getField(WORK_ITEM_FIELDS_NAME_HISTORY).setValue(history);
        item.save();
    }
}
