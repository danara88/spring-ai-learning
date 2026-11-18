package com.devtalles.medassistant.controller;

import com.devtalles.medassistant.dto.IngestionResult;
import com.devtalles.medassistant.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

	private final DocumentIngestionService ingestionService;

	@PostMapping("/ingest")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<IngestionResult> ingestDocument(
		// Es como Spring representa un archivo que llega por HTTP
		// Le estamos diciendo a spring que cuando llegue un archivo en un campo llamada file,
		// lo guarde en esta variable.
		@RequestParam("file") MultipartFile file
	) {
		return ResponseEntity.ok(ingestionService.ingestNewDocument(file.getResource()));
	}
}
