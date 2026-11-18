package com.devtalles.medassistant.agent.orchestrastor;

import com.devtalles.medassistant.dto.PatientInfo;
import com.devtalles.medassistant.service.PatientService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/// Este es el segundo worker involucrado en nuestro orquestador, encargado de buscar el historial del paciente
@Component
@Slf4j
@RequiredArgsConstructor
public class HistoryWorker {

	private final PatientService patientService;

	public String lookup(Long userId) {
		PatientInfo patientInfo = patientService.getPatientInfo(userId);
		if (Objects.isNull(patientInfo)) {
			log.warn("No patient info found for userId: {}", userId);
			return "No se encontró historial del paciente.";
		}
		String result = String.format(
			"Paciente: %s %s | Fecha de nacimiento: %s | Alergias: %s | Condiciones: %s",
			patientInfo.firstName(),
			patientInfo.lastName(),
			patientInfo.dateOfBirth(),
			patientInfo.allergies() != null
				? patientInfo.allergies()
				: "Ninguna",
			patientInfo.conditions() != null
				? patientInfo.conditions()
				: "Ninguna"
		);
		log.info("Patient history lookup result: {}", result);
		return result;
	}
}
