# coding: utf-8
from sqlalchemy import BigInteger, Column, DateTime, ForeignKey, Index, Integer, String, text
from sqlalchemy.orm import relationship
from sqlalchemy.dialects.mysql.base import BIT
from sqlalchemy.ext.declarative import declarative_base


Base = declarative_base()
metadata = Base.metadata


class waKKSUMMARYRECORDSSTATUSSTAT(Base):
    __tablename__ = 'wa_KK_SUMMARY_RECORDS_STATUSSTATS'

    id_OID = Column(ForeignKey(u'wa_kk_summary_records.id'), primary_key=True, nullable=False, index=True)
    KEY = Column(Integer, primary_key=True, nullable=False)
    VALUE = Column(Integer)

    wa_kk_summary_record = relationship(u'waKkSummaryRecord')


class waCallContent(Base):
    __tablename__ = 'wa_call_content'

    id = Column(BigInteger, primary_key=True, index=True)
    callDetailRecord_id_OID = Column(ForeignKey(u'wa_swc_cdrs.id', ondelete=u'CASCADE'), index=True)
    completionFlag = Column(BIT(1))
    contentFile = Column(String(255, u'utf8_bin'))
    contentName = Column(String(255, u'utf8_bin'))
    correctAnswerEntered = Column(BIT(1))
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    endTime = Column(DateTime)
    mobileKunjiCardCode = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    owner = Column(String(255, u'utf8_bin'))
    startTime = Column(DateTime)
    type = Column(String(255, u'utf8_bin'))

    wa_swc_cdr = relationship(u'waSwcCdr')


class waCircle(Base):
    __tablename__ = 'wa_circles'

    id = Column(BigInteger, primary_key=True, index=True)
    defaultLanguage_id_OID = Column(ForeignKey(u'wa_languages.id'), index=True)
    name = Column(String(255, u'utf8_bin'), unique=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))

    wa_language = relationship(u'waLanguage')


class waCsvAuditRecord(Base):
    __tablename__ = 'wa_csv_audit_records'

    id = Column(BigInteger, primary_key=True, index=True)
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    endpoint = Column(String(255, u'utf8_bin'))
    file = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    outcome = Column(String(1000, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))


class waDeployedService(Base):
    __tablename__ = 'wa_deployed_services'
    __table_args__ = (
        Index('UNIQUE_STATE_SERVICE_COMPOSITE_IDX', 'state_id_OID', 'service', unique=True),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    owner = Column(String(255, u'utf8_bin'))
    service = Column(String(255, u'utf8_bin'), nullable=False)
    state_id_OID = Column(ForeignKey(u'wa_states.id'), nullable=False, index=True)

    wa_state = relationship(u'waState')


class waDistrict(Base):
    __tablename__ = 'wa_districts'
    __table_args__ = (
        Index('UNIQUE_STATE_CODE', 'state_id_OID', 'code', unique=True),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    code = Column(BigInteger, nullable=False)
    language_id_OID = Column(ForeignKey(u'wa_languages.id'), index=True)
    name = Column(String(100, u'utf8_bin'))
    regionalName = Column(String(100, u'utf8_bin'))
    state_id_OID = Column(ForeignKey(u'wa_states.id', ondelete=u'CASCADE'), nullable=False, index=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))
    districts_INTEGER_IDX = Column(Integer)

    wa_language = relationship(u'waLanguage')
    wa_state = relationship(u'waState')


class waSwcCdr(Base):
    __tablename__ = 'wa_swc_cdrs'

    id = Column(BigInteger, primary_key=True, index=True)
    callDisconnectReason = Column(String(255, u'utf8_bin'))
    callDurationInPulses = Column(Integer, nullable=False)
    callEndTime = Column(DateTime)
    callId = Column(BigInteger, nullable=False)
    callStartTime = Column(DateTime)
    callStatus = Column(Integer, nullable=False)
    callingNumber = Column(BigInteger, nullable=False, index=True)
    circleName = Column(String(255, u'utf8_bin'))
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    endOfUsagePromptCounter = Column(Integer, nullable=False)
    finalCallStatus = Column(String(255, u'utf8_bin'))
    frontLineWorker_id_OID = Column(ForeignKey(u'wa_front_line_workers.id'), index=True)
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    operator = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))
    service = Column(String(255, u'utf8_bin'), nullable=False)
    welcomePrompt = Column(BIT(1))

    wa_front_line_worker = relationship(u'waFrontLineWorker')


