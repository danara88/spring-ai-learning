package com.devtalles.medassistant.tools;

import com.devtalles.medassistant.dto.PatientInfo;
import com.devtalles.medassistant.service.PatientService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class PatientInfoTool {

	private final PatientService patientService;

	@Tool(description = "Consultar el historial clínico de un paciente por su ID. " +
		"Usar cuando el usuario pregunte por su historial médico, alergia o condiciones.")
	public PatientInfo getPatientInfo(
		@ToolParam(description = "ID numérico del paciente")
		Long patientId,
		ToolContext context
	) {
		log.info("[Tool] Retrieving patient info for patientId: {}", patientId);
		Long userId = (Long) context.getContext()
			.get("userId");
		log.info("[Tool] Retrieving Tool Context userId: {}", userId);
		if (! patientId.equals(userId)) {
			log.warn("[Tool] Unauthorized access attempt by userId: {} for patientId: {}", userId, patientId);
			return null;
		}
		return patientService.getPatientInfo(patientId);
	}
}
