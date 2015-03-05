package life.controllers;

import java.util.List;

import life.models.Game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CellController {
	
	@Autowired Game game;
	
	@RequestMapping(name="/board", method={RequestMethod.GET})
	public List<life.models.CellModel> getCells(int x, int y, int width, int height, int generation){
		return game.getCells(x, y, width, height, generation);
	}
	
	@RequestMapping(name="/board", method={RequestMethod.POST})
	public boolean setCells(@ModelAttribute List<life.models.CellModel> cells, int generation) {
		return game.setCells(cells, generation);
	}
}
