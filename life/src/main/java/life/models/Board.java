package life.models;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class Board {
	
	private List<Cell> _cells;
	
	public Board(List<Cell> cells) {
		_cells = cells;
	}
	
    @Autowired
    private JdbcTemplate _db;
    private static final String _query = "SELECT x, y FROM cells WHERE " +
    				" (x >= ?) and (x < ? + ?) and " + 
    				" (y >= ?) and (y < ? + ?) and " + 
    				" gen = ?";
    
    public Board(int x, int y, int width, int height, int generation) {
    	_cells = _db.query(_query, new Object[]{x, x, width, y , y, height, generation }, Cell.rowMapper);
    }
	
	public List<Cell> getCells() { return _cells; }
	public void setCells(List<Cell> cells) { _cells = cells; }
	
}
