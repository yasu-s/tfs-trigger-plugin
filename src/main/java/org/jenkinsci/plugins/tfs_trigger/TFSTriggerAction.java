package org.jenkinsci.plugins.tfs_trigger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.jelly.XMLOutput;

import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractProject;
import hudson.model.Action;

public class TFSTriggerAction implements Action {

    private transient AbstractProject<?, ?> job;

    private transient File logFile;

    public TFSTriggerAction(AbstractProject<?, ?> job, File logFile) {
        this.job     = job;
        this.logFile = logFile;
    }

    public AbstractProject<?, ?> getOwner() {
        return job;
    }

    public File getLogFile() {
        return logFile;
    }

    public String getIconFileName() {
        return "clipboard.gif";
    }

    public String getDisplayName() {
        return Messages.TFSTriggerAction_DisplayName();
    }

    public String getUrlName() {
        return "tfsPollLog";
    }

    public String getLog() throws IOException {
        return Util.loadFile(getLogFile());
    }

    public void writeLogTo(XMLOutput out) throws IOException {
        new AnnotatedLargeText<TFSTriggerAction>(getLogFile(), Charset.defaultCharset(), true, this).writeHtmlTo(0, out.asWriter());
    }
}
