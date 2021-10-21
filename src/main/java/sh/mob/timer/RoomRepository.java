package sh.mob.timer;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {

  private final Map<String, Room> repository = new ConcurrentHashMap<>();

  Room get(String room) {
    return repository.computeIfAbsent(room, Room::new);
  }

  static final class Room {

    private final String name;
    private final List<TimerRequest> timerRequests = new CopyOnWriteArrayList<>();

    Room(String name) {
      this.name = name;
    }

    public void add(Long timer) {
      timerRequests.add(new TimerRequest(timer, Instant.now()));
    }

    public Duration timeLeft() {
      if (timerRequests.isEmpty()) {
        return Duration.ZERO;
      }

      var lastTimerRequest = timerRequests.get(timerRequests.size() - 1);
      var lastTimerRequestedTimestamp = lastTimerRequest.requested;
      return Duration.between(Instant.now(), lastTimerRequestedTimestamp.plus(lastTimerRequest.timer(), ChronoUnit.MINUTES));
    }

    public String name() {
      return name;
    }

    public List<TimerRequest> timerRequests() {
      return Collections.unmodifiableList(timerRequests);
    }
  }

  record TimerRequest(Long timer, Instant requested) {}
}