class waFrontLineWorker(Base):
    __tablename__ = 'wa_front_line_workers'
    __table_args__ = (
        Index('status_invalidationDate_composit_idx', 'status', 'invalidationDate'),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    contactNumber = Column(BigInteger, nullable=False, unique=True)
    district_id_OID = Column(ForeignKey(u'wa_districts.id'), index=True)
    swcId = Column(String(255, u'utf8_bin'), index=True)
    healthBlock_id_OID = Column(ForeignKey(u'wa_health_blocks.id'), index=True)
    healthFacility_id_OID = Column(ForeignKey(u'wa_health_facilities.id'), index=True)
    healthSubFacility_id_OID = Column(ForeignKey(u'wa_health_sub_facilities.id'), index=True)
    invalidationDate = Column(DateTime)
    language_id_OID = Column(ForeignKey(u'wa_languages.id'), index=True)
    mctsSwcId = Column(String(255, u'utf8_bin'))
    name = Column(String(255, u'utf8_bin'))
    state_id_OID = Column(ForeignKey(u'wa_states.id'), index=True)
    status = Column(String(255, u'utf8_bin'))
    taluka_id_OID = Column(ForeignKey(u'wa_talukas.id'), index=True)
    village_id_OID = Column(ForeignKey(u'wa_villages.id'), index=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))

    wa_district = relationship(u'waDistrict')
    wa_health_block = relationship(u'waHealthBlock')
    wa_health_facility = relationship(u'waHealthFacility')
    wa_health_sub_facility = relationship(u'waHealthSubFacility')
    wa_language = relationship(u'waLanguage')
    wa_state = relationship(u'waState')
    wa_taluka = relationship(u'waTaluka')
    wa_village = relationship(u'waVillage')


class waHealthBlock(Base):
    __tablename__ = 'wa_health_blocks'
    __table_args__ = (
        Index('UNIQUE_TALUKA_CODE', 'taluka_id_OID', 'code', unique=True),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    code = Column(BigInteger, nullable=False)
    hq = Column(String(50, u'utf8_bin'))
    name = Column(String(35, u'utf8_bin'))
    regionalName = Column(String(50, u'utf8_bin'))
    taluka_id_OID = Column(ForeignKey(u'wa_talukas.id', ondelete=u'CASCADE'), nullable=False, index=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))
    healthBlocks_INTEGER_IDX = Column(Integer)

    wa_taluka = relationship(u'waTaluka')


class waHealthFacility(Base):
    __tablename__ = 'wa_health_facilities'
    __table_args__ = (
        Index('UNIQUE_HEALTH_BLOCK_CODE', 'healthBlock_id_OID', 'code', unique=True),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    code = Column(BigInteger, nullable=False)
    healthBlock_id_OID = Column(ForeignKey(u'wa_health_blocks.id', ondelete=u'CASCADE'), nullable=False, index=True)
    healthFacilityType_id_OID = Column(ForeignKey(u'wa_health_facility_types.id'), nullable=False, index=True)
    name = Column(String(50, u'utf8_bin'))
    regionalName = Column(String(50, u'utf8_bin'))
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))
    healthFacilities_INTEGER_IDX = Column(Integer)

    wa_health_block = relationship(u'waHealthBlock')
    wa_health_facility_type = relationship(u'waHealthFacilityType')


class waHealthFacilityType(Base):
    __tablename__ = 'wa_health_facility_types'

    id = Column(BigInteger, primary_key=True, index=True)
    code = Column(BigInteger, nullable=False, unique=True)
    name = Column(String(100, u'utf8_bin'))
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))


class waHealthSubFacility(Base):
    __tablename__ = 'wa_health_sub_facilities'
    __table_args__ = (
        Index('UNIQUE_HEALTH_FACILITY_CODE', 'healthFacility_id_OID', 'code', unique=True),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    code = Column(BigInteger, nullable=False)
    healthFacility_id_OID = Column(ForeignKey(u'wa_health_facilities.id', ondelete=u'CASCADE'), nullable=False, index=True)
    name = Column(String(100, u'utf8_bin'))
    regionalName = Column(String(100, u'utf8_bin'))
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))
    healthSubFacilities_INTEGER_IDX = Column(Integer)

    wa_health_facility = relationship(u'waHealthFacility')


