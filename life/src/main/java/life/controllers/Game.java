package life.controllers;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.PostConstruct;

import life.models.Board;
import life.models.Cell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableScheduling
public class Game {
	@Autowired
	JdbcTemplate _db;
    
	@RequestMapping(name="/board", method={RequestMethod.GET})
	Board getBoard(int left, int top, int width, int height, int generation){
		return new Board(left, top, width, height, generation);
	}
	
	@RequestMapping(name="/board", method={RequestMethod.POST})
	boolean setCells(@ModelAttribute Cell[] cells, int generation) {
		return doSet(cells, generation);
	}
	
	@Transactional
	boolean doSet(Cell[] cells, int generation) {
		try {
			if (generation != currentGen()) return false;
			
			_db.batchUpdate("INSERT INTO cells (x,y,gen) VALUES (?,?,?)", new BatchPreparedStatementSetter() {
				
				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					ps.setInt(1, cells[i].getX());
					ps.setInt(2, cells[i].getY());
					ps.setInt(3, generation);
				}
				
				@Override
				public int getBatchSize() {
					return cells.length;
				}
			});
		} catch (DataAccessException e) {
			return false;
		}
		return true;
	}
	
	int currentGen() {
		return _db.queryForObject("SELECT COALESCE(MIN(val),0) FROM generation", Integer.class);
	}
	
	@Transactional
	@PostConstruct
	void setup() {
		try {
			_db.execute(
					  " CREATE TABLE IF NOT EXISTS generation ( val INT );"
	
					+ " CREATE TABLE IF NOT EXISTS cells "
					+ " ("
					+ " 	x   INT NOT NULL, "
					+ "		y   INT NOT NULL, " 
					+ " 	gen INT NOT NULL,"
					+ "		UNIQUE (x, y, gen) "
					+ " ); "
					);
			
			_db.execute(
					  " DROP TABLE IF EXISTS born; "
					+ " DROP TABLE IF EXISTS survive; "
					+ " CREATE TABLE born ( count INT PRIMARY KEY ); "
					+ " CREATE TABLE survive ( count INT PRIMARY KEY ); "
					+ " INSERT INTO born VALUES (3); "
					+ " INSERT INTO survive VALUES (2), (3);"
	
					+ " CREATE INDEX ON cells (gen); "
					+ " CREATE INDEX ON cells (x,y);"
					);
			
			_db.execute(
					//TODO perf test: view vs separate table for current gen
					  " CREATE VIEW IF NOT EXISTS alive AS"
					+ "  SELECT x,y "
					+ "	  FROM cells "
					+ "	  WHERE gen = "
					+ "        ( "
					+ "	         SELECT COALESCE(MIN(val),0) FROM generation"
					+ "        );"
					);
			
			_db.execute(
					  " CREATE TABLE IF NOT EXISTS offsets "
					+ " ("
					+ "	    x INT, "
					+ "	    y INT, "
					+ " ); "
	
					+ " INSERT INTO offsets (x,y) "
					+ "	VALUES "
					+ "    (-1, -1), (-1, 0), (-1, 1),"
					+ "     (0, -1),          (0, 1),"
					+ "     (1, -1), (1, 0), (1, 1);"
					);
			
			_db.execute(
					//TODO perf test: could generating a temp table in the update method
					//                be faster than using a view? 
					//                 - could index the table on (x,y)
					//                 - could count cells as a series of `SET n = n+1`
					//                   queries
					  " CREATE VIEW IF NOT EXISTS next AS "
					+ " SELECT "
					+ "        x, "
					+ "        y, "
					+ "        n IN (SELECT count FROM born) AS can_create,"
					+ "        n IN (SELECT count FROM survive) AS can_survive "
					+ "   FROM "
					+ "        ( "
					+ "          SELECT "
					+ "                 alive.x+offsets.x AS x, "
					+ "                 alive.y+offsets.y AS y, "
					+ "                 count(*) AS n "
					+ "            FROM alive "
					+ "            JOIN offsets "
					+ "        GROUP BY x,y "
					+ "        )"
					//TODO perf test: does filtering this view speed up queries involving it?
					+ "  WHERE "
					+ "        n IN (SELECT count FROM born) "
					+ "        OR "
					+ "        n IN (SELECT count FROM survive);"
					);
		} catch (DataAccessException e) {
			throw e;
		}
	}
	
	@Transactional
	@Scheduled(fixedDelay=5000)
	void update() {
		try {
			int gen = currentGen();
			//TODO perf test: there could be different strategies for this, but in the 
			//                end I don't see any way to do it without iterating the `alive`
			//                table once looking for survivors then iterating the `next`
			//                table once looking for new cells.
			_db.execute("INSERT INTO cells (x,y,gen) "
					+ "       SORTED "
					+ "       SELECT "
					+ "              next.x, "
					+ "              next.y, "
					+                gen //TODO this correctly
					+ "	        FROM "
					+ "              next "
					+ "    LEFT JOIN "
					+ "              alive "
					+ "           ON "
					+ "              next.x = alive.x "
					+ "              AND "
					+ "              next.y = alive.y"
					+ "        WHERE "
					+ "              alive.x IS NOT NULL AND next.can_survive "
					+ "              OR "
					+ "              alive.x IS NULL AND next.can_create;"
					);
		} catch (DataAccessException e) {
			throw e;
		}
	}
}
