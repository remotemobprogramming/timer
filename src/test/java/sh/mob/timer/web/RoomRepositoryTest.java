package sh.mob.timer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RoomRepositoryTest {

  private static final Duration EMPTY_ROOM_GRACE = Duration.ofMinutes(10);
  private static final Duration ROOM_RETENTION = Duration.ofHours(72);
  private static final Duration TIMER_REQUEST_RETENTION = Duration.ofHours(24);

  private RoomRepository repository(int maxRooms) {
    return new RoomRepository(
        new RoomNameGenerator(),
        maxRooms,
        EMPTY_ROOM_GRACE,
        ROOM_RETENTION,
        500,
        TIMER_REQUEST_RETENTION);
  }

  @Test
  void evictsEmptyRoomAfterGracePeriod() {
    var repository = repository(10);
    repository.get("empty-room");

    repository.cleanUpUnusedRooms(Instant.now().plus(EMPTY_ROOM_GRACE).plusSeconds(1));

    assertThat(repository.count()).isZero();
  }

  @Test
  void keepsEmptyRoomWithinGracePeriod() {
    var repository = repository(10);
    repository.get("empty-room");

    repository.cleanUpUnusedRooms(Instant.now());

    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void keepsRoomWithActiveTimerRequest() {
    var repository = repository(10);
    repository.get("active-room").addTimer(10L, "alice", Instant.now());

    repository.cleanUpUnusedRooms(Instant.now().plus(EMPTY_ROOM_GRACE).plusSeconds(1));

    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void keepsRoomWithSubscriber() {
    var repository = repository(10);
    var room = repository.get("subscribed-room");
    var subscription = room.timerRequestSink().asFlux().subscribe();

    repository.cleanUpUnusedRooms(Instant.now().plus(EMPTY_ROOM_GRACE).plusSeconds(1));

    assertThat(repository.count()).isEqualTo(1);
    subscription.dispose();
  }

  @Test
  void keepsRoomWithGoalUntilRetentionExpires() {
    var repository = repository(10);
    repository.get("goal-room").setGoal("a goal", "alice", Instant.now());

    repository.cleanUpUnusedRooms(Instant.now().plus(EMPTY_ROOM_GRACE).plusSeconds(1));
    assertThat(repository.count()).isEqualTo(1);

    repository.cleanUpUnusedRooms(Instant.now().plus(ROOM_RETENTION).plusSeconds(1));
    assertThat(repository.count()).isZero();
  }

  @Test
  void evictsRoomWhoseTimerRequestsExpired() {
    var repository = repository(10);
    repository
        .get("stale-room")
        .addTimer(10L, "alice", Instant.now().minus(Duration.ofHours(25)));

    repository.cleanUpUnusedRooms(Instant.now().plus(EMPTY_ROOM_GRACE).plusSeconds(1));

    assertThat(repository.count()).isZero();
  }

  @Test
  void rejectsNewRoomWhenLimitReached() {
    var repository = repository(2);
    repository.get("room-1");
    repository.get("room-2");

    assertThatExceptionOfType(TooManyRoomsException.class)
        .isThrownBy(() -> repository.get("room-3"));
    assertThat(repository.count()).isEqualTo(2);
  }

  @Test
  void existingRoomsStayReachableWhenLimitReached() {
    var repository = repository(2);
    var room = repository.get("room-1");
    repository.get("room-2");

    assertThat(repository.get("room-1")).isSameAs(room);
  }
}
