package com.itjob.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NameRequest {

    @NotBlank(message = "NAME_REQUIRED")
    @Size(max = 100, message = "NAME_TOO_LONG")
    private String name;
}
