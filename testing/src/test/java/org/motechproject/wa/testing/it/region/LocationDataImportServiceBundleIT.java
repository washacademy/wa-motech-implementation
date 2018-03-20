package org.motechproject.wa.testing.it.region;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.motechproject.wa.csv.exception.CsvImportDataException;
import org.motechproject.wa.region.csv.BlockImportService;
import org.motechproject.wa.region.csv.DistrictImportService;
import org.motechproject.wa.region.csv.PanchayatImportService;
import org.motechproject.wa.region.csv.StateImportService;
import org.motechproject.wa.region.domain.Block;
import org.motechproject.wa.region.domain.District;
import org.motechproject.wa.region.domain.Panchayat;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.repository.BlockDataService;
import org.motechproject.wa.region.repository.DistrictDataService;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.service.BlockService;
import org.motechproject.wa.region.service.DistrictService;
import org.motechproject.wa.region.service.PanchayatService;
import org.motechproject.wa.testing.service.TestingService;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.supercsv.exception.SuperCsvException;

import javax.inject.Inject;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;
import static org.motechproject.wa.testing.it.utils.RegionHelper.createDistrict;
import static org.motechproject.wa.testing.it.utils.RegionHelper.createTaluka;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class LocationDataImportServiceBundleIT extends BasePaxIT {

    @Inject
    TestingService testingService;
    @Inject
    StateDataService stateDataService;
    @Inject
    DistrictService districtService;
    @Inject
    DistrictDataService districtDataService;
    @Inject
    BlockService blockService;
    @Inject
    BlockDataService blockDataService;
    @Inject
    PanchayatService panchayatService;
    @Inject
    StateImportService stateImportService;
    @Inject
    DistrictImportService districtImportService;
    @Inject
    BlockImportService blockImportService;
    @Inject
    PanchayatImportService panchayatImportService;

    
    State exampleState;
    District exampleDistrict;
    Block exampleBlock;

    private String stateHeader = "StateID,Name";

    private String districtHeader = "DCode,Name_G,Name_E,StateID";

    private String talukaHeader = "TCode,ID,Name_G,Name_E,StateID,DCode";

    private String healthBlockHeader = "BID,Name_G,Name_E,HQ,StateID,DCode,TCode";

    private String healthFacilityHeader = "PID,Name_G,Name_E,StateID,DCode,TCode,BID,Facility_Type";

    private String healthSubFacilityHeader = "SID,Name_G,Name_E,StateID,DCode,TCode,BID,PID";

    private String villageHeader = "VCode,Name_G,Name_E,StateID,DCode,TCode";

    @Before
    public void setUp() {

        testingService.clearDatabase();

        exampleState = stateDataService.create(new State("EXAMPLE STATE", 1L));

        exampleDistrict = createDistrict(exampleState, 2L, "EXAMPLE DISTRICT");
        districtDataService.create(exampleDistrict);

        exampleBlock = createTaluka(exampleDistrict, (long)00003, "EXAMPLE TALUKA", 1);
        blockDataService.create(exampleBlock);

//        HealthFacilityType facilityType = createHealthFacilityType("EXAMPLE FACILITY TYPE", 5678L);
//        exampleFacilityType = healthFacilityTypeDataService.create(facilityType);

    }

    
    @Test
    public void testLocationDataImport() throws Exception {
        stateImportService.importData(read("csv/state.csv"));
        State state = stateDataService.findByCode(1234L);
        assertNotNull(state);
        assertEquals(1234L, (long) state.getCode());
        assertEquals("Delhi", state.getName());

        districtImportService.importData(read("csv/district.csv"));
        District district = districtService.findByStateAndCode(state, 1L);
        assertNotNull(district);
        assertEquals(1L, (long) district.getCode());
        assertEquals("district name", district.getName());
        assertEquals("district regional name", district.getRegionalName());
        assertNotNull(district.getState());

        blockImportService.importData(read("csv/block.csv"));
        Block block = blockService.findByDistrictAndCode(district, 23L);
        System.out.print(block.getDistrict());
        assertNotNull(block);
        assertEquals(23L, (long)block.getCode());
        assertEquals(2, (int) block.getIdentity());
        assertEquals("block name", block.getName());
        assertEquals("block regional name", block.getRegionalName());
        assertNotNull(block.getDistrict());

        panchayatImportService.importData(read("csv/census_village.csv"));
        Panchayat censusPanchayat = panchayatService.findByBlockAndVcodeAndSvid(block, 3L, 0L);
        assertNotNull(censusPanchayat);
        assertEquals(3L, censusPanchayat.getVcode());
        assertEquals("census panchayat name", censusPanchayat.getName());
        assertEquals("census panchayat regional name", censusPanchayat.getRegionalName());
        assertNotNull(censusPanchayat.getBlock());

//        nonCensusPanchayatImportService.importData(read("csv/non_census_village_associated.csv"));
//        Panchayat nonCensusPanchayatAssociated = panchayatService.findByBlockAndVcodeAndSvid(block, 3L, 4L);
//        assertNotNull(nonCensusPanchayatAssociated);
//        assertEquals(4L, nonCensusPanchayatAssociated.getSvid());
//        assertEquals("non census panchayat associated name", nonCensusPanchayatAssociated.getName());
//        assertEquals("non census panchayat associated regional name", nonCensusPanchayatAssociated.getRegionalName());
//        assertNotNull(nonCensusPanchayatAssociated.getBlock());
//        assertEquals(3L, nonCensusPanchayatAssociated.getVcode());
//
//        nonCensusPanchayatImportService.importData(read("csv/non_census_village_non_associated.csv"));
//        Panchayat nonCensusPanchayatNonAssociated = panchayatService.findByBlockAndVcodeAndSvid(block, 0L, 5L);
//        assertNotNull(nonCensusPanchayatNonAssociated);
//        assertEquals(5L, nonCensusPanchayatNonAssociated.getSvid());
//        assertEquals("non census panchayat non associated name", nonCensusPanchayatNonAssociated.getName());
//        assertEquals("non census panchayat non associated regional name",
//                nonCensusPanchayatNonAssociated.getRegionalName());
//        assertNotNull(nonCensusPanchayatNonAssociated.getBlock());
//        assertEquals(0, nonCensusPanchayatNonAssociated.getVcode());

    }

    @Test(expected = SuperCsvException.class)
    public void testThrowExceptionForMalformedCsv() throws Exception {
        districtImportService.importData(read("csv/district_malformed.csv"));
    }

    @Test(expected = CsvImportDataException.class)
    public void testThrowExceptionForInvalidCellFormat() throws Exception {
        districtImportService.importData(read("csv/district_invalid_cell_format.csv"));
    }

    @Test
    public void testRollbackAllAfterSingleFailure() throws Exception {
        boolean thrown = false;
        try {
            districtImportService.importData(read("csv/district_rollback.csv"));
        } catch (Exception e) {
            thrown = true;
        }
        assertTrue(thrown);
        assertNull(districtService.findByStateAndCode(exampleState, 1002L));
        assertNull(districtService.findByStateAndCode(exampleState, 1003L));
        assertNull(districtService.findByStateAndCode(exampleState, 1004L));
    }


    @Test(expected = CsvImportDataException.class)
    public void verifyStateRejectedIfIdMissing() throws Exception {
        Reader reader = createReaderWithHeaders(stateHeader, ",foo");
        stateImportService.importData(reader);
    }

    @Test(expected = CsvImportDataException.class)
    public void verifyStateRejectedIfNameMissing() throws Exception {
        Reader reader = createReaderWithHeaders(stateHeader, "123,");
        stateImportService.importData(reader);
    }

    @Test(expected = CsvImportDataException.class)
    public void verifyStateRejectedIfIdInvalid() throws Exception {
        Reader reader = createReaderWithHeaders(stateHeader, "foo,bar");
        stateImportService.importData(reader);
    }

    /*
    * Verify state upload is rejected when name is not provided
     */
    @Test(expected = CsvImportDataException.class)
    public void stateUploadNoName() throws Exception {
        Reader reader = createReaderWithHeaders(stateHeader, ",1234");
        stateImportService.importData(reader);
    }

    /*
    * Verify state upload is rejected when code is not provided
     */
    @Test(expected = CsvImportDataException.class)
    public void stateUploadNoCode() throws Exception {
        Reader reader = createReaderWithHeaders(stateHeader, "Bihar,");
        stateImportService.importData(reader);
    }

    /*
    * To verify district location data is rejected when mandatory parameter code is missing.
    */
    @Test(expected = CsvImportDataException.class)
    public void verifyFT221() throws Exception {
        Reader reader = createReaderWithHeaders(districtHeader, ",district regional name,district name,1234");
        districtImportService.importData(reader);
    }

    /*
    * To verify district location data is rejected when mandatory parameter state_id is missing.
    */
    @Test(expected = CsvImportDataException.class)
    public void verifyFT222() throws Exception {
        Reader reader = createReaderWithHeaders(districtHeader, "1,district regional name,district name,");
        districtImportService.importData(reader);
    }

    /*
    * To verify district location data is rejected when state_id is having invalid value.
    */
    @Test
    public void verifyFT223() throws Exception {
        boolean thrown = false;
        String errorMessage = "CSV instance error [row: 2]: validation failed for instance of type " +
                "org.motechproject.wa.region.domain.District, violations: {'state': may not be null}";
        Reader reader = createReaderWithHeaders(districtHeader, "1,district regional name,district name,12345");
        try {
            districtImportService.importData(reader);
        } catch (CsvImportDataException e) {
            thrown = true;
            assertEquals(errorMessage, e.getMessage());
        }
        assertTrue(thrown);
    }

    /*
    * To verify district location data is rejected when code is having invalid value.
    */
    @Test(expected = CsvImportDataException.class)
    public void verifyFT224() throws Exception {
        Reader reader = createReaderWithHeaders(districtHeader, "asd,district regional name,district name,1234");
        districtImportService.importData(reader);
    }

    /*
    * To verify block location data is rejected when mandatory parameter name is missing.
    */
    @Test(expected = CsvImportDataException.class)
    public void verifyFT227() throws Exception {
        Reader reader = createReaderWithHeaders(talukaHeader, "TALUKA,2,block regional name,,1,2");
        blockImportService.importData(reader);
    }


    /*
    * To verify error message is correct when a column is invalid.
    * Fixes: https://applab.atlassian.net/browse/wa-213
    */
    @Test
    public void verifyErrorMessageHasCorrectColumnNumber() throws Exception {
        Reader reader = createReaderWithHeaders(talukaHeader, "TALUKA,2,,Block 2,1,2");
        try {
            blockImportService.importData(reader);
        } catch (CsvImportDataException e) {
            assertEquals("CSV field error [row: 2, col: 3]: Expected String value, found null", e.getMessage());
        }
    }

    /*
    * To verify block location data is rejected when mandatory parameter code is missing.
    */
    @Test(expected = CsvImportDataException.class)
    public void verifyFT228() throws Exception {
        Reader reader = createReaderWithHeaders(talukaHeader, ",2,block regional name,block name,1,2");
        blockImportService.importData(reader);
    }

    /*
    * To verify block location data is rejected when mandatory parameter district_id is missing.
    */
    @Test(expected = CsvImportDataException.class)
    public void verifyFT229() throws Exception {
        Reader reader = createReaderWithHeaders(talukaHeader, "TALUKA,2,block regional name,block name,1,");
        blockImportService.importData(reader);
    }

    /*
    * To verify block location data is rejected when district_id is having invalid value.
    */
    @Test
    public void verifyFT230() throws Exception {
        boolean thrown = false;
        String errorMessage = "CSV instance error [row: 2]: validation failed for instance of type " +
                "org.motechproject.wa.region.domain.Block, violations: {'district': may not be null}";
        Reader reader = createReaderWithHeaders(talukaHeader, "TALUKA,2,block regional name,block name,1,3");
        try {
            blockImportService.importData(reader);
        } catch (CsvImportDataException e) {
            thrown = true;
            assertEquals(errorMessage, e.getMessage());
        }
        assertTrue(thrown);
    }

    /*
    * To verify health block location data is rejected when mandatory parameter name is missing.
    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT234() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthBlockHeader, "6,health block regional name,,health block hq,1,2,TALUKA");
//        healthBlockImportService.importData(reader);
//    }
//
//    /*
//    * To verify health block location data is rejected when mandatory parameter code is missing.
//    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT235() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthBlockHeader, ",health block regional name,health block name,health block hq,1,2,TALUKA");
//        healthBlockImportService.importData(reader);
//    }
//
//    /*
//    * To verify health block location data is rejected when mandatory parameter taluka_id is missing.
//    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT236() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthBlockHeader, "6,health block regional name,health block name,health block hq,1,2,");
//        healthBlockImportService.importData(reader);
//    }

    /*
    * To verify health block location data is rejected when taluka_id is having invalid value.
    */
//    @Test
//    public void verifyFT237() throws Exception {
//        boolean thrown = false;
//        String errorMessage = "CSV instance error [row: 2]: validation failed for instance of type " +
//                "org.motechproject.wa.region.domain.HealthBlock, violations: {'block': may not be null}";
//        Reader reader = createReaderWithHeaders(
//                healthBlockHeader, "6,health block regional name,health block name,health block hq,1,2,invalid block");
//        try {
//            healthBlockImportService.importData(reader);
//        } catch (CsvImportDataException e) {
//            thrown = true;
//            assertEquals(errorMessage, e.getMessage());
//        }
//        assertTrue(thrown);
//    }

    /*
    * To verify health block location data is rejected when code is having invalid value.
    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT238() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthBlockHeader, "abc,health block regional name,health block name,health block hq,1,2,TALUKA");
//        healthBlockImportService.importData(reader);
//    }
//
//    /*
//    * To verify health facility location data is rejected when mandatory parameter name is missing.
//    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT241() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthFacilityHeader, "7,health facility regional name,,1,2,00003,6,5678");
//        healthFacilityImportService.importData(reader);
//    }

    /*
    * To verify health facility location data is rejected when mandatory parameter code is missing.
    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT242() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthFacilityHeader, ",health facility regional name,health facility name,1,2,00003,6,5678");
//        healthFacilityImportService.importData(reader);
//    }
//
//    /*
//    * To verify health facility location data is rejected when mandatory parameter health_block_id is missing.
//    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT243() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthFacilityHeader, "7,health facility regional name,health facility name,1,2,00003,,5678");
//        healthFacilityImportService.importData(reader);
//    }
//
//    /*
//    * To verify health facility location data is rejected when health_block_id is having invalid value.
//    */
//    @Test
//    public void verifyFT244() throws Exception {
//        boolean thrown = false;
//        String errorMessage = "CSV instance error [row: 2]: validation failed for instance of type " +
//                "org.motechproject.wa.region.domain.HealthFacility, violations: {'healthBlock': may not be null}";
//        Reader reader = createReaderWithHeaders(
//                healthFacilityHeader, "7,health facility regional name,health facility name,1,2,00003,10,5678");
//        try {
//            healthFacilityImportService.importData(reader);
//        } catch (CsvImportDataException e) {
//            thrown = true;
//            assertEquals(errorMessage, e.getMessage());
//        }
//        assertTrue(thrown);
//    }
//
//    /*
//    * To verify health facility location data is rejected when code is having invalid value.
//    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT245() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthFacilityHeader, "abc,health facility regional name,health facility name,1,2,00003,6,5678");
//        healthFacilityImportService.importData(reader);
//    }
//
//    /*
//    * To verify health sub facility location data is rejected when mandatory parameter name is missing.
//    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT248() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthSubFacilityHeader, "8,health sub facility regional name,,1,2,00003,4,5");
//        healthSubFacilityImportService.importData(reader);
//    }
//
//    /*
//    * To verify health sub facility location data is rejected when mandatory parameter code is missing.
//    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT249() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthSubFacilityHeader, ",health sub facility regional name,health sub facility name,1,2,00003,4,5");
//        healthSubFacilityImportService.importData(reader);
//    }
//
//    /*
//    * To verify health sub facility location data is rejected when mandatory parameter health_facality_id is missing.
//    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT250() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthSubFacilityHeader, "8,health sub facility regional name,health sub facility name,1,2,00003,4,");
//        healthSubFacilityImportService.importData(reader);
//    }
//
//    /*
//    * To verify health sub facility location data is rejected when health_facality_id is having invalid value.
//    */
//    @Test
//    public void verifyFT251() throws Exception {
//        boolean thrown = false;
//        String errorMessage = "CSV instance error [row: 2]: validation failed for instance of type " +
//                "org.motechproject.wa.region.domain.HealthSubFacility, violations: {'healthFacility': may not be null}";
//        Reader reader = createReaderWithHeaders(
//                healthSubFacilityHeader, "8,health sub facility regional name,health sub facility name,1,2,00003,4,6");
//        try {
//            healthSubFacilityImportService.importData(reader);
//        } catch (CsvImportDataException e) {
//            thrown = true;
//            assertEquals(errorMessage, e.getMessage());
//        }
//        assertTrue(thrown);
//    }

    /*
    * To verify health sub facility location data is rejected when code is having invalid value.
    */
//    @Test(expected = CsvImportDataException.class)
//    public void verifyFT252() throws Exception {
//        Reader reader = createReaderWithHeaders(
//                healthSubFacilityHeader, "abc,health sub facility regional name,health sub facility name,1,2,00003,4,5");
//        healthSubFacilityImportService.importData(reader);
//    }

    /*
    * To verify panchayat location data is rejected when mandatory parameter name is missing.
    */
    @Test(expected = CsvImportDataException.class)
    public void verifyFT255() throws Exception {
        Reader reader = createReaderWithHeaders(
                villageHeader, "3,census panchayat regional name,,1,2,TALUKA");
        panchayatImportService.importData(reader);
    }

    /*
    * To verify panchayat location data is rejected when mandatory parameter code is missing.
    */
    @Test(expected = CsvImportDataException.class)
    public void verifyFT256() throws Exception {
        Reader reader = createReaderWithHeaders(
                villageHeader, ",census panchayat regional name,census panchayat name,1,2,TALUKA");
        panchayatImportService.importData(reader);
    }

    /*
    * To verify panchayat location data is rejected when mandatory parameter taluka_id is missing.
    */
    @Test(expected = CsvImportDataException.class)
    public void verifyFT257() throws Exception {
        Reader reader = createReaderWithHeaders(
                villageHeader, "3,census panchayat regional name,census panchayat name,1,2,");
        panchayatImportService.importData(reader);
    }

    /*
    * To verify panchayat location data is rejected when taluka_id is having invalid value.
    */
    @Test
    public void verifyFT258() throws Exception {
        boolean thrown = false;
        String errorMessage = "CSV instance error [row: 2]: validation failed for instance of type " +
                "org.motechproject.wa.region.domain.Panchayat, violations: {'block': may not be null}";
        Reader reader = createReaderWithHeaders(
                villageHeader, "3,census panchayat regional name,census panchayat name,1,2,44");
        try {
            panchayatImportService.importData(reader);
        } catch (CsvImportDataException e) {
            thrown = true;
            assertEquals(errorMessage, e.getMessage());
        }
        assertTrue(thrown);
    }

    /*
    * To verify panchayat location data is rejected when code is having invalid value.
    */
    @Test(expected = CsvImportDataException.class)
    public void verifyFT259() throws Exception {
        Reader reader = createReaderWithHeaders(
                villageHeader, "abc,census panchayat regional name,census panchayat name,1,2,TALUKA");
        panchayatImportService.importData(reader);
    }

    private Reader createReaderWithHeaders(String header, String... lines) {
        StringBuilder builder = new StringBuilder();
        builder.append(header);
        builder.append("\r\n");

        for (String line : lines) {
            builder.append(line).append("\r\n");
        }
        return new StringReader(builder.toString());
    }

    private Reader read(String resource) {
        return new InputStreamReader(getClass().getClassLoader().getResourceAsStream(resource));
    }
}
