package com.devtalles.medassistant.controller;

import com.devtalles.medassistant.dto.ChatRequestDTO;
import com.devtalles.medassistant.service.AssistantService;
import com.devtalles.medassistant.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@AllArgsConstructor
public class ChatController {

	private final AssistantService assistantService;
	private final SecurityUtils securityUtils;

	@PostMapping
	public ResponseEntity<String> chat(
		@RequestBody @Valid ChatRequestDTO chatRequest,
		@AuthenticationPrincipal Jwt jwt,
		Authentication authentication) {
		Long userId = jwt.getClaim("userId");
		String role = securityUtils.extractRole(authentication);
		return ResponseEntity.ok(assistantService.chat(chatRequest.prompt(), chatRequest.model(), userId, role));
	}

	// text/event-stream: Le dice al navegador que la respuesta es un flujo de eventos, lo que permite recibir datos en tiempo real.
	// No es un archivo único, si no una secuencia de eventos.
	// charset=UTF-8: Permite que no recibamos símbolos extraños. Emojis, acentos, etc. Se recomienda siempre usar UTF-8.
	@PostMapping(value = "/stream", produces = "text/event-stream; charset=UTF-8")
	public Flux<String> chatStream(@RequestBody @Valid ChatRequestDTO chatRequest, @AuthenticationPrincipal Jwt jwt,
		Authentication authentication) {
		Long userId = jwt.getClaim("userId");
		String role = securityUtils.extractRole(authentication);
		return assistantService.chatStream(chatRequest.prompt(), chatRequest.model(), userId, role);
	}

	@PostMapping(value = "/explain")
	public ResponseEntity<String> explainCondition(@RequestBody @Valid ChatRequestDTO chatRequest) {
		return ResponseEntity.ok(assistantService.explainCondition(chatRequest.prompt(), chatRequest.model()));
	}

	@PostMapping(value = "/analyze")
	public ResponseEntity<String> analyzeSymptoms(@RequestBody @Valid ChatRequestDTO chatRequest,
		@AuthenticationPrincipal Jwt jwt) {
		Long userId = jwt.getClaim("userId");
		return ResponseEntity.ok(assistantService.analyzeSymptoms(chatRequest.prompt(), chatRequest.model(), userId));
	}

	@PostMapping(value = "/diagnose")
	public ResponseEntity<String> diagnoseWithReasoning(@RequestBody @Valid ChatRequestDTO chatRequest,
		@AuthenticationPrincipal Jwt jwt) {
		Long userId = jwt.getClaim("userId");
		return ResponseEntity.ok(assistantService.diagnoseWithReasoning(chatRequest.prompt(), chatRequest.model(), userId));
	}

	@PostMapping(value = "/consult")
	public ResponseEntity<String> consult(@RequestBody @Valid ChatRequestDTO chatRequest,
		@AuthenticationPrincipal Jwt jwt) {
		Long userId = jwt.getClaim("userId");
		return ResponseEntity.ok(assistantService.consult(chatRequest.prompt(), chatRequest.model(), userId));
	}
}
