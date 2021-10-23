package sh.mob.timer.web;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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

  public static final class TimeLeft {

    private final Duration duration;
    private final Long timer;
    private final Instant requested;
    private final String name;

    public TimeLeft(Duration duration, Long timer, Instant requested, String name) {
      this.duration = duration;
      this.timer = timer;
      this.requested = requested;
      this.name = name;
    }

    public Duration duration() {
      return duration;
    }

    public Long timer() {
      return timer;
    }

    public Instant requested() {
      return requested;
    }

    public String name() {
      return name;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this)
        return true;
      if (obj == null || obj.getClass() != this.getClass())
        return false;
      var that = (TimeLeft) obj;
      return Objects.equals(this.duration, that.duration) &&
          Objects.equals(this.timer, that.timer) &&
          Objects.equals(this.requested, that.requested) &&
          Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(duration, timer, requested, name);
    }

    @Override
    public String toString() {
      return "TimeLeft[" +
          "duration=" + duration + ", " +
          "timer=" + timer + ", " +
          "requested=" + requested + ", " +
          "name=" + name + ']';
    }
  }

  public List<String> team() {
    return timerRequests().stream().map(TimerRequest::user).distinct().sorted().collect(Collectors.toList());
  }

  public String name() {
    return name;
  }

  public List<TimerRequest> timerRequests() {
    return Collections.unmodifiableList(timerRequests);
  }

  static final class TimerRequest {

    private final Long timer;
    private final Instant requested;
    private final String user;

    TimerRequest(Long timer, Instant requested, String user) {
      this.timer = timer;
      this.requested = requested;
      this.user = user;
    }

    public Long timer() {
      return timer;
    }

    public Instant requested() {
      return requested;
    }

    public String user() {
      return user;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this)
        return true;
      if (obj == null || obj.getClass() != this.getClass())
        return false;
      var that = (TimerRequest) obj;
      return Objects.equals(this.timer, that.timer) &&
          Objects.equals(this.requested, that.requested) &&
          Objects.equals(this.user, that.user);
    }

    @Override
    public int hashCode() {
      return Objects.hash(timer, requested, user);
    }

    @Override
    public String toString() {
      return "TimerRequest[" +
          "timer=" + timer + ", " +
          "requested=" + requested + ", " +
          "user=" + user + ']';
    }
  }
}
