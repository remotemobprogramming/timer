package sh.mob.timer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RoomController {

  private final RoomRepository roomRepository;

  public RoomController(RoomRepository roomRepository) {
    this.roomRepository = roomRepository;
  }

  @GetMapping
  @RequestMapping("/{roomId}")
  public String get(@PathVariable String roomId, Model model) {
    model.addAttribute("room", roomRepository.get(roomId));
    return "room";
  }
}
