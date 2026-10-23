package com.smartshipment.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CustomerCrudIntegrationTests {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void customerCanBeCreatedReadUpdatedAndDeleted() throws Exception {
		String email = "customer-" + UUID.randomUUID() + "@example.com";
		String createBody = """
				{"firstName":"Ada","lastName":"Lovelace","email":"%s","phone":"123456789"}
				""".formatted(email);
		String location = mockMvc.perform(post("/api/customers")
					.contentType(MediaType.APPLICATION_JSON)
					.content(createBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.firstName").value("Ada"))
				.andExpect(jsonPath("$.email").value(email))
				.andReturn().getResponse().getHeader("Location");

		assertThat(location).isNotNull();
		String id = location.substring(location.lastIndexOf('/') + 1);

		mockMvc.perform(get("/api/customers/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email));

		mockMvc.perform(put("/api/customers/{id}", id)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"firstName":"Augusta","lastName":"King","email":"%s","phone":"987654321"}
							""".formatted(email)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Augusta"))
				.andExpect(jsonPath("$.phone").value("987654321"));

		mockMvc.perform(delete("/api/customers/{id}", id))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/customers/{id}", id))
				.andExpect(status().isNotFound());
	}
}