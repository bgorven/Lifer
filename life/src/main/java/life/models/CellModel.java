package life.models;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;


public class CellModel {
	public static final RowMapper<CellModel> rowMapper = BeanPropertyRowMapper.newInstance(CellModel.class);
	
	private int x;
	private int y;
	
	public CellModel() { }
	
	public CellModel(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int getX() { return x; }
	public void setX(int x) { this.x = x; }
	
	public int getY() { return y; }
	public void setY(int y) { this.y = y; }
	
	public boolean equals(CellModel other) {
		return other != null && other.x == x && other.y == y;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof CellModel) return equals((CellModel)other);
		return false;
	}
	
	@Override
	public int hashCode() {
		return x^y;
	}
}
