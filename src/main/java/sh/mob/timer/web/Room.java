package sh.mob.timer.web;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import reactor.core.publisher.Sinks;
import sh.mob.timer.web.Room.TimerRequest.TimerType;

final class Room {

  private final String name;
  private final List<TimerRequest> timerRequests = new CopyOnWriteArrayList<>();
  private final Sinks.Many<TimerRequest> sink = Sinks.many().replay().latest();

  Room(String name) {
    this.name = name;
  }

  public void add(Long timer, String user) {
    TimerRequest timerRequest = new TimerRequest(timer, Instant.now(), user, TimerType.TIMER);
    timerRequests.add(timerRequest);
    sink.tryEmitNext(timerRequest);
  }

  public void addBreaktimer(Long breaktimer, String user) {
    TimerRequest timerRequest = new TimerRequest(breaktimer, Instant.now(), user, TimerType.BREAKTIMER);
    timerRequests.add(timerRequest);
    sink.tryEmitNext(timerRequest);
  }

  public Sinks.Many<TimerRequest> sink() {
    return sink;
  }

  public TimeLeft timeLeft() {
    if (timerRequests.isEmpty()) {
      return new TimeLeft(Duration.ZERO, null, null, null);
    }

    var lastTimerRequest = timerRequests.get(timerRequests.size() - 1);
    var lastTimerRequestedTimestamp = lastTimerRequest.requested;
    var timer = lastTimerRequest.getTimer();
    var result =
        Duration.between(
            Instant.now(), lastTimerRequestedTimestamp.plus(timer, ChronoUnit.MINUTES));

    if (result.isNegative()) {
      return new TimeLeft(
          Duration.ZERO, timer, lastTimerRequestedTimestamp, lastTimerRequest.getUser());
    }

    return new TimeLeft(result, timer, lastTimerRequestedTimestamp, lastTimerRequest.getUser());
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
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      var that = (TimeLeft) obj;
      return Objects.equals(this.duration, that.duration)
          && Objects.equals(this.timer, that.timer)
          && Objects.equals(this.requested, that.requested)
          && Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(duration, timer, requested, name);
    }

    @Override
    public String toString() {
      return "TimeLeft["
          + "duration="
          + duration
          + ", "
          + "timer="
          + timer
          + ", "
          + "requested="
          + requested
          + ", "
          + "name="
          + name
          + ']';
    }
  }

  public List<String> team() {
    return timerRequests().stream()
        .map(TimerRequest::getUser)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  public String name() {
    return name;
  }

  public List<TimerRequest> timerRequests() {
    return Collections.unmodifiableList(timerRequests);
  }

  public static final class TimerRequest {

    enum TimerType {
      TIMER,
      BREAKTIMER
    }

    private final Long timer;
    private final Instant requested;
    private final String user;
    private final TimerType type;

    TimerRequest(Long timer, Instant requested, String user, TimerType type) {
      this.timer = timer;
      this.requested = requested;
      this.user = user;
      this.type = type;
    }

    public Long getTimer() {
      return timer;
    }

    public Instant getRequested() {
      return requested;
    }

    public String getUser() {
      return user;
    }

    public TimerType getType() {
      return type;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      var that = (TimerRequest) obj;
      return Objects.equals(this.timer, that.timer)
          && Objects.equals(this.requested, that.requested)
          && Objects.equals(this.user, that.user)
          && this.type == that.type;
    }

    @Override
    public int hashCode() {
      return Objects.hash(timer, requested, user, type);
    }

    @Override
    public String toString() {
      return "TimerRequest["
          + "timer="
          + timer
          + ", "
          + "requested="
          + requested
          + ", "
          + "user="
          + user
          + ", "
          + "type="
          + type
          + ']';
    }
  }
}
