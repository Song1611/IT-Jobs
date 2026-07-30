package com.itjob.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserSkillRequest {

    private String level;
    private Integer yearsOfExperience;
}
