package sh.mob.timer.web;

import java.time.Instant;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class Stats {

  private final Instant statisticsSince = Instant.now();
  private ConcurrentHashMap<Long, Long> timerCounts = new ConcurrentHashMap<Long, Long>();
  private ConcurrentHashMap<Long, Long> breaktimerCounts = new ConcurrentHashMap<Long, Long>();

  public void incrementTimer(long timer) {
    Long count = timerCounts.get(timer);
    if (count == null) {
      count = 0L;
    }
    count++;
    timerCounts.put(timer, count);
  }

  public void incrementBreaktimer(long breakTimer) {
    Long count = breaktimerCounts.get(breakTimer);
    if (count == null) {
      count = 0L;
    }
    count++;
    breaktimerCounts.put(breakTimer, count);
  }

  public SortedMap<Long, Long> getTimer() {
    return new TreeMap<>(timerCounts);
  }

  public SortedMap<Long, Long> getBreaktimer() {
    return new TreeMap<>(breaktimerCounts);
  }

  public Instant getStatisticsSince() {
    return statisticsSince;
  }
}
