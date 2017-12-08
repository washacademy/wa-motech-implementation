package org.motechproject.nms.region.domain;

/**
 * Interface that marks a class as providing the full location hierachy.  Used along with the @ValidLocation
 * annotation
 */
public interface FullLocation {
    State getState();
    void setState(State state);

    District getDistrict();
    void setDistrict(District district);

    Block getBlock();
    void setBlock(Block block);

    Panchayat getPanchayat();
    void setPanchayat(Panchayat panchayat);
}
