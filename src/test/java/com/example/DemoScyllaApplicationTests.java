package com.example;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.IdGenerator;
import org.springframework.util.SimpleIdGenerator;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureJsonTesters
@TestMethodOrder(OrderAnnotation.class)
@Testcontainers(disabledWithoutDocker = true)
class DemoScyllaApplicationTests {

	@LocalServerPort
	int port;

	@Autowired
	RestClient.Builder restClientBuilder;

	RestClient restClient;

	@Autowired
	JacksonTester<City> cityTester;

	@Autowired
	JacksonTester<List<City>> listTester;

	@BeforeEach
	void setUp() {
		this.restClient = this.restClientBuilder.baseUrl("http://localhost:" + port)
			.defaultStatusHandler(new DefaultResponseErrorHandler() {
				@Override
				public void handleError(ClientHttpResponse response) {
					// NO-OP
				}
			})
			.build();
	}

	@Test
	@Order(1)
	void getCities() throws Exception {
		ResponseEntity<List<City>> response = this.restClient.get()
			.uri("/cities")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(this.listTester.write(response.getBody())).isEqualToJson("""
				[
				  {
				    "id": "00000000-0000-0000-0000-000000000001",
				    "name": "Tokyo"
				  },
				  {
				    "id": "00000000-0000-0000-0000-000000000002",
				    "name": "Osaka"
				  },
				  {
				    "id": "00000000-0000-0000-0000-000000000003",
				    "name": "Kyoto"
				  }
				]
				""");
	}

	@Test
	@Order(2)
	void postCities() throws Exception {
		{
			ResponseEntity<City> response = this.restClient.post().uri("/cities").body("""
						{"name": "Toyama"}
					""").contentType(MediaType.APPLICATION_JSON).retrieve().toEntity(City.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
			assertThat(this.cityTester.write(response.getBody())).isEqualToJson("""
					{
					  "id": "00000000-0000-0000-0000-000000000004",
					  "name": "Toyama"
					}
					""");
		}
		{
			ResponseEntity<List<City>> response = this.restClient.get()
				.uri("/cities")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {
				});
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(this.listTester.write(response.getBody())).isEqualToJson("""
					[
					  {
					    "id": "00000000-0000-0000-0000-000000000001",
					    "name": "Tokyo"
					  },
					  {
					    "id": "00000000-0000-0000-0000-000000000002",
					    "name": "Osaka"
					  },
					  {
					    "id": "00000000-0000-0000-0000-000000000003",
					    "name": "Kyoto"
					  },
					  {
					    "id": "00000000-0000-0000-0000-000000000004",
					    "name": "Toyama"
					  }
					]
					""");
		}
	}

	@TestConfiguration
	static class Config {

		@Bean
		@Primary
		public IdGenerator simpleIdGenerator() {
			return new SimpleIdGenerator();
		}

		@Bean
		public CommandLineRunner clr(CityRepository cityRepository, IdGenerator idGenerator) {
			return args -> cityRepository.saveAll(Set.of(new City(idGenerator.generateId(), "Tokyo"),
					new City(idGenerator.generateId(), "Osaka"), new City(idGenerator.generateId(), "Kyoto")));
		}

	}

}