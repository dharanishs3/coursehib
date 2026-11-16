package com.coursehub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseMaterialResponse {

    private Long materialId;
    private String materialTitle;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String downloadUrl;
}
