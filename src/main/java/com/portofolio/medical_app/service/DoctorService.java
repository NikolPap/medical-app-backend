package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.DoctorResponseDto;
import com.portofolio.medical_app.mapper.DoctorMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final AppUserRepository appUserRepository;
    private final DoctorMapper doctorMapper;

    public List<DoctorResponseDto> getAllDoctors() {
        return appUserRepository.findByRole("ROLE_DOCTOR")
                .stream()
                .map(doctorMapper::toDto)
                .toList();
    }
}