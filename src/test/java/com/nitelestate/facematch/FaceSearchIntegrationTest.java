package com.nitelestate.facematch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CCTV face search end to end with a stub engine (no AWS): a worker registered with a photo is
 * indexed in the background, and a search returns them with their sponsoring resident. Also
 * checks a resident can't run searches.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FaceSearchIntegrationTest.StubEngineConfig.class)
class FaceSearchIntegrationTest {

    /** Pretends every indexed photo is the same person, so any searched face matches them. */
    static class StubEngine implements FaceEngine {
        final Map<String, String> indexed = new ConcurrentHashMap<>(); // faceId -> externalId
        public boolean isEnabled() { return true; }
        public String index(byte[] image, String externalId) {
            String faceId = "face-" + externalId;
            indexed.put(faceId, externalId);
            return faceId;
        }
        public void delete(List<String> faceIds) { faceIds.forEach(indexed::remove); }
        public List<Box> detect(byte[] image) { return List.of(new Box(0.25f, 0.25f, 0.5f, 0.5f)); }
        public List<Hit> search(byte[] image, float min, int max) {
            return indexed.entrySet().stream().map(e -> new Hit(e.getKey(), e.getValue(), 96.5f)).toList();
        }
    }

    @TestConfiguration
    static class StubEngineConfig {
        @Bean
        @Primary
        StubEngine stubFaceEngine() {
            return new StubEngine();
        }
    }

    @Autowired
    private FaceIndexEntryRepository entryRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nitel_estate_test")
            .withUsername("nitel")
            .withPassword("nitel");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }

    private String login(String email, String password) {
        var response = restTemplate.postForEntity(baseUrl() + "/auth/login",
                Map.of("identifier", email, "password", password), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("token");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private static String jpegDataUri() throws Exception {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        var out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    @Test
    @SuppressWarnings("unchecked")
    void registeredWorker_isFoundFromCctvStill() throws Exception {
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");
        String photo = jpegDataUri();

        var worker = restTemplate.postForEntity(baseUrl() + "/workers", new HttpEntity<>(Map.of(
                "fullName", "Gate Camera Suspect",
                "phone", "+2348011112001",
                "contractorName", "Acme Ltd",
                "workType", "Gardener",
                "workerCategory", "NON_DOMESTIC_STAFF",
                "startDate", LocalDate.now().toString(),
                "expectedEndDate", LocalDate.now().plusMonths(1).toString(),
                "photo", photo), authHeaders(residentToken)), Map.class);
        assertThat(worker.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long workerId = ((Number) worker.getBody().get("id")).longValue();

        // Indexing runs in the background.
        for (int i = 0; i < 50 && entryRepository.findBySubjectTypeAndSubjectId(FaceSubjectType.WORKER, workerId).isEmpty(); i++) {
            Thread.sleep(100);
        }
        assertThat(entryRepository.findBySubjectTypeAndSubjectId(FaceSubjectType.WORKER, workerId)).hasSize(1);

        var forbidden = restTemplate.postForEntity(baseUrl() + "/face-search",
                new HttpEntity<>(Map.of("image", photo), authHeaders(residentToken)), Map.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var result = restTemplate.postForEntity(baseUrl() + "/face-search",
                new HttpEntity<>(Map.of("image", photo, "note", "Burglary at No. 12, 2 Nov"), authHeaders(adminToken)), Map.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().get("facesFound")).isEqualTo(1);
        var faces = (List<Map<String, Object>>) result.getBody().get("faces");
        var candidates = (List<Map<String, Object>>) faces.get(0).get("candidates");
        var match = candidates.stream().filter(c -> ((Number) c.get("subjectId")).longValue() == workerId).findFirst().orElseThrow();
        assertThat(match.get("subjectType")).isEqualTo("WORKER");
        assertThat(match.get("fullName")).isEqualTo("Gate Camera Suspect");
        assertThat(match.get("sponsorName")).isNotNull();
        assertThat(((Number) match.get("similarity")).doubleValue()).isGreaterThan(90);
    }
}
