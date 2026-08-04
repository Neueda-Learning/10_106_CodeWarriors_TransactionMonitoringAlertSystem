package com.monitoring.transactions.Alerts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AlertsControllerTest {

    @Mock
    private AlertsService alertsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AlertsController(alertsService)).build();
    }

    @Test
    void createAlert_shouldReturnCreated() throws Exception {
        when(alertsService.createAlert(any(Alerts.class))).thenReturn(1);

        mockMvc.perform(post("/alerts")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                            "transactionId": 1001,
                            "ruleId": 2002,
                            "alertReason": "High amount detected",
                            "severity": "HIGH",
                            "oldStatus": "PENDING",
                            "newStatus": "FLAGGED"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Alert created successfully"))
                .andExpect(jsonPath("$.rowsAffected").value(1));

        verify(alertsService).createAlert(any(Alerts.class));
    }

    @Test
    void createAlert_shouldReturnBadRequestWhenServiceThrowsValidationError() throws Exception {
        when(alertsService.createAlert(any(Alerts.class)))
                .thenThrow(new RuntimeException("Transaction ID cannot be null"));

        mockMvc.perform(post("/alerts")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                            "ruleId": 2002
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Transaction ID cannot be null"));

        verify(alertsService).createAlert(any(Alerts.class));
    }

    @Test
    void createAlert_shouldReturnInternalServerErrorWhenUnexpectedFailureOccurs() throws Exception {
        when(alertsService.createAlert(any(Alerts.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        mockMvc.perform(post("/alerts")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                            "transactionId": 1001,
                            "ruleId": 2002
                        }
                        """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Database unavailable"));
    }

    @Test
    void getAllAlerts_shouldReturnOkAndAlertsList() throws Exception {
        when(alertsService.getAllAlerts()).thenReturn(List.of(new Alerts(), new Alerts()));

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(alertsService).getAllAlerts();
    }

    @Test
    void getAllAlerts_shouldReturnInternalServerErrorWhenServiceFails() throws Exception {
        when(alertsService.getAllAlerts()).thenThrow(new RuntimeException("Failed to fetch alerts"));

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to fetch alerts"));
    }

    @Test
    void getAlertById_shouldReturnOkAndAlert() throws Exception {
        when(alertsService.getAlertById(10L)).thenReturn(new Alerts());

        mockMvc.perform(get("/alerts/10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));

        verify(alertsService).getAlertById(10L);
    }

    @Test
    void getAlertById_shouldReturnNotFoundWhenAlertMissing() throws Exception {
        when(alertsService.getAlertById(99L)).thenThrow(new RuntimeException("Alert not found"));

        mockMvc.perform(get("/alerts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Alert not found"));
    }

    @Test
    void getAlertById_shouldReturnBadRequestWhenIdInvalid() throws Exception {
        when(alertsService.getAlertById(0L)).thenThrow(new RuntimeException("Invalid Alert ID"));

        mockMvc.perform(get("/alerts/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid Alert ID"));
    }

    @Test
    void getAlertById_shouldReturnInternalServerErrorWhenUnexpectedErrorOccurs() throws Exception {
        when(alertsService.getAlertById(5L)).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/alerts/5"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }

    @Test
    void updateAlertStatus_shouldReturnOkAndSuccessPayload() throws Exception {
        when(alertsService.updateAlertStatus(eq(5L), eq("PENDING"), eq("RESOLVED"))).thenReturn(1);

        mockMvc.perform(put("/alerts/5/status")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                            "oldStatus": "PENDING",
                            "newStatus": "RESOLVED"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Alert status updated successfully"))
                .andExpect(jsonPath("$.rowsAffected").value(1));

        verify(alertsService).updateAlertStatus(5L, "PENDING", "RESOLVED");
    }

    @Test
    void updateAlertStatus_shouldReturnBadRequestWhenStatusEmpty() throws Exception {
        when(alertsService.updateAlertStatus(eq(5L), eq(""), eq("RESOLVED")))
                .thenThrow(new RuntimeException("Status cannot be empty"));

        mockMvc.perform(put("/alerts/5/status")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                            "oldStatus": "",
                            "newStatus": "RESOLVED"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Status cannot be empty"));
    }

    @Test
    void updateAlertStatus_shouldReturnNotFoundWhenAlertMissing() throws Exception {
        when(alertsService.updateAlertStatus(eq(10L), eq("PENDING"), eq("RESOLVED")))
                .thenThrow(new RuntimeException("Alert not found"));

        mockMvc.perform(put("/alerts/10/status")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                            "oldStatus": "PENDING",
                            "newStatus": "RESOLVED"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Alert not found"));
    }

    @Test
    void updateAlertStatus_shouldReturnInternalServerErrorWhenUnexpectedErrorOccurs() throws Exception {
        when(alertsService.updateAlertStatus(eq(8L), eq("PENDING"), eq("RESOLVED")))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(put("/alerts/8/status")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                            "oldStatus": "PENDING",
                            "newStatus": "RESOLVED"
                        }
                        """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }

    @Test
    void deleteAlert_shouldReturnOkAndSuccessPayload() throws Exception {
        when(alertsService.deleteAlert(7L)).thenReturn(1);

        mockMvc.perform(delete("/alerts/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Alert deleted successfully"))
                .andExpect(jsonPath("$.rowsAffected").value(1));

        verify(alertsService).deleteAlert(7L);
    }

    @Test
    void deleteAlert_shouldReturnNotFoundWhenAlertMissing() throws Exception {
        when(alertsService.deleteAlert(7L)).thenThrow(new RuntimeException("Alert not found"));

        mockMvc.perform(delete("/alerts/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Alert not found"));
    }

    @Test
    void deleteAlert_shouldReturnBadRequestWhenIdInvalid() throws Exception {
        when(alertsService.deleteAlert(0L)).thenThrow(new RuntimeException("Invalid Alert ID"));

        mockMvc.perform(delete("/alerts/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid Alert ID"));
    }

    @Test
    void deleteAlert_shouldReturnInternalServerErrorWhenUnexpectedErrorOccurs() throws Exception {
        when(alertsService.deleteAlert(11L)).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(delete("/alerts/11"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }
}