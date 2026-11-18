package com.devtalles.medassistant.service;

import com.devtalles.medassistant.domain.Doctor;
import com.devtalles.medassistant.dto.DoctorInfo;
import com.devtalles.medassistant.repository.DoctorRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class DoctorServiceImpl implements DoctorService {

	private final DoctorRepository doctorRepository;

	@Transactional(readOnly = true)
	public List<DoctorInfo> searchDoctors(String query) {
		List<Doctor> doctors = doctorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
			query, query, query);
		return doctors.stream()
			.map(this::toDoctorInfo)
			.toList();
	}

	private DoctorInfo toDoctorInfo(Doctor doctor) {
		return new DoctorInfo(
			doctor.getFirstName(),
			doctor.getFirstName(),
			doctor.getSpecialty(),
			doctor.getLicenseNumber(),
			doctor.getPhone(),
			doctor.getOffice()
		);
	}
}
