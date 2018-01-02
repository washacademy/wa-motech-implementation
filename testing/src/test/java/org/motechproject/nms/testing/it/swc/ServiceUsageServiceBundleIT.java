package org.motechproject.nms.testing.it.swc;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.nms.swc.domain.CallDetailRecord;
import org.motechproject.nms.swc.domain.Swachchagrahi;
import org.motechproject.nms.swc.domain.SwachchagrahiStatus;
import org.motechproject.nms.swc.domain.ServiceUsage;
import org.motechproject.nms.swc.repository.CallDetailRecordDataService;
import org.motechproject.nms.swc.repository.SwcDataService;
import org.motechproject.nms.swc.service.SwcService;
import org.motechproject.nms.swc.service.ServiceUsageService;
import org.motechproject.nms.props.domain.Service;
import org.motechproject.nms.testing.service.TestingService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verify that HelloWorldService present, functional.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class ServiceUsageServiceBundleIT extends BasePaxIT {

    @Inject
    SwcDataService swcDataService;

    @Inject
    SwcService swcService;

    @Inject
    ServiceUsageService serviceUsageService;

    @Inject
    TestingService testingService;

    @Inject
    CallDetailRecordDataService callDetailRecordDataService;

    private void setupData() {
        testingService.clearDatabase();

        for (Swachchagrahi swc: swcDataService.retrieveAll()) {
            swc.setCourseStatus(SwachchagrahiStatus.INVALID);
            swc.setInvalidationDate(new DateTime().withDate(2011, 8, 1));

            swcDataService.update(swc);
        }

        callDetailRecordDataService.deleteAll();
        swcDataService.deleteAll();
    }

    @Test
    public void testServiceUsageServicePresent() throws Exception {
        assertNotNull(serviceUsageService);
    }

    @Test
    public void testGetCurrentMonthlyUsageForFLWAndService() throws Exception {
        setupData();
        Swachchagrahi swc = new Swachchagrahi("Valid Worker", 1111111111L);
        swcService.add(swc);

        Swachchagrahi swcIgnored = new Swachchagrahi("Ignored Worker", 2222222222L);
        swcService.add(swcIgnored);

        CallDetailRecord lastMonth = new CallDetailRecord();
        lastMonth.setSwachchagrahi(swc);
        lastMonth.setCallingNumber(1111111111l);
        lastMonth.setService(Service.WASH_ACADEMY);
        lastMonth.setCallDurationInPulses(1);
        lastMonth.setEndOfUsagePromptCounter(1);
        lastMonth.setWelcomePrompt(true);
        lastMonth.setCallStartTime(DateTime.now().minusMonths(2));
        callDetailRecordDataService.create(lastMonth);

        // A usage record for a different service that should be ignored
        CallDetailRecord differentService = new CallDetailRecord();
        differentService.setSwachchagrahi(swc);
        differentService.setCallingNumber(1111111111l);
        differentService.setService(Service.MOBILE_KUNJI);
        differentService.setCallDurationInPulses(1);
        differentService.setEndOfUsagePromptCounter(1);
        differentService.setWelcomePrompt(true);
        differentService.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(differentService);

        // A usage record for a different FLW that should be ignored
        CallDetailRecord differentFLW = new CallDetailRecord();
        differentFLW.setSwachchagrahi(swcIgnored);
        differentFLW.setCallingNumber(1111111111l);
        differentFLW.setService(Service.MOBILE_KUNJI);
        differentFLW.setCallDurationInPulses(1);
        differentFLW.setEndOfUsagePromptCounter(1);
        differentFLW.setWelcomePrompt(true);
        differentFLW.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(differentFLW);

        // Two valid records that should get aggregated
        CallDetailRecord recordOne = new CallDetailRecord();
        recordOne.setSwachchagrahi(swc);
        recordOne.setCallingNumber(1111111111l);
        recordOne.setService(Service.WASH_ACADEMY);
        recordOne.setCallDurationInPulses(1);
        recordOne.setEndOfUsagePromptCounter(0);
        recordOne.setWelcomePrompt(true);
        recordOne.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(recordOne);

        CallDetailRecord recordTwo = new CallDetailRecord();
        recordTwo.setSwachchagrahi(swc);
        recordTwo.setCallingNumber(1111111111l);
        recordTwo.setService(Service.WASH_ACADEMY);
        recordTwo.setCallDurationInPulses(1);
        recordTwo.setEndOfUsagePromptCounter(1);
        recordTwo.setWelcomePrompt(false);
        recordTwo.setCallStartTime(DateTime.now());
        callDetailRecordDataService.create(recordTwo);

        ServiceUsage serviceUsage = serviceUsageService.getCurrentMonthlyUsageForFLWAndService(swc, Service.WASH_ACADEMY);

        assertEquals(swc, serviceUsage.getSwachchagrahi());
        assertEquals(Service.WASH_ACADEMY, serviceUsage.getService());
        assertEquals(2, serviceUsage.getUsageInPulses());
        assertEquals(1, serviceUsage.getEndOfUsage());
        assertEquals(true, serviceUsage.getWelcomePrompt());

        callDetailRecordDataService.delete(lastMonth);
        callDetailRecordDataService.delete(differentService);
        callDetailRecordDataService.delete(differentFLW);
        callDetailRecordDataService.delete(recordOne);
        callDetailRecordDataService.delete(recordTwo);

        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(swc);
        swcService.delete(swc);

        swcIgnored.setCourseStatus(SwachchagrahiStatus.INVALID);
        swcIgnored.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(swcIgnored);
        swcService.delete(swcIgnored);
    }

    @Test
    public void testGetCurrentMonthlyUsageForFLWAndServiceWithNoService() throws Exception {
        setupData();
        Swachchagrahi swc = new Swachchagrahi("Valid Worker", 1111111111L);
        swcService.add(swc);

        ServiceUsage serviceUsage = serviceUsageService.getCurrentMonthlyUsageForFLWAndService(swc, Service.WASH_ACADEMY);

        assertEquals(swc, serviceUsage.getSwachchagrahi());
        assertEquals(Service.WASH_ACADEMY, serviceUsage.getService());
        assertEquals(0, serviceUsage.getUsageInPulses());
        assertEquals(0, serviceUsage.getEndOfUsage());
        assertEquals(false, serviceUsage.getWelcomePrompt());

        callDetailRecordDataService.deleteAll();

        swc.setCourseStatus(SwachchagrahiStatus.INVALID);
        swc.setInvalidationDate(new DateTime().withDate(2011, 8, 1));
        swcService.update(swc);
        swcService.delete(swc);
    }

}
