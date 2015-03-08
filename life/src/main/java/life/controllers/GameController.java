package life.controllers;

import java.util.Arrays;

import life.models.BoardModel;
import life.models.CellModel;
import life.models.Game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GameController {
	
	@Autowired
	Game game;

	@RequestMapping("/game")
	String game(
			@RequestParam(value="x", defaultValue="0") int x, 
			@RequestParam(value="y", defaultValue="0") int y, 
			@RequestParam(value="width", defaultValue="30") int width, 
			@RequestParam(value="height", defaultValue="20") int height, 
			@RequestParam(value="generation", defaultValue="-1") int generation, 
			Model model) {
		
		if (generation < 0) generation = game.currentGen();
		
		model.addAttribute("board", new BoardModel(x, y, width, height, generation, game.getCells(x, y, width, height, generation)));
		return "game";
	}
	
	@RequestMapping(value="/game/add", method={ RequestMethod.POST })
	String add(CellModel cell, int generation) {
		game.setCells(Arrays.asList(new CellModel[]{ cell }), generation);
		
		return "redirect:/game";
	}
	
	@RequestMapping(value="/game/next", method={ RequestMethod.POST })
	String next() {
		game.update();
		
		return "redirect:/game";
	}
}
