package org.motechproject.wa.rch.service.impl;

import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.description.TypeDesc;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.encoding.ser.BeanSerializer;
import org.apache.axis.server.AxisServer;
import org.apache.commons.lang3.time.StopWatch;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.motechproject.alerts.contract.AlertService;
import org.motechproject.alerts.domain.AlertStatus;
import org.motechproject.alerts.domain.AlertType;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.mds.query.QueryParams;
import org.motechproject.mds.util.Order;
import org.motechproject.wa.rch.contract.SwcDataSet;
import org.motechproject.wa.region.domain.Panchayat;
import org.motechproject.wa.swc.domain.Swachchagrahi;
import org.motechproject.wa.swc.domain.SwachchagrahiStatus;
import org.motechproject.wa.swc.exception.SwcExistingRecordException;
import org.motechproject.wa.swc.exception.SwcImportException;
import org.motechproject.wa.swc.service.SwcService;
import org.motechproject.wa.swcUpdate.contract.SwcRecord;
import org.motechproject.wa.swcUpdate.service.SwcImportService;
import org.motechproject.wa.swc.domain.RejectionReasons;
import org.motechproject.wa.rch.domain.RchImportAudit;
import org.motechproject.wa.rch.domain.RchImportFacilitator;
import org.motechproject.wa.rch.domain.RchImportFailRecord;
import org.motechproject.wa.rch.domain.RchUserType;
import org.motechproject.wa.rch.exception.ExecutionException;
import org.motechproject.wa.rch.exception.RchFileManipulationException;
import org.motechproject.wa.rch.exception.RchInvalidResponseStructureException;
import org.motechproject.wa.rch.exception.RchWebServiceException;
import org.motechproject.wa.rch.repository.RchImportAuditDataService;
import org.motechproject.wa.rch.repository.RchImportFailRecordDataService;
import org.motechproject.wa.rch.service.RchImportFacilitatorService;
import org.motechproject.wa.rch.service.RchWebServiceFacade;
import org.motechproject.wa.rch.soap.DS_DataResponseDS_DataResult;
import org.motechproject.wa.rch.soap.Irchwebservices;
import org.motechproject.wa.rch.soap.RchwebservicesLocator;
import org.motechproject.wa.rch.utils.Constants;
import org.motechproject.wa.rch.utils.ExecutionHelper;
import org.motechproject.wa.rch.utils.MarshallUtils;
import org.motechproject.wa.region.domain.State;
import org.motechproject.wa.region.exception.InvalidLocationException;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.repository.PanchayatDataService;
import org.motechproject.wa.rejectionhandler.service.SwcRejectionService;
import org.motechproject.server.config.SettingsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;


import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.io.StringWriter;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;


import static org.motechproject.wa.rch.utils.ObjectListCleaner.cleanSwcRecords;
import static org.motechproject.wa.swcUpdate.utils.RejectedObjectConverter.swcRejection;

@Service("rchWebServiceFacade")
public class RchWebServiceFacadeImpl implements RchWebServiceFacade {

    private static final String DATE_FORMAT = "dd-MM-yyyy";
    private static final String LOCAL_RESPONSE_DIR = "rch.local_response_dir";
    private static final String REMOTE_RESPONSE_DIR = "rch.remote_response_dir";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("dd-MM-yyyy");
    private static final String SCP_TIMEOUT_SETTING = "rch.scp_timeout";
    private static final Long SCP_TIME_OUT = 60000L;
    private static final String RCH_WEB_SERVICE = "RCH Web Service";
    private static final double THOUSAND = 1000d;

    @Autowired
    @Qualifier("rchSettings")
    private SettingsFacade settingsFacade;

    @Autowired
    @Qualifier("rchServiceLocator")
    private RchwebservicesLocator rchServiceLocator;

    private static final Logger LOGGER = LoggerFactory.getLogger(RchWebServiceFacadeImpl.class);

    @Autowired
    private RchImportAuditDataService rchImportAuditDataService;

    @Autowired
    private RchImportFacilitatorService rchImportFacilitatorService;

    @Autowired
    private StateDataService stateDataService;

    @Autowired
    private PanchayatDataService panchayatDataService;

    @Autowired
    private SwcImportService swcImportService;

    @Autowired
    private RchImportFailRecordDataService rchImportFailRecordDataService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private SwcRejectionService swcRejectionService;

    @Autowired
    private SwcService swcService;






