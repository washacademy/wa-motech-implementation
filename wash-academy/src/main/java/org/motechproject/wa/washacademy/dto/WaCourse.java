package org.motechproject.wa.washacademy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.jdo.annotations.Unique;

/**
 * Course object containing all the course related information and metadata
 */
public class WaCourse {

    @JsonProperty("name")
    private String name;

    @JsonProperty("courseVersion")
    private Long version;

    @JsonProperty("chapters")
    private String content;

    @JsonProperty("courseId")
    @Unique
    private int courseId;


    public WaCourse() {
    }

    public WaCourse(String name, Long version, String content,Integer courseId ) {
        this.name = name;
        this.version = version;
        this.content = content;
        this.courseId = courseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getCourseId() {     return courseId;   }

    public void setCourseId(int courseId) {     this.courseId = courseId;    }
}
