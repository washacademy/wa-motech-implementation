package org.motechproject.wa.rch.utils;

/**
 * Created by vishnu on 26/12/17.
 */

import org.motechproject.wa.swcUpdate.contract.SwcRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/**
 * Created by beehyv on 24/7/17.
 */
public final class ObjectListCleaner {

    private ObjectListCleaner() {
    }

    public static List<List<SwcRecord>> cleanSwcRecords(List<SwcRecord> anmAshaRecords) {
        List<SwcRecord> rejectedRecords = new ArrayList<>();
        List<SwcRecord> acceptedRecords = new ArrayList<>();
        List<List<SwcRecord>> full = new ArrayList<>();
        HashMap<String, Integer> motherPhoneMap = new HashMap<>();
        for (SwcRecord record : anmAshaRecords) {
            if (motherPhoneMap.containsKey(record.getMobileNo())) {
                motherPhoneMap.put(record.getMobileNo(), motherPhoneMap.get(record.getMobileNo()) + 1);
            } else {
                motherPhoneMap.put(record.getMobileNo(), 1);
            }
        }
        for (SwcRecord record : anmAshaRecords) {
            Integer count = motherPhoneMap.get(record.getMobileNo());
            if (count > 1) {
                rejectedRecords.add(record);
            } else {
                acceptedRecords.add(record);
            }
        }
        full.add(rejectedRecords);
        full.add(acceptedRecords);
        return full;
    }
}