class waImiCdr(Base):
    __tablename__ = 'wa_imi_cdrs'

    id = Column(BigInteger, primary_key=True, index=True)
    attemptNo = Column(String(255, u'utf8_bin'))
    callAnswerTime = Column(String(255, u'utf8_bin'))
    callDisconnectReason = Column(String(255, u'utf8_bin'))
    callDurationInPulse = Column(String(255, u'utf8_bin'))
    callEndTime = Column(String(255, u'utf8_bin'))
    callId = Column(String(255, u'utf8_bin'))
    callStartTime = Column(String(255, u'utf8_bin'))
    callStatus = Column(String(255, u'utf8_bin'))
    circleId = Column(String(255, u'utf8_bin'))
    contentFile = Column(String(255, u'utf8_bin'))
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    languageLocationId = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    msgPlayEndTime = Column(String(255, u'utf8_bin'))
    msgPlayStartTime = Column(String(255, u'utf8_bin'))
    msisdn = Column(BigInteger)
    operatorId = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))
    priority = Column(String(255, u'utf8_bin'))
    requestId = Column(String(255, u'utf8_bin'))
    weekId = Column(String(255, u'utf8_bin'))


class waImiCsr(Base):
    __tablename__ = 'wa_imi_csrs'

    id = Column(BigInteger, primary_key=True, index=True)
    attempts = Column(Integer)
    callFlowUrl = Column(String(255, u'utf8_bin'))
    circleName = Column(String(255, u'utf8_bin'))
    cli = Column(String(255, u'utf8_bin'))
    contentFileName = Column(String(255, u'utf8_bin'))
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    finalStatus = Column(Integer)
    languageCode = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    msisdn = Column(BigInteger)
    owner = Column(String(255, u'utf8_bin'))
    priority = Column(Integer)
    requestId = Column(String(255, u'utf8_bin'))
    serviceId = Column(String(255, u'utf8_bin'))
    statusCode = Column(Integer)
    weekId = Column(String(255, u'utf8_bin'))


class waImiFileAuditRecord(Base):
    __tablename__ = 'wa_imi_file_audit_records'

    id = Column(BigInteger, primary_key=True, index=True)
    checksum = Column(String(40, u'utf8_bin'))
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    error = Column(String(1024, u'utf8_bin'))
    fileName = Column(String(255, u'utf8_bin'), index=True)
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    owner = Column(String(255, u'utf8_bin'))
    recordCount = Column(Integer)
    success = Column(BIT(1), nullable=False)
    type = Column(String(255, u'utf8_bin'), nullable=False)


class waInboxCallDatum(Base):
    __tablename__ = 'wa_inbox_call_data'

    id = Column(BigInteger, primary_key=True, index=True)
    contentFileName = Column(String(255, u'utf8_bin'))
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    endTime = Column(DateTime)
    inboxWeekId = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    owner = Column(String(255, u'utf8_bin'))
    startTime = Column(DateTime)
    subscriptionId = Column(String(255, u'utf8_bin'))
    subscriptionPack = Column(String(255, u'utf8_bin'))
    content_id_OWN = Column(ForeignKey(u'wa_inbox_call_details.id'), index=True)

    wa_inbox_call_detail = relationship(u'waInboxCallDetail')


class waInboxCallDetail(Base):
    __tablename__ = 'wa_inbox_call_details'

    id = Column(BigInteger, primary_key=True, index=True)
    callDisconnectReason = Column(Integer)
    callDurationInPulses = Column(Integer)
    callEndTime = Column(DateTime)
    callId = Column(BigInteger)
    callStartTime = Column(DateTime)
    callStatus = Column(Integer)
    callingNumber = Column(BigInteger)
    circleName = Column(String(255, u'utf8_bin'))
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    operator = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))


class waKkRetryRecord(Base):
    __tablename__ = 'wa_kk_retry_records'

    id = Column(BigInteger, primary_key=True, index=True)
    callStage = Column(String(255, u'utf8_bin'))
    circleName = Column(String(255, u'utf8_bin'))
    contentFileName = Column(String(255, u'utf8_bin'))
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    dayOfTheWeek = Column(String(255, u'utf8_bin'), index=True)
    languageCode = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    msisdn = Column(BigInteger)
    owner = Column(String(255, u'utf8_bin'))
    subscriptionId = Column(String(255, u'utf8_bin'), index=True)
    subscriptionOrigin = Column(String(255, u'utf8_bin'))
    weekId = Column(String(255, u'utf8_bin'))


