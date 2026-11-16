package com.coursehub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class ReferenceRequest {

    @NotBlank(message = "Reference title is required")
    @Size(max = 200, message = "Reference title must be at most 200 characters")
    private String referenceTitle;

    @NotBlank(message = "Reference URL is required")
    @Size(max = 1000, message = "Reference URL must be at most 1000 characters")
    @Pattern(regexp = "^(?i)https?://\\S+$", message = "Reference URL must start with http:// or https://")
    private String referenceUrl;
}