    @Override
    public boolean getAnmAshaData(LocalDate from, LocalDate to, URL endpoint, Long stateId) {
        DS_DataResponseDS_DataResult result;
        Irchwebservices dataService = getService(endpoint);
        boolean status = false;

        try {
            result = dataService.DS_Data(settingsFacade.getProperty(Constants.RCH_PROJECT_ID), settingsFacade.getProperty(Constants.RCH_USER_ID),
                    settingsFacade.getProperty(Constants.RCH_PASSWORD), from.toString(DATE_FORMAT), to.toString(DATE_FORMAT), stateId.toString(),
                    settingsFacade.getProperty(Constants.RCH_ASHA_USER), settingsFacade.getProperty(Constants.RCH_DTID));
        } catch (RemoteException e) {
            throw new RchWebServiceException("Remote Server Error. Could Not Read RCH SWC Data.", e);
        }

        LOGGER.debug("writing RCH Asha response to file");
        File responseFile = generateResponseFile(result, RchUserType.ASHA, stateId);
        if (responseFile != null) {
            LOGGER.info("RCH asha response successfully written to file. Copying to remote directory.");
            try {
                scpResponseToRemote(responseFile.getName());
                LOGGER.info("RCH asha response file successfully copied to remote server");

                RchImportFacilitator rchImportFacilitator = new RchImportFacilitator(responseFile.getName(), from, to, stateId, RchUserType.ASHA, LocalDate.now());
                rchImportFacilitatorService.createImportFileAudit(rchImportFacilitator);
                status = true;
            } catch (ExecutionException e) {
                LOGGER.error("error copying file to remote server.");
            } catch (RchFileManipulationException e) {
                LOGGER.error("invalid file error");
            }
        } else {
            LOGGER.error("Error writing response to file.");
        }

        return status;
    }

    @MotechListener(subjects = Constants.RCH_ASHA_READ_SUBJECT)
    @Transactional
    public void readAshaResponseFromFile(MotechEvent event) throws RchFileManipulationException {
        LOGGER.info("RCH Asha file import entry point");
        LOGGER.info("Copying RCH Asha response file from remote server to local directory.");

        try {
            List<RchImportFacilitator> rchImportFacilitatorsAsha = rchImportFacilitatorService.findByImportDateAndRchUserType(LocalDate.now(), RchUserType.ASHA);
            LOGGER.info("Files imported today for ashas= " + rchImportFacilitatorsAsha.size());
            for (RchImportFacilitator rchImportFacilitatorAsha : rchImportFacilitatorsAsha
                    ) {
                File localResponseFile = scpResponseToLocal(rchImportFacilitatorAsha.getFileName());
                DS_DataResponseDS_DataResult result = readResponses(localResponseFile);
                Long stateId = rchImportFacilitatorAsha.getStateId();
                State importState = stateDataService.findByCode(stateId);

                String stateName = importState.getName();
                Long stateCode = importState.getCode();

                LocalDate startReferenceDate = rchImportFacilitatorAsha.getStartDate();
                LocalDate endReferenceDate = rchImportFacilitatorAsha.getEndDate();
                try {
                    validAnmAshaDataResponse(result, stateId);
                    List ashaResultFeed = result.get_any()[1].getChildren();
                    SwcDataSet ashaDataSet = (ashaResultFeed == null) ?
                            null :
                            (SwcDataSet) MarshallUtils.unmarshall(ashaResultFeed.get(0).toString(), SwcDataSet.class);

                    LOGGER.info("Starting RCH SWC import for stateId: {}", stateId);
                    StopWatch stopWatch = new StopWatch();
                    stopWatch.start();

                    if (ashaDataSet == null || ashaDataSet.getRecords() == null) {
                        String warning = String.format("No SWC data set received from RCH for %s state", stateName);
                        LOGGER.warn(warning);
                        rchImportAuditDataService.create(new RchImportAudit(startReferenceDate, endReferenceDate, RchUserType.ASHA, stateCode, stateName, 0, 0, warning));
                    } else {
                        LOGGER.info("Received {} SWC records from RCH for {} state", sizeNullSafe(ashaDataSet.getRecords()), stateName);

                        RchImportAudit audit = saveImportedAshaData(ashaDataSet, stateName, stateCode, startReferenceDate, endReferenceDate);
                        rchImportAuditDataService.create(audit);
                        stopWatch.stop();
                        double seconds = stopWatch.getTime() / THOUSAND;
                        LOGGER.info("Finished RCH SWC import dispatch in {} seconds. Accepted {} Ashas, Rejected {} Ashas",
                                seconds, audit.getAccepted(), audit.getRejected());

                        // Delete RchImportFailRecords once import is successful
                        deleteRchImportFailRecords(startReferenceDate, endReferenceDate, RchUserType.ASHA, stateId);
                    }
                } catch (JAXBException e) {
                    throw new RchInvalidResponseStructureException(String.format("Cannot deserialize RCH SWC data from %s location.", stateId), e);
                } catch (RchInvalidResponseStructureException e) {
                    String error = String.format("Cannot read RCH SWC data from %s state with stateId:%d. Response Deserialization Error", stateName, stateCode);
                    LOGGER.error(error, e);
                    alertService.create(RCH_WEB_SERVICE, "RCH Web Service SWC Import", e.getMessage() + " " + error, AlertType.CRITICAL, AlertStatus.NEW, 0, null);
                    rchImportAuditDataService.create(new RchImportAudit(startReferenceDate, endReferenceDate, RchUserType.ASHA, stateCode, stateName, 0, 0, error));
                    rchImportFailRecordDataService.create(new RchImportFailRecord(endReferenceDate, RchUserType.ASHA, stateId));
                } catch (NullPointerException e) {
                    LOGGER.error("No files saved : ", e);
                }
            }
        } catch (ExecutionException e) {
            LOGGER.error("Failed to copy response file from remote server to local directory.");
        }
    }

