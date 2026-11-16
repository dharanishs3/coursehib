package com.coursehub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseReferenceResponse {

    private Long referenceId;
    private String referenceTitle;
    private String referenceUrl;
}
