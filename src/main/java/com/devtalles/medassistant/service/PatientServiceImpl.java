package com.devtalles.medassistant.service;

import com.devtalles.medassistant.domain.Patient;
import com.devtalles.medassistant.dto.PatientInfo;
import com.devtalles.medassistant.repository.PatientRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class PatientServiceImpl implements PatientService {

	private final PatientRepository patientRepository;

	@Transactional(readOnly = true)
	@Override
	public PatientInfo getPatientInfo(Long patientId) {
		log.info("Retrieving patient with id {}", patientId);
		/// Lanzar errors (throw) es buena practica cuando trabajamos con controladores.
		/// Pero esto será implementado en un TOOL, por lo que es mejor retornar null.
		/// EL consumidor no es un controller, si no un TOOL. Si lanzaramos una exception, puede cortar
		/// la conversación.
		return patientRepository.findById(patientId)
			.map(this::toPatientInfo)
			.orElse(null);
	}

	PatientInfo toPatientInfo(Patient patient) {
		return new PatientInfo(
			patient.getFirstName(),
			patient.getLastName(),
			patient.getDateOfBirth()
				.toString(),
			patient.getAllergies(),
			patient.getConditions()
		);
	}
}