class waKkSummaryRecord(Base):
    __tablename__ = 'wa_kk_summary_records'

    id = Column(BigInteger, primary_key=True, index=True)
    attemptedDayCount = Column(Integer)
    callAttempts = Column(Integer)
    circleName = Column(String(255, u'utf8_bin'))
    contentFileName = Column(String(255, u'utf8_bin'))
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    finalStatus = Column(String(255, u'utf8_bin'))
    languageCode = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    msisdn = Column(BigInteger)
    owner = Column(String(255, u'utf8_bin'))
    percentPlayed = Column(Integer)
    requestId = Column(String(255, u'utf8_bin'), unique=True)
    weekId = Column(String(255, u'utf8_bin'))


class waLanguage(Base):
    __tablename__ = 'wa_languages'

    id = Column(BigInteger, primary_key=True, index=True)
    code = Column(String(255, u'utf8_bin'), unique=True)
    name = Column(String(255, u'utf8_bin'), unique=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))


class waMaCompletionRecord(Base):
    __tablename__ = 'wa_ma_completion_records'

    id = Column(BigInteger, primary_key=True, index=True)
    callingNumber = Column(BigInteger, nullable=False, unique=True)
    completionCount = Column(Integer, nullable=False)
    lastDeliveryStatus = Column(String(255, u'utf8_bin'))
    notificationRetryCount = Column(Integer, nullable=False)
    score = Column(Integer, nullable=False)
    sentNotification = Column(BIT(1), nullable=False)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))


class waMaCourse(Base):
    __tablename__ = 'wa_ma_course'

    id = Column(BigInteger, primary_key=True, index=True)
    content = Column(String)
    name = Column(String(255, u'utf8_bin'), unique=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))


class waMctsChildren(Base):
    __tablename__ = 'wa_mcts_children'

    id = Column(BigInteger, primary_key=True, index=True)
    mother_id_OID = Column(ForeignKey(u'wa_mcts_mothers.id'), index=True)
    beneficiaryId = Column(String(255, u'utf8_bin'), unique=True)
    district_id_OID = Column(ForeignKey(u'wa_districts.id'), index=True)
    healthBlock_id_OID = Column(ForeignKey(u'wa_health_blocks.id'), index=True)
    healthFacility_id_OID = Column(ForeignKey(u'wa_health_facilities.id'), index=True)
    healthSubFacility_id_OID = Column(ForeignKey(u'wa_health_sub_facilities.id'), index=True)
    name = Column(String(255, u'utf8_bin'))
    primaryHealthCenter_id_OID = Column(ForeignKey(u'wa_health_facilities.id'), index=True)
    state_id_OID = Column(ForeignKey(u'wa_states.id'), index=True)
    taluka_id_OID = Column(ForeignKey(u'wa_talukas.id'), index=True)
    village_id_OID = Column(ForeignKey(u'wa_villages.id'), index=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))

    wa_district = relationship(u'waDistrict')
    wa_health_block = relationship(u'waHealthBlock')
    wa_health_facility = relationship(u'waHealthFacility', primaryjoin='waMctsChildren.healthFacility_id_OID == waHealthFacility.id')
    wa_health_sub_facility = relationship(u'waHealthSubFacility')
    wa_mcts_mother = relationship(u'waMctsMother')
    wa_health_facility1 = relationship(u'waHealthFacility', primaryjoin='waMctsChildren.primaryHealthCenter_id_OID == waHealthFacility.id')
    wa_state = relationship(u'waState')
    wa_taluka = relationship(u'waTaluka')
    wa_village = relationship(u'waVillage')


class waMctsMother(Base):
    __tablename__ = 'wa_mcts_mothers'

    id = Column(BigInteger, primary_key=True, index=True)
    dateOfBirth = Column(DateTime)
    beneficiaryId = Column(String(255, u'utf8_bin'), unique=True)
    district_id_OID = Column(ForeignKey(u'wa_districts.id'), index=True)
    healthBlock_id_OID = Column(ForeignKey(u'wa_health_blocks.id'), index=True)
    healthFacility_id_OID = Column(ForeignKey(u'wa_health_facilities.id'), index=True)
    healthSubFacility_id_OID = Column(ForeignKey(u'wa_health_sub_facilities.id'), index=True)
    name = Column(String(255, u'utf8_bin'))
    primaryHealthCenter_id_OID = Column(ForeignKey(u'wa_health_facilities.id'), index=True)
    state_id_OID = Column(ForeignKey(u'wa_states.id'), index=True)
    taluka_id_OID = Column(ForeignKey(u'wa_talukas.id'), index=True)
    village_id_OID = Column(ForeignKey(u'wa_villages.id'), index=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))

    wa_district = relationship(u'waDistrict')
    wa_health_block = relationship(u'waHealthBlock')
    wa_health_facility = relationship(u'waHealthFacility', primaryjoin='waMctsMother.healthFacility_id_OID == waHealthFacility.id')
    wa_health_sub_facility = relationship(u'waHealthSubFacility')
    wa_health_facility1 = relationship(u'waHealthFacility', primaryjoin='waMctsMother.primaryHealthCenter_id_OID == waHealthFacility.id')
    wa_state = relationship(u'waState')
    wa_taluka = relationship(u'waTaluka')
    wa_village = relationship(u'waVillage')


