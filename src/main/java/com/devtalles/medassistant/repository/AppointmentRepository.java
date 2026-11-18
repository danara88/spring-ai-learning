package com.devtalles.medassistant.repository;

import com.devtalles.medassistant.domain.Appointment;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

	// WHERE doctor_id IN (1, 3, 5) AND date = '2026-06-23' AND available = true
	List<Appointment> findByDoctorIdInAndDateAndAvailableTrue(List<Long> doctorIds, LocalDate date);

	List<Appointment> findByDoctorIdInAndDateAndStartTimeAndAvailableTrue(List<Long> doctorIds, LocalDate date,
		LocalTime startTime);
}
