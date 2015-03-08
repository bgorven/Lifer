package life.models;

import java.util.ArrayList;
import java.util.List;

public class BoardModel {
	private int _x, _y, _width, _height, _generation;
	private List<CellModel> _cells;
	
	public BoardModel() {
		_cells = new ArrayList<CellModel>();
	}

	public BoardModel(int x, int y, int width, int height, int generation, List<CellModel> cells) {
		_x = x;
		_y = y;
		_width = width;
		_height = height;
		_generation = generation;
		_cells = cells;
	}
	
	public int getX() { return _x; }
	public void setX(int x) { _x = x; }
	public int getY() { return _y; }
	public void setY(int y) { _y = y; }
	public int getWidth() { return _width; }
	public void setWidth(int width) { _width = width; }
	public int getHeight() { return _height; }
	public void setHeight(int height) { _height = height; }
	public int getGeneration() { return _generation; }
	public void setGeneration(int generation) { _generation = generation; }
	public List<CellModel> getCells() { return _cells; }
	public void setCells(List<CellModel> cells) { _cells = cells; }
}