class waNationalDefaultLanguage(Base):
    __tablename__ = 'wa_national_default_language'

    id = Column(BigInteger, primary_key=True, index=True)
    code = Column(Integer, nullable=False, unique=True, server_default=text("'0'"))
    language_id_OID = Column(ForeignKey(u'wa_languages.id'), nullable=False, index=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))

    wa_language = relationship(u'waLanguage')


class waServiceUsageCap(Base):
    __tablename__ = 'wa_service_usage_caps'
    __table_args__ = (
        Index('UNIQUE_STATE_SERVICE_COMPOSITE_IDX', 'state_id_OID', 'service', unique=True),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    maxUsageInPulses = Column(Integer, nullable=False)
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    owner = Column(String(255, u'utf8_bin'))
    service = Column(String(255, u'utf8_bin'))
    state_id_OID = Column(ForeignKey(u'wa_states.id'), index=True)

    wa_state = relationship(u'waState')


class waState(Base):
    __tablename__ = 'wa_states'

    id = Column(BigInteger, primary_key=True, index=True)
    code = Column(BigInteger, nullable=False, unique=True)
    name = Column(String(255, u'utf8_bin'), index=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))


class waStatesJoinCircle(Base):
    __tablename__ = 'wa_states_join_circles'

    circle_id = Column(ForeignKey(u'wa_circles.id'), primary_key=True, nullable=False, index=True)
    state_id = Column(ForeignKey(u'wa_states.id'), nullable=False, index=True)
    IDX = Column(Integer, primary_key=True, nullable=False)

    circleName = relationship(u'waCircle')
    state = relationship(u'waState')


class waSubscriber(Base):
    __tablename__ = 'wa_subscribers'

    id = Column(BigInteger, primary_key=True, index=True)
    callingNumber = Column(BigInteger, nullable=False, unique=True)
    child_id_OID = Column(ForeignKey(u'wa_mcts_children.id'), index=True)
    circle_id_OID = Column(ForeignKey(u'wa_circles.id'), index=True)
    dateOfBirth = Column(DateTime)
    language_id_OID = Column(ForeignKey(u'wa_languages.id'), index=True)
    lastMenstrualPeriod = Column(DateTime)
    mother_id_OID = Column(ForeignKey(u'wa_mcts_mothers.id'), index=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))

    wa_mcts_children = relationship(u'waMctsChildren')
    wa_circle = relationship(u'waCircle')
    wa_language = relationship(u'waLanguage')
    wa_mcts_mother = relationship(u'waMctsMother')


class waSubscriptionError(Base):
    __tablename__ = 'wa_subscription_errors'

    id = Column(BigInteger, primary_key=True, index=True)
    beneficiaryId = Column(String(255, u'utf8_bin'), index=True)
    contactNumber = Column(BigInteger, nullable=False, index=True)
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    owner = Column(String(255, u'utf8_bin'))
    packType = Column(String(255, u'utf8_bin'))
    rejectionMessage = Column(String(255, u'utf8_bin'))
    rejectionReason = Column(String(255, u'utf8_bin'))


class waSubscriptionPackMessage(Base):
    __tablename__ = 'wa_subscription_pack_messages'

    id = Column(BigInteger, primary_key=True, index=True)
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    duration = Column(Integer, nullable=False)
    messageFileName = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    owner = Column(String(255, u'utf8_bin'))
    weekId = Column(String(255, u'utf8_bin'))
    messages_id_OWN = Column(ForeignKey(u'wa_subscription_packs.id'), index=True)
    messages_INTEGER_IDX = Column(Integer)

    wa_subscription_pack = relationship(u'waSubscriptionPack')


