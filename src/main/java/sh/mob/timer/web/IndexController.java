package sh.mob.timer.web;

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

  private final RoomRepository roomRepository;
  private final String url;

  public IndexController(RoomRepository roomRepository, @Value("${url}") String url) {
    this.roomRepository = roomRepository;
    this.url = url;
  }

  @GetMapping
  public String index(Model model) {
    model.addAttribute("numberOfRooms", roomRepository.count());
    model.addAttribute("numberOfUsers", roomRepository.countUsers());
    model.addAttribute("numberOfConnections", 0);
    model.addAttribute("url", url);
    return "index";
  }

  @PostMapping
  public String post(@ModelAttribute Form form) {
    return String.format("redirect:/%s", form.room);
  }

  static class Form {
    public String room;
  }
}
