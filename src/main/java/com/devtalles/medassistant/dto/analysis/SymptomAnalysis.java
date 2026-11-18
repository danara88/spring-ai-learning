package com.devtalles.medassistant.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record SymptomAnalysis(
	@JsonPropertyDescription("Lista de síntomas identificados en la consulta del paciente")
	List<String> identidiedSymptoms,

	@JsonPropertyDescription("Posibles condiciones médicas basados en los síntomas, ordenadas de más a menor probable")
	List<PossibleCondition> possibleConditions,

	@JsonPropertyDescription("Nivel de urgencia general de la situación del paciente")
	Urgency urgencyLevel,

	@JsonPropertyDescription("Recomendación general para el paciente, incluyendo si debe bsucar atención médica")
	String recommendation
) {
}
