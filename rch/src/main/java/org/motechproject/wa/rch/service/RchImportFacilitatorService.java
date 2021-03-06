package org.motechproject.wa.rch.service;

import org.joda.time.LocalDate;
import org.motechproject.wa.rch.domain.RchImportFacilitator;
import org.motechproject.wa.rch.domain.RchUserType;
import org.motechproject.wa.rch.exception.RchFileManipulationException;

import java.util.List;


public interface RchImportFacilitatorService {

    void createImportFileAudit(RchImportFacilitator rchImportFacilitator) throws RchFileManipulationException;

    List<RchImportFacilitator> findByImportDateAndRchUserType(LocalDate importDate, RchUserType rchUserType);
}
