package com.pragmatix.app.services;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.testcase.AbstractSpringTest;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 07.12.2017 12:10
 */
public class PaymentServiceTest extends AbstractSpringTest {

    @Resource
    PaymentService paymentService;

    @Test
    public void buyCraftBoxTest() {
        UserProfile profile = getProfile(testerProfileId);
        profile.setStuff(new short[0]);
        profile.setStuff(new short[]{(short) 5006, (short) 5003, (short) 5007, (short) 5001, (short) 5004, (short) 5008, (short) 5005});
//        profile.setStuff(new short[]{(short) 5002, (short) 5006, (short) 5003, (short) 5007, (short) 5001, (short) 5004, (short) 5008, (short) 5005});
        craftService.getReagentsForProfile(profile.getId()).clean();

        IntStream.rangeClosed(1, 10).forEach(i -> {
//            List<GenericAwardStructure> awards = paymentService.purchaseBundle(profile, RandomStringUtils.randomAlphanumeric(32), new Date(), "craft_box_1")._2;
//            println(awards);
//
            List<GenericAwardStructure> awards = paymentService.purchaseBundle(profile, RandomStringUtils.randomAlphanumeric(32), new Date(), "craft_box_2")._2;
            println(awards);
            println(Arrays.toString(profile.getStuff()));
        });

    }

}