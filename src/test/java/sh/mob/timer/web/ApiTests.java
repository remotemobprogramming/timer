package sh.mob.timer.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.test.StepVerifier;
import sh.mob.timer.web.RoomApiController.PutTimerRequest;

@SpringBootTest
@AutoConfigureWebTestClient
class ApiTests {

  @Autowired private WebTestClient webTestClient;

  @MockBean private Clock clock;
  @Autowired private RoomRepository roomRepository;

  @BeforeEach
  void setUp() {
    roomRepository.deleteAll();
  }

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
    var now = "2020-01-24T06:00:00Z";
    Mockito.when(clock.instant()).thenReturn(Instant.parse(now));

    var user = "alice";
    webTestClient
        .put()
        .uri("/big-boar-37")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new PutTimerRequest(10L, null, user))
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

    StepVerifier.create(result.getResponseBody())
        .expectNext(List.of(), toTimerRequestMap(10, user, null, "TIMER", now))
        .thenCancel()
        .verify();
  }

  @Test
  void putGoal() throws Exception {
    var now = "2020-01-24T07:00:00Z";
    Mockito.when(clock.instant()).thenReturn(Instant.parse(now));

    var user = "alice";
    var goal = "This is an awesome goal!";
    webTestClient
        .put()
        .uri("/big-boar-37/goal")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RoomApiController.PutGoalRequest(goal, user))
        .exchange()
        .expectStatus()
        .isAccepted();

    // Assert API
    webTestClient.get().uri("/big-boar-37/goal")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.goal").isEqualTo(goal);

    // Assert ServerSentEvents
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

    StepVerifier.create(result.getResponseBody())
        .expectNext(
            List.of(),
            toTimerRequestMap(0, null, null, null, null),
            toGoalRequestMap(goal, now, user))
        .thenCancel()
        .verify();
  }

  @Test
  void deleteGoal() {
    var now = "2020-01-24T08:00:00Z";
    Mockito.when(clock.instant()).thenReturn(Instant.parse(now));

    var user = "alice";
      webTestClient
        .put()
        .uri("/big-boar-37/goal")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RoomApiController.PutGoalRequest("This is an awesome goal!", user))
        .exchange()
        .expectStatus()
        .isAccepted();

    webTestClient
            .method(HttpMethod.DELETE)
            .uri("/big-boar-37/goal")
            .contentType(APPLICATION_JSON)
            .bodyValue(new RoomApiController.DeleteGoalRequest(user))
            .exchange()
            .expectStatus()
            .isAccepted();

    // Assert API
    webTestClient.get().uri("/big-boar-37/goal")
            .exchange()
            .expectStatus().isNoContent();

    // Assert ServerSentEvents
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

    StepVerifier.create(result.getResponseBody())
        .expectNext(
            List.of(),
            toTimerRequestMap(0, null, null, null, null),
            toGoalRequestMap(null, now, user))
        .thenCancel()
        .verify();
  }

  private static Map<String, Object> toTimerRequestMap(
      int timer, String user, String nextUser, String type, String requested) {
    Map<String, Object> map = new java.util.HashMap<>();
    map.put("timer", timer);
    map.put("user", user);
    map.put("nextUser", nextUser);
    map.put("type", type);
    map.put("requested", requested);
    return map;
  }

  private static Map<String, Object> toGoalRequestMap(String goal, String now, String user) {
    Map<String, Object> map = new java.util.HashMap<>();
    map.put("goal", goal);
    map.put("requested", now);
    map.put("user", user);
    return map;
  }
}
