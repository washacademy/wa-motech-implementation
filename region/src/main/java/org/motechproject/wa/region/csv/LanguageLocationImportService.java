package org.motechproject.wa.region.csv;

import java.io.IOException;
import java.io.Reader;

public interface LanguageLocationImportService {

    void importData(Reader reader) throws IOException;
}
