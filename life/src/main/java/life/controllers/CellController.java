package life.controllers;

import java.util.List;

import life.models.BoardModel;
import life.models.Game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/board")
public class CellController {
	
	@Autowired Game game;
	
	@RequestMapping(method={RequestMethod.GET})
	public BoardModel getCells(int x, int y, int width, int height, int generation){
		generation = generation < 0 ? 0 : game.currentGen();
		BoardModel board = new BoardModel(
				x, y, width, height, generation, 
				game.getCells(x, y, width, height, generation));
		
		return board;
	}
	
	@RequestMapping(method={RequestMethod.POST})
	public boolean setCells(@ModelAttribute List<life.models.CellModel> cells, int generation) {
		return game.setCells(cells, generation);
	}
}
