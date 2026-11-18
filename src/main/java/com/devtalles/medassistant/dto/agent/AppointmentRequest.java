package com.devtalles.medassistant.dto.agent;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AppointmentRequest(
	@JsonPropertyDescription("Especialidad médica solicitada, por ejemplo: 'Cardiología', 'Dermatología', etc.")
	String specialty,

	@JsonPropertyDescription("Fecha solicitada en formato yyyy-MM-dd")
	String date
) {
}
