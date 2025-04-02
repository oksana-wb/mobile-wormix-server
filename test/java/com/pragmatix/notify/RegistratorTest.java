package com.pragmatix.notify;

import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;

public class RegistratorTest extends AbstractSpringTest {

    @Resource
    private Registrator registrator;

    @Test
    public void testRegistrate() throws Exception {
        registrator.registrate(876821L, (short) 11, "APA91bEXucRNc3aB0WruQ5yrzbjnzE3Hog9CUQJsPmFFd32nyMYyTsWCIt8qPOHJtCbPXVDq8o8dzEjI32u6kVr03xfVP-9zuhaIBv5kV3jc3nwbr_ya5xrWhsgBHXfQNxudM3CNSwFjJzTkLJRaUDni3MrYFvqq7Q");
    }
}