package com.devtalles.medassistant.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
	@NotBlank(message = "The prompt must not be empty")
	String prompt,
	String model
) {
}
