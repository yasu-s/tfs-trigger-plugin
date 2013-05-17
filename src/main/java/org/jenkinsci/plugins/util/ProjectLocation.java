package org.jenkinsci.plugins.util;

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class ProjectLocation implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String projectPath;

    @DataBoundConstructor
    public ProjectLocation(String projectPath) {
        this.projectPath    = projectPath;
    }

    public String getProjectPath() {
        return projectPath;
    }
}
