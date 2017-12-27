package org.motechproject.nms.testing.it;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.motechproject.nms.testing.it.api.*;
import org.motechproject.nms.testing.it.swc.ServiceUsageCapServiceBundleIT;
import org.motechproject.nms.testing.it.swc.ServiceUsageServiceBundleIT;
import org.motechproject.nms.testing.it.swc.SwachgrahiServiceBundleIT;
import org.motechproject.nms.testing.it.swc.WhiteListServiceBundleIT;
import org.motechproject.nms.testing.it.swcUpdate.SwachgrahiImportServiceBundleIT;
import org.motechproject.nms.testing.it.swcUpdate.SwachgrahiUpdateImportServiceBundleIT;
import org.motechproject.nms.testing.it.imi.CdrFileServiceBundleIT;
import org.motechproject.nms.testing.it.imi.ImiControllerCdrBundleIT;
import org.motechproject.nms.testing.it.imi.ImiControllerObdBundleIT;
import org.motechproject.nms.testing.it.imi.TargetFileServiceBundleIT;
import org.motechproject.nms.testing.it.wa.WashAcademyServiceBundleIT;
import org.motechproject.nms.testing.it.props.PropertyServiceBundleIT;
import org.motechproject.nms.testing.it.rch.RchWebServiceFacadeBundleIT;
import org.motechproject.nms.testing.it.region.CircleServiceBundleIT;
import org.motechproject.nms.testing.it.region.LanguageLocationCodesImportServiceBundleIT;
import org.motechproject.nms.testing.it.region.LanguageServiceBundleIT;
import org.motechproject.nms.testing.it.region.LocationDataImportServiceBundleIT;
import org.motechproject.nms.testing.it.region.LocationServiceBundleIT;
import org.motechproject.nms.testing.it.region.NationalDefaultLanguageLocationBundleIT;
import org.motechproject.nms.testing.it.testing.BundleIT;
import org.motechproject.nms.testing.it.tracking.TrackChangesBundleIT;
import org.motechproject.nms.testing.it.tracking.TrackManyToManyChangesBundleIT;
import org.motechproject.nms.testing.it.tracking.TrackOneToManyChangesBundleIT;

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
     * FLW
     */
    ServiceUsageServiceBundleIT.class,
    ServiceUsageCapServiceBundleIT.class,
    WhiteListServiceBundleIT.class,
    SwachgrahiServiceBundleIT.class,

    /**
     * FLW UPDATE
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
