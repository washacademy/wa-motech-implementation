package org.motechproject.wa.washacademy.domain;

import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;
import org.motechproject.mds.domain.MdsEntity;

import javax.jdo.annotations.Unique;
import javax.validation.constraints.NotNull;

/**
 * Course entity to store the contents
 */
@Entity(tableName = "wash_course")
public class WaCourse extends MdsEntity {

    @Field
    @Unique
    private String name;

    @Field(type = "text")
    private String content;

    @Field
    @NotNull
    private Integer noOfChapters;

    @Field
    @NotNull
    private Integer passingScore;



    public WaCourse() {
    }

    public WaCourse(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public WaCourse(String name, String content, Integer noOfChapters, Integer passingScore){
        this.name = name;
        this.content = content;
        this.passingScore = passingScore;
        this.noOfChapters = noOfChapters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getNoOfChapters() {   return noOfChapters;   }

    public void setNoOfChapters(Integer noOfChapters) {   this.noOfChapters = noOfChapters;    }

    public Integer getPassingScore() {   return passingScore;    }

    public void setPassingScore(Integer passingScore) {  this.passingScore = passingScore;    }
}
