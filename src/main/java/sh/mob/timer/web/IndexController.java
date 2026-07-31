package sh.mob.timer.web;

import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/", ""})
public class IndexController {

  private static final Pattern VALID_ROOM = Pattern.compile(Room.NAME_REGEX);

  private final RoomRepository roomRepository;
  private final String url;

  public IndexController(RoomRepository roomRepository, @Value("${url}") String url) {
    this.roomRepository = roomRepository;
    this.url = url;
  }

  @GetMapping
  public String index(Model model) {
    model.addAttribute("numberOfConnections", roomRepository.countConnections());
    model.addAttribute("url", url);
    model.addAttribute("randomRoomName", roomRepository.newRandomRoomName());
    return "index";
  }

  @PostMapping
  public String post(@ModelAttribute Form form) {
    if (form.room == null || !VALID_ROOM.matcher(form.room).matches()) {
      return "redirect:/";
    }
    return String.format("redirect:/%s", form.room);
  }

  static class Form {
    public String room;

    public String getRoom() {
      return room;
    }

    public void setRoom(String room) {
      this.room = room;
    }
  }
}
