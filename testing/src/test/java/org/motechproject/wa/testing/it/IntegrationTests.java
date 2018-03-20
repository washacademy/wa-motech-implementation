package org.motechproject.wa.testing.it;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.motechproject.wa.testing.it.api.*;
import org.motechproject.wa.testing.it.imi.CdrFileServiceBundleIT;
import org.motechproject.wa.testing.it.imi.ImiControllerCdrBundleIT;
import org.motechproject.wa.testing.it.imi.ImiControllerObdBundleIT;
import org.motechproject.wa.testing.it.imi.TargetFileServiceBundleIT;
import org.motechproject.wa.testing.it.props.PropertyServiceBundleIT;
import org.motechproject.wa.testing.it.rch.RchWebServiceFacadeBundleIT;
import org.motechproject.wa.testing.it.region.*;
import org.motechproject.wa.testing.it.swc.ServiceUsageCapServiceBundleIT;
import org.motechproject.wa.testing.it.swc.ServiceUsageServiceBundleIT;
import org.motechproject.wa.testing.it.swc.SwachgrahiServiceBundleIT;
import org.motechproject.wa.testing.it.swc.WhiteListServiceBundleIT;
import org.motechproject.wa.testing.it.swcUpdate.SwachgrahiImportServiceBundleIT;
import org.motechproject.wa.testing.it.swcUpdate.SwachgrahiUpdateImportServiceBundleIT;
import org.motechproject.wa.testing.it.testing.BundleIT;
import org.motechproject.wa.testing.it.tracking.TrackChangesBundleIT;
import org.motechproject.wa.testing.it.tracking.TrackManyToManyChangesBundleIT;
import org.motechproject.wa.testing.it.tracking.TrackOneToManyChangesBundleIT;
import org.motechproject.wa.testing.it.wa.WashAcademyServiceBundleIT;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    /**
     * API
     */
    LanguageControllerBundleIT.class,
    UserControllerBundleIT.class,
    CallDetailsControllerBundleIT.class,
        WashAcademyControllerBundleIT.class,
    OpsControllerBundleIT.class,

    /**
     * SWC
     */
    ServiceUsageServiceBundleIT.class,
    ServiceUsageCapServiceBundleIT.class,
    WhiteListServiceBundleIT.class,
    SwachgrahiServiceBundleIT.class,

    /**
     * SWC UPDATE
     */
    SwachgrahiImportServiceBundleIT.class,
    SwachgrahiUpdateImportServiceBundleIT.class,

    /**
     * IMI
     * https://github.com/motech-implementations/mim/issues/381 (Re-enable)
     */
    ImiControllerCdrBundleIT.class,
    ImiControllerObdBundleIT.class,
    TargetFileServiceBundleIT.class,
    CdrFileServiceBundleIT.class,

    /**
     * Kilkari
     */
//    SubscriptionServiceBundleIT.class,
//    SubscriberServiceBundleIT.class,
//    CsrServiceBundleIT.class,
//    MctsBeneficiaryImportServiceBundleIT.class,
//    MctsBeneficiaryUpdateServiceBundleIT.class,

    /**
     * Wash Academy
     */
    WashAcademyServiceBundleIT.class,

    /**
     * Props
     */
    PropertyServiceBundleIT.class,

    /**
     * Region
     */
    LocationServiceBundleIT.class,
    CircleServiceBundleIT.class,
    NationalDefaultLanguageLocationBundleIT.class,
    LocationDataImportServiceBundleIT.class,
    LanguageLocationCodesImportServiceBundleIT.class,
    LanguageServiceBundleIT.class,

    /**
     * MCTS
     */
//    MctsWebServiceFacadeBundleIT.class,
//    MctsImportBundleIT.class,

    /**
     * RCH
     */
    RchWebServiceFacadeBundleIT.class,

    /**
     * Testing
     */
    BundleIT.class,

    /**
     * Tracking
     */
    TrackChangesBundleIT.class,
    TrackOneToManyChangesBundleIT.class,
    TrackManyToManyChangesBundleIT.class,
})
public class IntegrationTests {
}
