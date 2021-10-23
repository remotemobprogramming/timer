package sh.mob.timer.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping({"/", ""})
public class IndexController {

  private final RoomRepository roomRepository;

  public IndexController(RoomRepository roomRepository) {
    this.roomRepository = roomRepository;
  }

  @GetMapping
  public String index(Model model) {
    model.addAttribute("numberOfRooms", roomRepository.count());
    model.addAttribute("numberOfUsers", roomRepository.countUsers());
    model.addAttribute("numberOfConnections", 0);
    return "index";
  }

  @PostMapping
  public String post(@ModelAttribute Form form) {
    return "redirect:/%s".formatted(form.room());
  }

  public record Form (String room) {}
}
