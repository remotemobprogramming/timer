package sh.mob.timer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
public class IndexController {

  private final RoomRepository roomRepository;

  public IndexController(RoomRepository roomRepository) {
    this.roomRepository = roomRepository;
  }

  @GetMapping
  public String index(Model model) {
    System.out.println("INDEX");
    model.addAttribute("numberOfRooms", roomRepository.count());
    model.addAttribute("numberOfUsers", roomRepository.countUsers());
    return "index";
  }

  @PostMapping
  public String post(@RequestParam("room") String room) {
    System.out.println("POST");
    return "redirect:/" + room;
  }
}
