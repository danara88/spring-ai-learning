package com.devtalles.medassistant.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ConditionSummary(
	// Spring AI lee JsonPropertyDescription al momento de general
	// el JSON schema. Le da al modelo contexto sobre lo que se espera de cada campo.
	// Esto guía la calidad de la respuesta.
	// La descrición no es solo documentación, si no también una instrucción para el modelo.
	@JsonPropertyDescription("Nombre de la condición médica")
	String conditionName,

	@JsonPropertyDescription("Descripción accesible para pacientes, sin jerga médica necesaria")
	String description,

	@JsonPropertyDescription("Síntomas frecuentes asociados a esta condición")
	String commonSymptoms,

	@JsonPropertyDescription("Indicaciones claras de cuándo buscar atención médica profesional")
	String whenToSeeADoctor,

	@JsonPropertyDescription("Nivel de gravedad general de la condición")
	Severity severity
) {
}
