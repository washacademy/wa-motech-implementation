package org.motechproject.wa.swcUpdate.service;

import java.io.IOException;
import java.io.Reader;

public interface SwcUpdateImportService {

    void importLanguageData(Reader reader) throws IOException;

    void importMSISDNData(Reader reader) throws IOException;
}
