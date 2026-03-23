package org.example.notesapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NoteControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testGetAllNotes() {
        ResponseEntity<String> response = restTemplate
                .getForEntity("http://localhost:" + port + "/api/notes", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testCreateNote() {
        String note = "{\"title\":\"Test\",\"content\":\"Integration test note\"}";
        ResponseEntity<String> response = restTemplate
                .postForEntity("http://localhost:" + port + "/api/notes", note, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}