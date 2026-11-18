package com.devtalles.medassistant.service;

import com.devtalles.medassistant.domain.Appointment;
import com.devtalles.medassistant.domain.Doctor;
import com.devtalles.medassistant.dto.AppointmentInfo;
import com.devtalles.medassistant.repository.AppointmentRepository;
import com.devtalles.medassistant.repository.DoctorRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

	private final DoctorRepository doctorRepository;
	private final AppointmentRepository appointmentRepository;

	public List<AppointmentInfo> findAvailableAppointments(String specialty, LocalDate date) {
		log.info("Finding available appointments for specialty {} on date {}", specialty, date);
		List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
		if (doctors.isEmpty()) {
			log.info("No doctors found for specialty {}", specialty);
			return List.of();
		}
		Map<Long, String> doctorNames = buildDoctorNameMap(doctors);
		List<Long> doctorIds = new ArrayList<>(doctorNames.keySet());
		var appointments = appointmentRepository.findByDoctorIdInAndDateAndAvailableTrue(doctorIds, date);
		return toAppointmentInfoList(appointments, doctorNames, specialty);
	}

	@Transactional
	@Override
	public String bookAppointment(String specialty, LocalDate date, LocalTime time, Long patientId) {
		log.info("Booking appointment for specialty {} on date {} at time {} for patient {}", specialty, date, time,
			patientId);
		// Verificar que tengamos doctores con esa especialidad.
		List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
		if (doctors.isEmpty()) {
			log.warn("No doctors found for specialty {}", specialty);
			return "No se encontró la especialidad: " + specialty;
		}
		List<Long> doctorIds = doctors.stream()
			.map(Doctor::getId)
			.toList();
		Appointment availableAppointment = findAvailableAppointment(doctorIds, date, time);
		if (availableAppointment == null) {
			log.warn("No appointment found for specialty {}", specialty);
			return "No hay turnos disponibles.";
		}
		availableAppointment.setAvailable(false);
		availableAppointment.setPatientId(patientId);
		appointmentRepository.save(availableAppointment);
		log.info("Appointment booked successfully for specialty {} on date {} at time {} for patient {}", specialty, date,
			time, patientId);
		return "Turno reservado con éxito.";
	}

	// @Transactional(readOnly = true) Esto mejora el procesamiento de lectura.
	// Es buena práctica.
	private Appointment findAvailableAppointment(List<Long> doctorIds, LocalDate date, LocalTime time) {
		return appointmentRepository.findByDoctorIdInAndDateAndStartTimeAndAvailableTrue(doctorIds, date, time)
			.stream()
			.findFirst()
			.orElse(null);
	}

	private Map<Long, String> buildDoctorNameMap(List<Doctor> doctors) {
		return doctors.stream()
			.collect(Collectors.toMap(Doctor::getId, d -> d.getFirstName() + " " + d.getLastName()));
	}

	private List<AppointmentInfo> toAppointmentInfoList(List<Appointment> appointments, Map<Long, String> doctorNames,
		String specialty) {
		return appointments.stream()
			.map(appontment ->
				new AppointmentInfo(
					doctorNames.get(appontment.getDoctorId()),
					specialty,
					appontment.getDate()
						.toString(),
					appontment.getStartTime()
						.toString())
			)
			.toList();
	}

}
