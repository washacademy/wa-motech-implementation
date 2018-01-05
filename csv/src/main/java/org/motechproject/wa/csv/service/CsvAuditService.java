package org.motechproject.wa.csv.service;

public interface CsvAuditService {

    void auditSuccess(String file, String endpoint);
    void auditFailure(String file, String endpoint, String failure);
}
