package org.jenkinsci.plugins.service;

import java.util.regex.Pattern;

import org.jenkinsci.plugins.service.TFSService;
import org.junit.Assert;
import org.junit.Test;

public class TFSServiceTest {

    @Test
    public void testIsPatterns() {
        Pattern[] patterns = new Pattern[1];
        patterns[0] = Pattern.compile("/MyProject/Sample.txt");
        boolean r = TFSService.isPatterns("/MyProject/Sample.txt", patterns);
        Assert.assertTrue(r);

        patterns[0] = Pattern.compile("/MyProject/.*\\.txt");
        r = TFSService.isPatterns("/MyProject/Sample.txt", patterns);
        Assert.assertTrue(r);

        r = TFSService.isPatterns("/MyProject/doc/Sample.txt", patterns);
        Assert.assertTrue(r);

        patterns[0] = Pattern.compile("/MyProject/doc/.*\\.txt");
        r = TFSService.isPatterns("/MyProject/doc/Sample.txt", patterns);
        Assert.assertTrue(r);

        r = TFSService.isPatterns("/MyProject/Sample.txt", patterns);
        Assert.assertFalse(r);

        r = TFSService.isPatterns("/MyProject/doc/Sample.java", patterns);
        Assert.assertFalse(r);

        patterns[0] = Pattern.compile("/.*\\.txt");
        r = TFSService.isPatterns("/MyProject/Sample.txt", patterns);
        Assert.assertTrue(r);

    }
}
