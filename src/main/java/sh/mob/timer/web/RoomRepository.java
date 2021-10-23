package sh.mob.timer.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {

  private final Map<String, Room> repository = new ConcurrentHashMap<>();

  Room get(String room) {
    return repository.computeIfAbsent(room, Room::new);
  }

  public long count() {
    return repository.size();
  }

  public long countUsers() {
    return repository.values().stream().mapToLong(room -> room.team().size()).sum();
  }

}
