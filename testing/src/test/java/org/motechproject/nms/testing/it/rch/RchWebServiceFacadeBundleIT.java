package org.motechproject.nms.testing.it.rch;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.nms.imi.service.SettingsService;
import org.motechproject.nms.rch.service.RchWebServiceFacade;
import org.motechproject.nms.region.domain.*;
import org.motechproject.nms.region.repository.DistrictDataService;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.testing.it.rch.util.*;
import org.motechproject.nms.testing.service.TestingService;
import org.motechproject.scheduler.service.MotechSchedulerService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.motechproject.testing.osgi.http.SimpleHttpServer;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.service.http.NamespaceException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class RchWebServiceFacadeBundleIT extends BasePaxIT {

    @Inject
    private RchWebServiceFacade rchWebServiceFacade;

    @Inject
    private MotechSchedulerService schedulerService;

    @Inject
    private StateDataService stateDataService;

    @Inject
    private DistrictDataService districtDataService;

    @Inject
    private TestingService testingService;

    @Inject
    private SettingsService settingsService;

    @Before
    public void setUp() throws ServletException, NamespaceException {
        testingService.clearDatabase();
        State state = stateDataService.create(new State("My State", 21L));

        District district = new District();
        district.setCode(4L);
        district.setState(state);
        district.setName("District_Name 4");
        district.setRegionalName("Regional Name 4");

        Block block = new Block();
        block.setRegionalName("Block Regional");
        block.setName("Taluka_Name 1");
        block.setCode((long)0046);
        block.setIdentity(55);

        block.setDistrict(district);
        district.setBlocks(new ArrayList<>(singletonList(block)));

        Panchayat healthBlock = new Panchayat();
        healthBlock.setVcode(113L);
        healthBlock.setName("HealthBlock_Name 1");
        healthBlock.setRegionalName("HB1");

        healthBlock.setBlock(block);
        block.setPanchayats(new ArrayList<>(singletonList(healthBlock)));


        districtDataService.create(district);

//        SubscriptionPack pregnancyPack = new SubscriptionPack("prg", SubscriptionPackType.PREGNANCY, 70, 10,
//                Collections.<SubscriptionPackMessage>emptyList());
//        SubscriptionPack childPack = new SubscriptionPack("child", SubscriptionPackType.CHILD, 5000, 6,
//                Collections.<SubscriptionPackMessage>emptyList());
//
//
//        subscriptionPackDataService.create(pregnancyPack);
//        subscriptionPackDataService.create(childPack);
    }


    @Test
    @Ignore
    public void shouldSerializeAshaDataFromSoapResponse() throws IOException {
        String response = RchImportTestHelper.getAnmAshaResponseData();

        SimpleHttpServer simpleServer = SimpleHttpServer.getInstance();
        String url = simpleServer.start("rchEndpoint", 200, response);

        URL endpoint = new URL(url);
        LocalDate referenceDate = LocalDate.now().minusDays(1);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(rchWebServiceFacade.getClass().getClassLoader());
        boolean status = rchWebServiceFacade.getAnmAshaData(referenceDate, referenceDate, endpoint, 21l);
        Thread.currentThread().setContextClassLoader(cl);

        assertTrue(status);
    }
}
