package life.models;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;

public class Cell {
	public static final RowMapper<Cell> rowMapper = new BeanPropertyRowMapper<Cell>(Cell.class);
	
	private int _x;
	private int _y;
	
	public Cell(int x, int y) {
		_x = x;
		_y = y;
	}
	
	public int getX() { return _x; }
	public void setX(int x) { _x = x; }
	
	public int getY() { return _y; }
	public void setY(int y) { _y = y; }
}
