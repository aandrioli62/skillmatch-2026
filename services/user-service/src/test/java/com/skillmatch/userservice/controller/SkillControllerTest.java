package com.skillmatch.userservice.controller;

import com.skillmatch.userservice.config.TestSecurityConfig;
import com.skillmatch.userservice.dto.response.SkillResponse;
import com.skillmatch.userservice.exception.GlobalExceptionHandler;
import com.skillmatch.userservice.mapper.SkillMapper;
import com.skillmatch.userservice.model.Skill;
import com.skillmatch.userservice.repository.SkillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SkillController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("SkillController — @WebMvcTest")
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SkillRepository skillRepository;

    @MockBean
    private SkillMapper skillMapper;

    private Skill skill(String name) {
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setName(name);
        skill.setCategory("Sviluppo software");
        return skill;
    }

    private SkillResponse response(String name) {
        SkillResponse response = new SkillResponse();
        response.setName(name);
        return response;
    }

    @Test
    @DisplayName("blank search → first 20 skills alphabetically")
    void searchSkills_blankQuery_returnsAll() throws Exception {
        Skill aws = skill("AWS");
        Skill java = skill("Java");
        when(skillRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc(""))
                .thenReturn(List.of(aws, java));
        when(skillMapper.toResponse(aws)).thenReturn(response("AWS"));
        when(skillMapper.toResponse(java)).thenReturn(response("Java"));

        mockMvc.perform(get("/api/v1/skills")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("AWS"));
    }

    @Test
    @DisplayName("search param is forwarded to the repository")
    void searchSkills_withQuery_forwardsSearchTerm() throws Exception {
        Skill java = skill("Java");
        when(skillRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc("java"))
                .thenReturn(List.of(java));
        when(skillMapper.toResponse(any())).thenReturn(response("Java"));

        mockMvc.perform(get("/api/v1/skills").param("search", "java")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"));
    }

    @Test
    @DisplayName("no matches → empty list")
    void searchSkills_noMatches_returnsEmptyList() throws Exception {
        when(skillRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc("xyz"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/skills").param("search", "xyz")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
