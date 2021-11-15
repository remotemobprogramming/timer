package sh.mob.timer.web;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.test.StepVerifier;
import sh.mob.timer.web.RoomApiController.PutTimerRequest;

@SpringBootTest
@AutoConfigureWebTestClient
class ApiTests {

  @Autowired private WebTestClient webTestClient;

  @MockBean private Clock clock;

  @Test
  void getIndex() {
    webTestClient.get().uri("/").exchange().expectStatus().isOk();
  }

  @Test
  void postRoom() {
    webTestClient
        .post()
        .uri("/")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData("room", "big-boar-37"))
        .exchange()
        .expectStatus()
        .is3xxRedirection();
  }

  @Test
  void getRoom() {
    webTestClient.get().uri("/big-boar-37").exchange().expectStatus().isOk();
  }

  @Test
  void putTimer() {
    Mockito.when(clock.instant()).thenReturn(Instant.parse("2020-01-24T06:00:00Z"));

    webTestClient
        .put()
        .uri("/big-boar-37")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new PutTimerRequest(10L, null, "alice"))
        .exchange()
        .expectStatus()
        .isAccepted();

    var result =
        webTestClient
            .get()
            .uri("/big-boar-37/events")
            .accept(TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(TEXT_EVENT_STREAM)
            .returnResult(Object.class);

    Map<String, Object> t = new java.util.HashMap<>();
    t.put("timer", 10);
    t.put("requested", "2020-01-24T06:00:00Z");
    t.put("user", "alice");
    t.put("nextUser", null);
    t.put("type", "TIMER");
    StepVerifier.create(result.getResponseBody())
        .expectNext(List.of(), t)
        .thenCancel()
        .verify();
  }
}