    private Irchwebservices getService(URL endpoint) {
        try {
            if (endpoint != null) {
                return rchServiceLocator.getBasicHttpBinding_Irchwebservices(endpoint);
            } else {
                return rchServiceLocator.getBasicHttpBinding_Irchwebservices();
            }
        } catch (ServiceException e) {
            throw new RchWebServiceException("Cannot retrieve RCH Service for the endpoint", e);
        }
    }
    

    private void validAnmAshaDataResponse(DS_DataResponseDS_DataResult data, Long stateId) {
        if (data.get_any().length != 2) {
            throw new RchInvalidResponseStructureException("Invalid anm asha data response for location " + stateId);
        }

        if (data.get_any()[1].getChildren() != null && data.get_any()[1].getChildren().size() < 1) {
            throw new RchInvalidResponseStructureException("Invalid anm asha data response " + stateId);
        }
    }

    private String targetFileName(String timeStamp, RchUserType userType, Long stateId) {
            LOGGER.info(userType.name());
            return String.format("RCH_StateID_%d_Swachgrahi_Response_%s.xml", stateId, timeStamp);

    }

    private File generateResponseFile(DS_DataResponseDS_DataResult result, RchUserType userType, Long stateId) {
        String targetFileName = targetFileName(TIME_FORMATTER.print(DateTime.now()), userType, stateId);
        File localResponseDir = localResponseDir();
        File localResponseFile = new File(localResponseDir, targetFileName);

        try {
            FileWriter writer = new FileWriter(localResponseFile);
            writer.write(serializeAxisObject(result));

            writer.flush();
            writer.close();

        } catch (Exception e) {
            LOGGER.debug("Failed deserialization", e);
            LOGGER.error((e.toString()));
            return null;
        }
        return localResponseFile;
    }



