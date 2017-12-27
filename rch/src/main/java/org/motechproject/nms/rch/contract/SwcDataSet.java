package org.motechproject.nms.rch.contract;

import org.motechproject.nms.swcUpdate.contract.SwcRecord;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "NewDataSet")
@XmlAccessorType(XmlAccessType.NONE)
public class SwcDataSet {

    private List<SwcRecord> records;

    public List<SwcRecord> getRecords() {
        return records;
    }

    @XmlElement(name = "Records")
    public void setRecords(List<SwcRecord> records) {
        this.records = records;
    }
}
