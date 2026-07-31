package sh.mob.timer.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RoomTest {

  @Test
  void dropsOldestTimerRequestsWhenLimitExceeded() {
    var room = new Room("small-room", 3, java.time.Duration.ofHours(24));
    var now = Instant.now();

    for (int i = 1; i <= 5; i++) {
      room.addTimer((long) i, "user-" + i, now.plusSeconds(i));
    }

    var history = room.historyWithoutLatest();
    assertThat(history).hasSize(2);
    assertThat(history.get(0).getTimer()).isEqualTo(3L);
    assertThat(room.lastTimerRequest()).isPresent();
    assertThat(room.lastTimerRequest().get().getTimer()).isEqualTo(5L);
  }
}