    private RchImportAudit saveImportedAshaData(SwcDataSet anmAshaDataSet, String stateName, Long stateCode, LocalDate startReferenceDate, LocalDate endReferenceDate) { //NOPMD NcssMethodCount // NO CHECKSTYLE Cyclomatic Complexity
        LOGGER.info("Starting RCH ASHA import for state {}", stateName);
        List<List<SwcRecord>> rchAshaRecordsSet = cleanSwcRecords(anmAshaDataSet.getRecords());
        List<SwcRecord> rejectedRchAshas = rchAshaRecordsSet.get(0);
        String action = "";
        for (SwcRecord record : rejectedRchAshas) {
            action = this.rchSwcActionFinder(record);
            LOGGER.error("Existing Asha Record with same MSISDN in the data set");
            swcRejectionService.createUpdate(swcRejection(record, false, RejectionReasons.DUPLICATE_MOBILE_NUMBER_IN_DATASET.toString(), action));
        }
        List<SwcRecord> acceptedRchAshas = rchAshaRecordsSet.get(1);

        int saved = 0;
        int rejected = 0;

        for (SwcRecord record : acceptedRchAshas) {
            try {
                action = this.rchSwcActionFinder(record);
                Long msisdn = Long.parseLong(record.getMobileNo());
                Panchayat panchayat = panchayatDataService.findByCode(record.getPanchayatId());
                String swcId = record.getGfId().toString();
                Swachchagrahi swc = swcService.getByContactNumber(msisdn);
                if ((swc != null && (!swcId.equals(swc.getSwcId()) || panchayat != swc.getPanchayat()))  && swc.getCourseStatus() != SwachchagrahiStatus.ANONYMOUS) {

                    LOGGER.error("Existing SWC with same MSISDN but different ID");
                    swcRejectionService.createUpdate(swcRejection(record, false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), action));
                    rejected++;
                } else {
                        try {
                            // get user property map
                            Map<String, Object> recordMap = record.toSwcRecordMap();    // temp var used for debugging
                            swcImportService.importRchFrontLineWorker(recordMap, panchayat);
                            swcRejectionService.createUpdate(swcRejection(record, true, null, action));
                            saved++;
                        } catch (InvalidLocationException e) {
                            LOGGER.warn("Invalid location for SWC: ", e);
                            swcRejectionService.createUpdate(swcRejection(record, false, RejectionReasons.INVALID_LOCATION.toString(), action));
                            rejected++;
                        } catch (SwcImportException e) {
                            LOGGER.error("Existing SWC with same MSISDN but different RCH ID", e);
                            swcRejectionService.createUpdate(swcRejection(record, false, RejectionReasons.MOBILE_NUMBER_ALREADY_IN_USE.toString(), action));
                            rejected++;
                        } catch (SwcExistingRecordException e) {
                            LOGGER.error("Cannot import SWC with ID: {}, and MSISDN (Mobile_No): {}", record.getGfId(), record.getMobileNo(), e);
                            swcRejectionService.createUpdate(swcRejection(record, false, RejectionReasons.UPDATED_RECORD_ALREADY_EXISTS.toString(), action));
                            rejected++;
                        } catch (Exception e) {
                            LOGGER.error("RCH Swc import Error. Cannot import SWC with ID: {}, and MSISDN (Mobile_No): {}",
                                    record.getGfId(), record.getMobileNo(), e);
                            swcRejectionService.createUpdate(swcRejection(record, false, RejectionReasons.SWC_IMPORT_ERROR.toString(), action));
                            rejected++;
                        }
                    if ((saved + rejected) % THOUSAND == 0) {
                        LOGGER.debug("RCH import: {} state, Progress: {} Ashas imported, {} Ashas rejected", stateName, saved, rejected);
                    }
                }
            } catch (NumberFormatException e) {
                LOGGER.error("Mobile number either not present or is not in number format");
                swcRejectionService.createUpdate(swcRejection(record, false, RejectionReasons.MOBILE_NUMBER_EMPTY_OR_WRONG_FORMAT.toString(), action));
            }
        }
        LOGGER.info("RCH import: {} state, Total: {} Ashas imported, {} Ashas rejected", stateName, saved, rejected);
        return new RchImportAudit(startReferenceDate, endReferenceDate, RchUserType.ASHA, stateCode, stateName, saved, rejected, null);
    }

    private void deleteRchImportFailRecords(final LocalDate startReferenceDate, final LocalDate endReferenceDate, final RchUserType rchUserType, final Long stateId) {

        LOGGER.debug("Deleting wa_rch_failures records which are successfully imported");
        if (startReferenceDate.equals(endReferenceDate)) {
            LOGGER.debug("No failed imports in the past 7days ");
        } else {
            QueryParams queryParams = new QueryParams(new Order("importDate", Order.Direction.ASC));
            List<RchImportFailRecord> failedImports = rchImportFailRecordDataService.getByStateAndImportdateAndUsertype(stateId, startReferenceDate, rchUserType, queryParams);
            int counter = 0;
            for (RchImportFailRecord eachFailedImport : failedImports) {
                rchImportFailRecordDataService.delete(eachFailedImport);
                counter++;
            }
            LOGGER.debug("Deleted {} rows from wa_rch_failures", counter);
        }
    }

    private int sizeNullSafe(Collection collection) {
        return collection == null ? 0 : collection.size();
    }

    private File localResponseDir() {
        return new File(this.settingsFacade.getProperty(LOCAL_RESPONSE_DIR));
    }

    private void scpResponseToRemote(String fileName) {
        String remoteDir = settingsFacade.getProperty(REMOTE_RESPONSE_DIR);
        String command = "scp " + localResponseFile(fileName) + " " + remoteDir;
        ExecutionHelper execHelper = new ExecutionHelper();
        execHelper.exec(command, getScpTimeout());
    }

    private File scpResponseToLocal(String fileName) {
        String localDir = settingsFacade.getProperty(LOCAL_RESPONSE_DIR);

        String command = "scp " + remoteResponseFile(fileName) + " " + localDir;
        ExecutionHelper execHelper = new ExecutionHelper();
        execHelper.exec(command, getScpTimeout());
        return new File(localResponseFile(fileName));
    }

    public String localResponseFile(String file) {
        String localFile = settingsFacade.getProperty(LOCAL_RESPONSE_DIR);
        localFile += localFile.endsWith("/") ? "" : "/";
        localFile += file;
        return localFile;
    }

    public String remoteResponseFile(String file) {
        String remoteFile = settingsFacade.getProperty(REMOTE_RESPONSE_DIR);
        remoteFile += remoteFile.endsWith("/") ? "" : "/";
        remoteFile += file;
        return remoteFile;
    }

    private Long getScpTimeout() {
        try {
            return Long.parseLong(settingsFacade.getProperty(SCP_TIMEOUT_SETTING));
        } catch (NumberFormatException e) {
            return SCP_TIME_OUT;
        }
    }

    private DS_DataResponseDS_DataResult readResponses(File file) throws RchFileManipulationException {
        try {
            FileReader reader = new FileReader(file);
            BufferedReader br = new BufferedReader(reader);
            String xml = "";
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                xml += currentLine;
            }

            return (DS_DataResponseDS_DataResult) deserializeAxisObject(DS_DataResponseDS_DataResult.class, xml);
        } catch (Exception e) {
            throw new RchFileManipulationException("Failed to read response file."); //NOPMD
        }
    }


    private String serializeAxisObject(Object obj) throws IOException {
        try {

            if (obj == null) {
                return null;
            }
            StringWriter outStr = new StringWriter();
            TypeDesc typeDesc = getAxisTypeDesc(obj);
            QName qname = typeDesc.getXmlType();
            String lname = qname.getLocalPart();
            if (lname.startsWith(">") && lname.length() > 1) {
                lname = lname.substring(1);
            }
            qname = new QName(qname.getNamespaceURI(), lname);
            AxisServer server = new AxisServer();
            BeanSerializer ser = new BeanSerializer(obj.getClass(), qname, typeDesc);
            SerializationContext ctx = new SerializationContext(outStr,
                    new MessageContext(server));
            ctx.setSendDecl(false);
            ctx.setDoMultiRefs(false);
            ctx.setPretty(true);
            try {
                ser.serialize(qname, new AttributesImpl(), obj, ctx);
            } catch (final Exception e) {
                throw new Exception("Unable to serialize object "
                        + obj.getClass().getName(), e);
            }

            String xml = outStr.toString();
            return xml; //NOPMD

        } catch (Exception e) {
            throw new IOException("Serialization failed", e);
        }
    }

    private Object deserializeAxisObject(Class<?> cls, String xml)
            throws IOException {
        //CHECKSTYLE:OFF
        try {
            final String SOAP_START = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header /><soapenv:Body>";
            final String SOAP_START_XSI = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soapenv:Header /><soapenv:Body>";
            final String SOAP_END = "</soapenv:Body></soapenv:Envelope>";

            //CHECKSTYLE:ON
            Object result = null;
            try {
                Message message = new Message(SOAP_START + xml + SOAP_END);
                result = message.getSOAPEnvelope().getFirstBody()
                        .getObjectValue(cls);
            } catch (Exception e) {
                try {
                    Message message = new Message(SOAP_START_XSI + xml + SOAP_END);
                    result = message.getSOAPEnvelope().getFirstBody()
                            .getObjectValue(cls);
                } catch (Exception e1) {
                    throw new Exception(e1); //NOPMD
                }
            }
            return result;
        } catch (Exception e) {
            throw new IOException("Deserialization failed", e); //NOPMD
        }
    }

    private TypeDesc getAxisTypeDesc(Object obj) throws Exception { //NOPMD
        final Class<? extends Object> objClass = obj.getClass();
        try {
            final Method methodGetTypeDesc = objClass.getMethod("getTypeDesc",
                    new Class[]{});
            final TypeDesc typeDesc = (TypeDesc) methodGetTypeDesc.invoke(obj,
                    new Object[]{});
            return (typeDesc);
        } catch (final Exception e) {
            throw new Exception("Unable to get Axis TypeDesc for "
                    + objClass.getName(), e); //NOPMD
        }
    }


    private String rchSwcActionFinder(SwcRecord record) {
        if (swcService.getBySwcIdAndPanchayat(record.getGfId().toString(), panchayatDataService.findByCode(record.getPanchayatId())) == null) {
            return "CREATE";
        } else {
            return "UPDATE";
        }
    }
}
