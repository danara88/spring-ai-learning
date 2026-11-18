package com.devtalles.medassistant.repository;

import com.devtalles.medassistant.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
