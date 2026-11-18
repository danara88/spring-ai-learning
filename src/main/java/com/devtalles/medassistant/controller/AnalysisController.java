package com.devtalles.medassistant.controller;

import com.devtalles.medassistant.dto.ChatRequestDTO;
import com.devtalles.medassistant.dto.analysis.ConditionSummary;
import com.devtalles.medassistant.dto.analysis.QueryClassification;
import com.devtalles.medassistant.dto.analysis.SymptomAnalysis;
import com.devtalles.medassistant.service.AnalysisService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis")
@AllArgsConstructor
public class AnalysisController {

	private final AnalysisService analysisService;

	@PostMapping("/condition")
	public ResponseEntity<ConditionSummary> summarizeCondition(@Valid @RequestBody ChatRequestDTO request,
		@AuthenticationPrincipal Jwt jwt) {
		Long userId = jwt.getClaim("userId");
		return ResponseEntity.ok(analysisService.summarizeCondition(request.prompt(), request.model(), userId));
	}

	@PostMapping("/condition/v2")
	public ResponseEntity<ConditionSummary> summarizeConditionV2(@Valid @RequestBody ChatRequestDTO request,
		@AuthenticationPrincipal Jwt jwt) {
		Long userId = jwt.getClaim("userId");
		return ResponseEntity.ok(analysisService.summarizeConditionV2(request.prompt(), request.model(), userId));
	}

	@PostMapping("/conditions")
	public ResponseEntity<List<ConditionSummary>> getRelatedConditions(@Valid @RequestBody ChatRequestDTO request,
		@AuthenticationPrincipal Jwt jwt) {
		Long userId = jwt.getClaim("userId");
		return ResponseEntity.ok(analysisService.listRelatedConditions(request.prompt(), request.model(), userId));
	}

	@PostMapping("/symptoms")
	public ResponseEntity<SymptomAnalysis> analyzeSymptoms(@Valid @RequestBody ChatRequestDTO request,
		@AuthenticationPrincipal Jwt jwt) {
		Long userId = jwt.getClaim("userId");
		return ResponseEntity.ok(analysisService.analyzeSymptoms(request.prompt(), request.model(), userId));
	}

	@PostMapping("/classification")
	public ResponseEntity<QueryClassification> classify(@Valid @RequestBody ChatRequestDTO request,
		@AuthenticationPrincipal Jwt jwt) {
		Long userId = jwt.getClaim("userId");
		return ResponseEntity.ok(analysisService.classifyQuery(request.prompt(), request.model(), userId));
	}
}
