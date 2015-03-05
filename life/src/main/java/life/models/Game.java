package life.models;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
public class Game {
	
	@Autowired
	JdbcTemplate _db;
    
	public List<CellModel> getCells(int x, int y, int width, int height, int generation){
		final String query = "SELECT x, y FROM cells WHERE " +
			" (x >= ?) and (x < ? + ?) and " + 
			" (y >= ?) and (y < ? + ?) and " + 
			" gen = ?";
		
		return _db.query(query, new Object[]{x, x, width, y , y, height, generation }, CellModel.rowMapper);
	}
	
	@Transactional
	public boolean setCells(List<CellModel> cells, int generation) {
		try {
			if (generation != currentGen()) return false;
			
			_db.batchUpdate("INSERT INTO cells (x,y,gen) VALUES (?,?,?)", new BatchPreparedStatementSetter() {
				
				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					ps.setInt(1, cells.get(i).getX());
					ps.setInt(2, cells.get(i).getY());
					ps.setInt(3, generation);
				}
				
				@Override
				public int getBatchSize() {
					return cells.size();
				}
			});
		} catch (DataAccessException e) {
			//TODO could be a bit more verbose about results
			return false;
		}
		return true;
	}
	
	public int currentGen() {
		return _db.queryForObject("SELECT COALESCE(MAX(gen),0) FROM cells", Integer.class);
	}
	
	@Transactional
	@PostConstruct
	public void setup() {
		try {
			_db.execute(
					  " CREATE TABLE IF NOT EXISTS cells "
					+ " ("
					+     " x   INT NOT NULL, "
					+     " y   INT NOT NULL, " 
					+     " gen INT NOT NULL,"
					+     " PRIMARY KEY (x, y, gen) "
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
					+  " SELECT x,y "
					+    " FROM cells "
					+   " WHERE gen = (SELECT MAX(gen) FROM cells"
					+         " )"
					);
			
			_db.execute(
					  " CREATE TABLE IF NOT EXISTS offsets "
					+ " ("
					+     " x INT, "
					+     " y INT, "
					+ " ) "
					);
			
			_db.execute(
					  " INSERT INTO offsets (x,y) "
					+ "	VALUES "
					+    " (-1, -1), (-1, 0), (-1, 1),"
					+     " (0, -1),          (0, 1),"
					+     " (1, -1), (1, 0), (1, 1);"
					);
			
			_db.execute(
					//TODO perf test: could generating a temp table in the update method
					//                be faster than using a view? 
					//                 - could index the table on (x,y)
					//                 - could count cells as a series of `SET n = n+1`
					//                   queries
					  " CREATE VIEW IF NOT EXISTS next AS "
					+ " SELECT "
					+        " x, "
					+        " y, "
					+        " n IN (SELECT count FROM born) AS can_create,"
					+        " n IN (SELECT count FROM survive) AS can_survive "
					+   " FROM "
					+        " ( "
					+          " SELECT "
					+                 " alive.x+offsets.x AS x, "
					+                 " alive.y+offsets.y AS y, "
					+                 " count(*) AS n "
					+            " FROM alive "
					+            " JOIN offsets "
					+        " GROUP BY x,y "
					+        " )"
					//TODO perf test: does filtering this view speed up queries involving it?
					+  " WHERE "
					+        " n IN (SELECT count FROM born) "
					+        " OR "
					+        " n IN (SELECT count FROM survive);"
					);
		} catch (DataAccessException e) {
			throw e;
		}
	}
	
	@Transactional
//	@Scheduled(fixedDelay=5000)
	public void update() {
		try {
			int gen = currentGen();
			//TODO perf test: there could be different strategies for this, but in the 
			//                end I don't see any way to do it without iterating the `alive`
			//                table once looking for survivors then iterating the `next`
			//                table once looking for new cells.
			_db.update("INSERT INTO cells (x,y,gen) "
					+       " SORTED "
					+       " SELECT "
					+              " next.x, "
					+              " next.y, "
					+              " ?"
					+         " FROM "
					+              " next "
					+    " LEFT JOIN "
					+              " alive "
					+           " ON "
					+              " next.x = alive.x "
					+              " AND "
					+              " next.y = alive.y"
					+        " WHERE "
					+              " alive.x IS NOT NULL AND next.can_survive "
					+              " OR "
					+              " alive.x IS NULL AND next.can_create;",
					gen + 1
					);
		} catch (DataAccessException e) {
			throw e;
		}
	}
}
