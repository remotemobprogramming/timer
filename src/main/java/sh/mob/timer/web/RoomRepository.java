package sh.mob.timer.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {

  private static final Logger log = LogManager.getLogger();

  private final Map<String, Room> repository = new ConcurrentHashMap<>();

  Room get(String room) {
    return repository.computeIfAbsent(room, name -> {
      log.info("Created room {}", name);
      return new Room(name);
    });
  }

  public long count() {
    return repository.size();
  }

  public long countUsers() {
    return repository.values().stream().mapToLong(room -> room.team().size()).sum();
  }

}
