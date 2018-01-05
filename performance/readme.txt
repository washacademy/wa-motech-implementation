# TODO
- Create location, circleName & language import files
- Write script to generate MCTS import files
  - Script should generate seperate files for:
    - Mothers
    - Children
    - SWCs
  - Parameter for count should be included
  - What other toggles should the script take?
- How will we set config values?
  - Deployed services?
  - Whitelist
- How will we age records to test purging?
- How will we simulate user load & IVR
- Should a script be created that can take a clean server, import files & script for data munging and produce a deployed server for testing?

# To generate the SQLAlchemy models file
sqlacodegen --tables wa_KK_SUMMARY_RECORDS_STATUSSTATS,wa_call_content,wa_circles,wa_csv_audit_records,wa_deployed_services,wa_districts,wa_swc_cdrs,wa_front_line_workers,wa_health_blocks,wa_health_facilities,wa_health_facility_types,wa_health_sub_facilities,wa_imi_cdrs,wa_imi_csrs,wa_imi_file_audit_records,wa_inbox_call_data,wa_inbox_call_details,wa_kk_retry_records,wa_kk_summary_records,wa_languages,wa_ma_completion_records,wa_ma_course,wa_mcts_children,wa_mcts_mothers,wa_national_default_language,wa_service_usage_caps,wa_states,wa_states_join_circles,wa_subscribers,wa_subscription_errors,wa_subscription_pack_messages,wa_subscription_packs,wa_subscriptions,wa_talukas,wa_villages,wa_whitelist_entries,wa_whitelisted_states mysql://root:password@localhost/motech_data_services > models.py

For imports we need the following files with the following fields:

# States:

StateID,Name

# Circles:

Circle,State

# Districts:

DCode,Name_G,Name_E,StateID

# Talukas:

TCode,ID,Name_G,Name_E,DCode,StateID

# Census Villages:

VCode,Name_G,Name_E,TCode,DCode,StateID

# Non-Census Village

SVID,Name_G,Name_E,TCode,VCode,DCode,StateID

# HealthBlocks:

BID,Name_G,Name_E,HQ,TCode,DCode,StateID

# Health Facilities:

PID,Name_G,Name_E,BID,Facility_Type,TCode,DCode,StateID

# Health Sub-Facilities:

SID,Name_G,Name_E,PID,BID,TCode,DCode,StateID

# Languages -> Location mapping:

languagelocation code,Language,Circle,State,District,Default Language for Circle (Y/N)";

# Child beneficiaries:

State Name :	
User Name :	
Password :	
From Date (dd-mm-yyyy) :	
To Date (dd-mm-yyyy) :	
		
StateID	District_ID	Taluka_ID	HealthBlock_ID	PHC_ID	SubCentre_ID	Village_ID	ID_No	Name	Mother_ID	Whom_PhoneNo	Birthdate Entry_Type

# Mother beneficiaries:

State Name :	
User Name :	
Password :	
From Date (dd-mm-yyyy) :	
To Date (dd-mm-yyyy) :	
		
StateID	District_ID	Taluka_ID	HealthBlock_ID	PHC_ID	SubCentre_ID	Village_ID	ID_No	Name	Whom_PhoneNo	Birthdate	LMP_Date	Abortion	Outcome_Nos Entry_Type

# SWCs

State Name : State 1
User Name :
Password :
From Date (dd-mm-yyyy) :	
To Date (dd-mm-yyyy) :	
		
ID	District_ID	Taluka_ID	HealthBlock_ID	PHC_ID	SubCentre_ID	Village_ID	Name	Contact_No
