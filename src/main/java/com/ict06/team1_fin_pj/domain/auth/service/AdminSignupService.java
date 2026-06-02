package com.ict06.team1_fin_pj.domain.auth.service;

import com.ict06.team1_fin_pj.common.dto.auth.AdminSignupRequestDto;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.DepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.PositionEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.RoleEntity;
import com.ict06.team1_fin_pj.domain.employee.repository.AdDepartmentRepository;
import com.ict06.team1_fin_pj.domain.employee.repository.AdPositionRepository;
import com.ict06.team1_fin_pj.domain.employee.repository.AdRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminSignupService {

    private static final int ADMIN_ROLE_ID = 1;

    private final EmpRepository empRepository;
    private final AdDepartmentRepository departmentRepository;
    private final AdPositionRepository positionRepository;
    private final AdRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String signup(AdminSignupRequestDto request) {
        validateRequiredFields(request);

        if (empRepository.existsByEmpId(request.getEmpId().trim())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (empRepository.existsByPhone(request.getPhone().trim())) {
            throw new IllegalArgumentException("이미 사용 중인 연락처입니다.");
        }
        if (empRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        DepartmentEntity department = departmentRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("기본 부서가 등록되어 있지 않습니다."));
        PositionEntity position = positionRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("기본 직급이 등록되어 있지 않습니다."));
        RoleEntity adminRole = roleRepository.findById(ADMIN_ROLE_ID)
                .orElseThrow(() -> new IllegalStateException("관리자 권한이 등록되어 있지 않습니다."));

        String empNo = generateEmpNo();
        String empId = request.getEmpId().trim();

        empRepository.save(EmpEntity.builder()
                .empNo(empNo)
                .empId(empId)
                .password(passwordEncoder.encode(request.getPassword()))
                .name(empId)
                .email(request.getEmail().trim())
                .phone(request.getPhone().trim())
                .bank("미등록")
                .accountNo("ADMIN-" + empNo)
                .department(department)
                .position(position)
                .role(adminRole)
                .hireDate(LocalDate.now())
                .build());

        return empNo;
    }

    private void validateRequiredFields(AdminSignupRequestDto request) {
        if (request == null
                || isBlank(request.getEmpId())
                || isBlank(request.getPassword())
                || isBlank(request.getPhone())
                || isBlank(request.getEmail())) {
            throw new IllegalArgumentException("모든 항목을 입력해 주세요.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String generateEmpNo() {
        String yearPrefix = String.valueOf(LocalDate.now().getYear());
        int maxNumber = empRepository.findEmpNosByYearPrefix(yearPrefix).stream()
                .map(empNo -> empNo.substring(yearPrefix.length()))
                .filter(number -> number.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        return yearPrefix + String.format("%04d", maxNumber + 1);
    }
}
