package org.jenkinsci.plugins.tfs_trigger.service;

import java.util.regex.Pattern;

import org.jenkinsci.plugins.tfs_trigger.service.TFSService;
import org.junit.Test;

public class TFSServiceTest {

    @Test
    public void testIsPatterns() {
        Pattern[] patterns = new Pattern[1];
        patterns[0] = Pattern.compile("$/MyProject/Sample.txt");
        boolean r = TFSService.isPatterns("$/MyProject/Sample.txt", patterns);
        Assert.assertTrue(r);

        patterns[0] = Pattern.compile("$/MyProject/*.txt");
        r = TFSService.isPatterns("$/MyProject/Sample.txt", patterns);
        Assert.assertTrue(r);


    }
}
