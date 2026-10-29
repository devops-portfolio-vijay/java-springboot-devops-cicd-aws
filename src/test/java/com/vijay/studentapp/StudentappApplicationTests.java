package com.vijay.studentapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentappApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired StudentRepository repository;

    private static final String STUDENT = """
            {"name":"Vijay","email":"vijay@example.com","course":"AWS DevOps"}
            """;

    @BeforeEach
    void resetDatabase() {
        repository.deleteAll();
    }

    @Test
    void completeCrudLifecycle() throws Exception {
        mvc.perform(post("/students").contentType("application/json").content(STUDENT))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Vijay"));
        long id = repository.findAll().getFirst().getId();
        mvc.perform(get("/students")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/students/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("vijay@example.com"));
        mvc.perform(put("/students/{id}", id).contentType("application/json")
                .content(STUDENT.replace("AWS DevOps", "Cloud Security")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.course").value("Cloud Security"));
        mvc.perform(delete("/students/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/students/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/students")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void missingStudentReturns404ForReadUpdateAndDelete() throws Exception {
        mvc.perform(get("/students/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Student not found"));
        mvc.perform(put("/students/999999").contentType("application/json").content(STUDENT))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/students/999999")).andExpect(status().isNotFound());
    }

    @Test
    void rejectsBlankFieldsAndInvalidEmail() throws Exception {
        mvc.perform(post("/students").contentType("application/json")
                .content("{\"name\":\" \",\"email\":\"invalid\",\"course\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.course").exists());
        mvc.perform(get("/students")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsOversizedFields() throws Exception {
        mvc.perform(post("/students").contentType("application/json")
                .content(STUDENT.replace("Vijay", "x".repeat(256))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void invalidUpdatePreservesExistingStudent() throws Exception {
        mvc.perform(post("/students").contentType("application/json").content(STUDENT))
                .andExpect(status().isCreated());
        long id = repository.findAll().getFirst().getId();
        mvc.perform(put("/students/{id}", id).contentType("application/json")
                .content(STUDENT.replace("vijay@example.com", "invalid")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/students/{id}", id))
                .andExpect(jsonPath("$.email").value("vijay@example.com"));
    }

    @Test
    void rejectsMalformedRequests() throws Exception {
        mvc.perform(post("/students").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/students/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(post("/students").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesHealthReadinessAndReleaseInfo() throws Exception {
        for (String path : new String[]{"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }
        mvc.perform(get("/actuator/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.app.version").value("test"));
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }

    @Test
    void servesDemoPage() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Student Management")));
    }
}
