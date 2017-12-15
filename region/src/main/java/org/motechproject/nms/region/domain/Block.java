package org.motechproject.nms.region.domain;

import org.codehaus.jackson.annotate.JsonBackReference;
import org.codehaus.jackson.annotate.JsonManagedReference;
import org.motechproject.mds.annotations.Cascade;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;
import org.motechproject.mds.annotations.InstanceLifecycleListeners;
import org.motechproject.mds.domain.MdsEntity;
import org.motechproject.nms.tracking.annotation.TrackClass;
import org.motechproject.nms.tracking.annotation.TrackFields;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Unique;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "wash_blocks")
@Unique(name = "UNIQUE_DISTRICT_CODE", members = { "district", "code" })
@TrackClass
@TrackFields
@InstanceLifecycleListeners
public class Block extends MdsEntity {

    @Field
    @Column(allowsNull = "false", length = 150)
    @NotNull
    @Size(min = 1, max = 150)
    private String name;

    @Field
    @Column(length = 150)
    @Size(min = 1, max = 150)
    private String regionalName;

    @Field
    @Column(allowsNull = "false", length = 7)
    @NotNull
    @Size(min = 1, max = 7)
    // File from MoH shows a 50 char string in taluka file, but a 7 char string in village.
    // Sample data shows string (i.e. '0005')
    // Email thread says number.   grrrr
    private String code;

    @Field
    private Integer identity;

    @Field
    @Column(allowsNull = "false")
    @NotNull
    @JsonBackReference
    private District district;

    @Field
    @Cascade(delete = true)
    @Persistent(mappedBy = "block", defaultFetchGroup = "false")
    @JsonManagedReference
    private List<Panchayat> panchayats;

    public Block() {
        this.panchayats = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegionalName() {
        return regionalName;
    }

    public void setRegionalName(String regionalName) {
        this.regionalName = regionalName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getIdentity() {
        return identity;
    }

    public void setIdentity(Integer identity) {
        this.identity = identity;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public List<Panchayat> getPanchayats() {
        return panchayats;
    }

    public void setPanchayats(List<Panchayat> panchayats) {
        this.panchayats = panchayats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Block block = (Block) o;

        if (name != null ? !name.equals(block.name) : block.name != null) {
            return false;
        }
        return !(code != null ? !code.equals(block.code) : block.code != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Block{" +
                "name='" + name + '\'' +
                ", code=" + code +
                '}';
    }
}
