package com.monitoring.transactions.Rules;

import java.math.BigDecimal;
import java.util.List;

import com.monitoring.transactions.Exception.GeneralizedException;
import com.monitoring.transactions.Exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RulesControllerTests {

    @Mock
    private RulesServices rulesServices;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RulesController controller = new RulesController(rulesServices);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllRules_returns200WithRules() throws Exception {
        when(rulesServices.getAllRules()).thenReturn(List.of(
                new Rules(1L, "Large Transaction", "AMOUNT_THRESHOLD", new BigDecimal("10000.00"), null, null, "HIGH", true),
                new Rules(2L, "High Velocity Check", "VELOCITY", null, 10, 5, "MEDIUM", true)));

        mockMvc.perform(get("/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].name").value("High Velocity Check"));
    }

    @Test
    void getAllRules_returns200WithEmptyList() throws Exception {
        when(rulesServices.getAllRules()).thenReturn(List.of());

        mockMvc.perform(get("/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getRuleById_returns200WhenFound() throws Exception {
        Rules expected = new Rules(1L, "Large Transaction", "AMOUNT_THRESHOLD", new BigDecimal("10000.00"), null, null, "HIGH", true);
        when(rulesServices.getRuleById(1L)).thenReturn(expected);

        mockMvc.perform(get("/rules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Large Transaction"));
    }

    @Test
    void getRuleById_returns404WhenRuleMissing() throws Exception {
        when(rulesServices.getRuleById(500L))
                .thenThrow(new GeneralizedException("Rule not found for id: 500", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/rules/500"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getRuleById_returns400WhenIdInvalid() throws Exception {
        when(rulesServices.getRuleById(0L))
                .thenThrow(new GeneralizedException("Rule id must be a positive number.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(get("/rules/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createRule_returns201WithCreatedRule() throws Exception {
        Rules created = new Rules(10L, "New Rule", "VELOCITY", null, 15, 4, "MEDIUM", true);
        when(rulesServices.createRule(any(Rules.class))).thenReturn(created);

        mockMvc.perform(post("/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Rule",
                                  "type": "VELOCITY",
                                  "threshold": null,
                                  "timeWindow": 15,
                                  "maxTransactions": 4,
                                  "severity": "MEDIUM",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.type").value("VELOCITY"));
    }

    @Test
    void createRule_returns400WhenPayloadInvalid() throws Exception {
        when(rulesServices.createRule(any(Rules.class)))
                .thenThrow(new GeneralizedException("Invalid rule input.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "type": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createRule_returns400WhenBodyMalformed() throws Exception {
        mockMvc.perform(post("/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRule_returns200WithUpdatedRule() throws Exception {
        Rules updated = new Rules(2L, "Updated Rule", "VELOCITY", null, 20, 10, "MEDIUM", true);
        when(rulesServices.updateRule(eq(2L), any(Rules.class))).thenReturn(updated);

        mockMvc.perform(put("/rules/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Rule",
                                  "type": "VELOCITY",
                                  "timeWindow": 20,
                                  "maxTransactions": 10,
                                  "severity": "MEDIUM",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Updated Rule"));
    }

    @Test
    void updateRule_returns404WhenRuleMissing() throws Exception {
        when(rulesServices.updateRule(eq(700L), any(Rules.class)))
                .thenThrow(new GeneralizedException("Rule not found for id: 700", HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/rules/700")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Rule",
                                  "type": "VELOCITY"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteRule_returns204WhenDeleted() throws Exception {
        doNothing().when(rulesServices).deleteRule(3L);

        mockMvc.perform(delete("/rules/3"))
                .andExpect(status().isNoContent());

        verify(rulesServices).deleteRule(3L);
    }

    @Test
    void deleteRule_returns404WhenRuleMissing() throws Exception {
        doThrow(new GeneralizedException("Rule not found for id: 3", HttpStatus.NOT_FOUND))
                .when(rulesServices).deleteRule(3L);

        mockMvc.perform(delete("/rules/3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
