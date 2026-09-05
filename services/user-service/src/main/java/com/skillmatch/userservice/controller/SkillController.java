package com.skillmatch.userservice.controller;

import com.skillmatch.userservice.dto.response.SkillResponse;
import com.skillmatch.userservice.mapper.SkillMapper;
import com.skillmatch.userservice.repository.SkillRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
@Tag(name = "Skills", description = "Shared skill catalog, used by professional profiles and project requirements")
@SecurityRequirement(name = "bearerAuth")
public class SkillController {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    @Operation(
            summary = "Search the shared skill catalog",
            description = "Case-insensitive substring match, capped at 20 results, ordered alphabetically. "
                    + "Feeds the skill autocomplete used both for a professional's own skills and for a "
                    + "company's project requirements."
    )
    @GetMapping
    public ResponseEntity<List<SkillResponse>> searchSkills(
            @Parameter(description = "Substring to search for; blank returns the first 20 skills alphabetically")
            @RequestParam(name = "search", defaultValue = "") String search) {
        List<SkillResponse> results = skillRepository
                .findTop20ByNameContainingIgnoreCaseOrderByNameAsc(search)
                .stream()
                .map(skillMapper::toResponse)
                .toList();
        return ResponseEntity.ok(results);
    }
}
