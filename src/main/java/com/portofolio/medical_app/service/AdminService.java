package com.portofolio.medical_app.service;

import com.portofolio.medical_app.config.Roles;
import com.portofolio.medical_app.dto.PendingDoctorResponseDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.DoctorMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AppUserRepository appUserRepository;
    private final DoctorMapper doctorMapper;
    private final EmailService emailService;

    public List<PendingDoctorResponseDto> getPendingDoctors() {
        return appUserRepository.findByRoleAndEnabledTrue(Roles.PENDING_DOCTOR)
                .stream()
                .map(doctorMapper::toPendingDto)
                .toList();
    }

    @Transactional
    public void approveDoctor(Long userId) {
        AppUser doctor = findPendingDoctor(userId);
        doctor.setRole(Roles.DOCTOR);
        appUserRepository.save(doctor);

        emailService.sendStatusEmail(doctor.getEmail(),
                "Dein Arztkonto wurde freigeschaltet. Du kannst dich jetzt einloggen.");
    }

    @Transactional
    public void rejectDoctor(Long userId) {
        AppUser doctor = findPendingDoctor(userId);
        doctor.setRole(Roles.REJECTED_DOCTOR);
        appUserRepository.save(doctor);

        emailService.sendStatusEmail(doctor.getEmail(),
                "Leider konnten wir dein Arztkonto nicht freischalten.");
    }

    private AppUser findPendingDoctor(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new AppException("Benutzer nicht gefunden.", HttpStatus.NOT_FOUND));

        if (!Roles.PENDING_DOCTOR.equals(user.getRole())) {
            throw new AppException("Dieser Benutzer wartet nicht auf Freischaltung.");
        }
        return user;
    }
}