package sh.mob.timer.web;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.test.StepVerifier;
import sh.mob.timer.web.Room.TimerRequest;
import sh.mob.timer.web.Room.TimerRequest.TimerType;
import sh.mob.timer.web.RoomApiController.PutTimerRequest;

@SpringBootTest
@AutoConfigureWebTestClient
class TimerApplicationTests {

  @Autowired private WebTestClient webTestClient;

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
    webTestClient
        .get()
        .uri("/big-boar-37")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void putTimer() {
    webTestClient
        .put()
        .uri("/big-boar-37")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new PutTimerRequest(10L, null, "alice"))
        .exchange()
        .expectStatus()
        .isAccepted();

    var result = webTestClient
        .get()
        .uri("/big-boar-37/events")
        .accept(TEXT_EVENT_STREAM)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
        .returnResult(TimerRequest.class);

    StepVerifier.create(result.getResponseBody())
        .consumeNextWith(timerRequest -> {
          assertThat(timerRequest.getTimer(), equalTo(10L));
          assertThat(timerRequest.getUser(), equalTo("alice"));
          assertThat(timerRequest.getRequested(), notNullValue());
          assertThat(timerRequest.getType(), equalTo(TimerType.TIMER));
        })
        .thenCancel()
        .verify();
  }

}
