package life.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class GameController {

	@RequestMapping("/game") 
	String game() {
		return "game";
	}
}
