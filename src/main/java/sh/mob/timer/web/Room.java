package sh.mob.timer.web;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class Room {

  private final String name;
  private final List<TimerRequest> timerRequests = new CopyOnWriteArrayList<>();

  Room(String name) {
    this.name = name;
  }

  public void add(Long timer, String user) {
    timerRequests.add(new TimerRequest(timer, Instant.now(), user));
  }

  public TimeLeft timeLeft() {
    if (timerRequests.isEmpty()) {
      return new TimeLeft(Duration.ZERO, null, null, null);
    }

    var lastTimerRequest = timerRequests.get(timerRequests.size() - 1);
    var lastTimerRequestedTimestamp = lastTimerRequest.requested;
    var timer = lastTimerRequest.timer();
    var result =
        Duration.between(
            Instant.now(), lastTimerRequestedTimestamp.plus(timer, ChronoUnit.MINUTES));

    if (result.isNegative()) {
      return new TimeLeft(
          Duration.ZERO, timer, lastTimerRequestedTimestamp, lastTimerRequest.user());
    }

    return new TimeLeft(result, timer, lastTimerRequestedTimestamp, lastTimerRequest.user());
  }

  public record TimeLeft(Duration duration, Long timer, Instant requested, String name) {}

  public List<String> team() {
    return timerRequests().stream().map(TimerRequest::user).distinct().sorted().toList();
  }

  public String name() {
    return name;
  }

  public List<TimerRequest> timerRequests() {
    return Collections.unmodifiableList(timerRequests);
  }

  record TimerRequest(Long timer, Instant requested, String user) {}
}
