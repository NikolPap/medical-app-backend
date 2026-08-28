package com.portofolio.medical_app.repository;

import com.portofolio.medical_app.entity.Appoitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appoitment, Long> {

}
