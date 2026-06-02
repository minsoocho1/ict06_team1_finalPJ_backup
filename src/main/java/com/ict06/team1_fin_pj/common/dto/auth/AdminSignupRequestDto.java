package com.ict06.team1_fin_pj.common.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSignupRequestDto {
    private String empId;
    private String password;
    private String phone;
    private String email;
}
