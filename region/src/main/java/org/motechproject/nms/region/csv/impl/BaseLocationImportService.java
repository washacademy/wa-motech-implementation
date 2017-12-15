package org.motechproject.nms.region.csv.impl;

import org.apache.commons.collections.CollectionUtils;
import org.motechproject.nms.csv.exception.CsvImportDataException;
import org.motechproject.nms.csv.utils.ConstraintViolationUtils;
import org.motechproject.nms.csv.utils.CsvImporterBuilder;
import org.motechproject.nms.csv.utils.CsvInstanceImporter;
import org.motechproject.nms.csv.utils.GetInstanceByLong;
import org.motechproject.nms.csv.utils.GetInstanceByString;
import org.motechproject.nms.csv.utils.Store;
import org.motechproject.nms.region.domain.Block;
import org.motechproject.nms.region.domain.District;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.region.service.BlockService;
import org.motechproject.nms.region.service.DistrictService;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.cellprocessor.ift.CellProcessor;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

public abstract class BaseLocationImportService<T> {

    public static final String STATE = "state";
    public static final String DISTRICT = "district";
    public static final String BLOCK = "block";
    public static final String PANCHAYAT = "panchayat";

    private Class<T> type;

    public BaseLocationImportService(Class<T> type) {
        this.type = type;
    }

    @Transactional
    public void importData(Reader reader) throws IOException {
        CsvInstanceImporter<T> csvImporter = new CsvImporterBuilder()
                .setProcessorMapping(getProcessorMapping())
                .setFieldNameMapping(getFieldNameMapping())
                .createAndOpen(reader, type);
        try {
            T instance;
            while (null != (instance = csvImporter.read())) {
                createOrUpdateInstance(instance);
            }
        } catch (ConstraintViolationException e) {
            throw new CsvImportDataException(createErrorMessage(e.getConstraintViolations(),
                    csvImporter.getRowNumber()), e);
        } catch (IllegalStateException e) {
            throw new CsvImportDataException(createErrorMessage(e.getMessage(),
                csvImporter.getRowNumber()), e);
        }
    }

    protected CellProcessor mapState(final StateDataService stateDataService) {
        return new GetInstanceByLong<State>() {
            @Override
            public State retrieve(Long value) {
                return stateDataService.findByCode(value);
            }
        };
    }


    protected CellProcessor mapDistrict(final Store store, final DistrictService districtService) {
        return new GetInstanceByLong<District>() {
            @Override
            public District retrieve(Long value) {
                State state = (State) store.get(STATE);
                if (state == null) {
                    throw new IllegalStateException(String
                            .format("Unable to load District %s with a null state", value));
                }

                return districtService.findByStateAndCode(state, value);
            }
        };
    }

    protected CellProcessor mapBlock(final Store store, final BlockService blockService) {
        return new GetInstanceByString<Block>() {
            @Override
            public Block retrieve(String value) {
                District district = (District) store.get(DISTRICT);
                if (district == null) {
                    throw new IllegalStateException(String
                            .format("Unable to load Block %s with a null district", value));
                }

                return blockService.findByDistrictAndCode(district, value);
            }
        };
    }


    protected abstract void createOrUpdateInstance(T instance);

    protected abstract Map<String, CellProcessor> getProcessorMapping();

    protected abstract Map<String, String> getFieldNameMapping();

    private String createErrorMessage(Set<ConstraintViolation<?>> violations, int rowNumber) {
        if (CollectionUtils.isNotEmpty(violations)) {
            return String.format("CSV instance error [row: %d]: validation failed for instance of type %s, violations: %s", rowNumber, type.getName(), ConstraintViolationUtils.toString(violations));
        } else {
            return String.format("CSV instance error [row: %d]", rowNumber);
        }
    }

    private String createErrorMessage(String message, int rowNumber) {
        return String.format("CSV instance error [row: %d]: Error loading entities in record for instance of type %s, message: %s", rowNumber, type.getName(), message);
    }
}
