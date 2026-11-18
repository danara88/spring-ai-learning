package com.devtalles.medassistant.exception;

import java.nio.file.AccessDeniedException;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(value = RestClientException.class)
	public ResponseEntity<String> handleRestClientException(RestClientException ex) {
		log.error("Error al consumir la API externa: {}", ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body("El servicio externo no está disponible en este momento.");
	}

	// Nuestras tools hacen parse de nuestras fechas. Pero, en ocaciones puede que no
	// formaten bien la fecha por lo que se lanza una exception.
	@ExceptionHandler(value = DateTimeParseException.class)
	public ResponseEntity<String> handleDateTimeParseException(DateTimeParseException ex) {
		log.error("Error al parsear la fecha: {}", ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body("Formato de fecha inválido. Use el formato yyyy-MM-dd.");
	}

	@ExceptionHandler(value = IllegalArgumentException.class)
	public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
		log.error("Error al validar la fecha: {}", ex.getMessage(), ex);
		return ResponseEntity.badRequest()
			.body(ex.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<String> handleAccessDeniedtException(AccessDeniedException ex) {
		log.error("Access denied: {}", ex.getMessage(), ex);
		return ResponseEntity.status(403)
			.body("No tienes permisos para realizar esta acción.");
	}
}