class waSubscriptionPack(Base):
    __tablename__ = 'wa_subscription_packs'

    id = Column(BigInteger, primary_key=True, index=True)
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    messagesPerWeek = Column(Integer, nullable=False)
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    name = Column(String(100, u'utf8_bin'), unique=True)
    owner = Column(String(255, u'utf8_bin'))
    type = Column(String(255, u'utf8_bin'), nullable=False, index=True)
    weeks = Column(Integer, nullable=False)


class waSubscription(Base):
    __tablename__ = 'wa_subscriptions'
    __table_args__ = (
        Index('status_endDate_composit_idx', 'status', 'endDate'),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    activationDate = Column(DateTime)
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    deactivationReason = Column(String(255, u'utf8_bin'))
    endDate = Column(DateTime)
    firstMessageDayOfWeek = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    needsWelcomeMessageViaObd = Column(BIT(1), nullable=False)
    origin = Column(String(255, u'utf8_bin'), nullable=False)
    owner = Column(String(255, u'utf8_bin'))
    secondMessageDayOfWeek = Column(String(255, u'utf8_bin'))
    startDate = Column(DateTime, index=True)
    status = Column(String(255, u'utf8_bin'), nullable=False, index=True)
    subscriber_id_OID = Column(ForeignKey(u'wa_subscribers.id'), nullable=False, index=True)
    subscriptionId = Column(String(36, u'utf8_bin'), unique=True)
    subscriptionPack_id_OID = Column(ForeignKey(u'wa_subscription_packs.id'), nullable=False, index=True)

    wa_subscriber = relationship(u'waSubscriber')
    wa_subscription_pack = relationship(u'waSubscriptionPack')


class waTaluka(Base):
    __tablename__ = 'wa_talukas'
    __table_args__ = (
        Index('UNIQUE_DISTRICT_CODE', 'district_id_OID', 'code', unique=True),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    code = Column(String(7, u'utf8_bin'))
    district_id_OID = Column(ForeignKey(u'wa_districts.id', ondelete=u'CASCADE'), nullable=False, index=True)
    identity = Column(Integer, nullable=False)
    name = Column(String(100, u'utf8_bin'))
    regionalName = Column(String(100, u'utf8_bin'))
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))
    talukas_INTEGER_IDX = Column(Integer)

    wa_district = relationship(u'waDistrict')


class waVillage(Base):
    __tablename__ = 'wa_villages'
    __table_args__ = (
        Index('UNIQUE_TALUKA_VCODE_SVID', 'taluka_id_OID', 'vcode', 'svid', unique=True),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    name = Column(String(50, u'utf8_bin'))
    regionalName = Column(String(50, u'utf8_bin'))
    svid = Column(BigInteger, nullable=False)
    taluka_id_OID = Column(ForeignKey(u'wa_talukas.id', ondelete=u'CASCADE'), nullable=False, index=True)
    vcode = Column(BigInteger, nullable=False)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))
    villages_INTEGER_IDX = Column(Integer)

    wa_taluka = relationship(u'waTaluka')


class waWhitelistEntry(Base):
    __tablename__ = 'wa_whitelist_entries'
    __table_args__ = (
        Index('UNIQUE_STATE_CONTACT_NUMBER_COMPOSITE_IDX', 'state_id_OID', 'contactNumber', unique=True),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    contactNumber = Column(BigInteger, nullable=False)
    creationDate = Column(DateTime, nullable=False)
    creator = Column(String(255, u'utf8_bin'), nullable=False)
    modificationDate = Column(DateTime, nullable=False)
    modifiedBy = Column(String(255, u'utf8_bin'), nullable=False)
    owner = Column(String(255, u'utf8_bin'))
    state_id_OID = Column(ForeignKey(u'wa_states.id'), index=True)

    wa_state = relationship(u'waState')


class waWhitelistedState(Base):
    __tablename__ = 'wa_whitelisted_states'

    id = Column(BigInteger, primary_key=True, index=True)
    state_id_OID = Column(ForeignKey(u'wa_states.id'), nullable=False, unique=True)
    creationDate = Column(DateTime)
    creator = Column(String(255, u'utf8_bin'))
    modificationDate = Column(DateTime)
    modifiedBy = Column(String(255, u'utf8_bin'))
    owner = Column(String(255, u'utf8_bin'))

    wa_state = relationship(u'waState')
