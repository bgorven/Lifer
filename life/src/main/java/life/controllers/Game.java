package life.controllers;

import javax.annotation.PostConstruct;

import life.models.Board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableScheduling
public class Game {
	@Autowired
	JdbcTemplate _db;
    
	@RequestMapping
	Board getBoard(int left, int top, int width, int height, int generation){
		return new Board(left, top, width, height, generation);
	}
	
	@PostConstruct
	void setup() {
		_db.execute("CREATE TABLE IF NOT EXISTS cells "
				+ " (id INT IDENTITY, x INT NOT NULL, y INT NOT NULL, " 
				+ " generation INT NOT NULL, UNIQUE (x, y, generation)); "
				+ " CREATE INDEX ON cells (generation); "
				+ " CREATE TABLE IF NOT EXISTS generations "
				+ " (gen INT IDENTITY); "
				+ " CREATE TABLE IF NOT EXISTS offsets "
				+ " (x INT, y INT, PRIMARY KEY (x,y)); "
				+ " INSERT INTO offsets (x,y) VALUES "
				+ " (-1, -1), (-1, 0), (-1, 1),"
				+ " (0, -1),          (0, 1),"
				+ " (1, -1), (1, 0), (1, 1); ");	
	}
	
	@Scheduled(fixedDelay=5000)
	void generation() {
		_db.execute("SELECT next.x,alive.x,next.y,alive.y,neighbours FROM "
				+ " (SELECT * FROM "
				+	" (SELECT c.x+o.x AS x, c.y+o.y AS y, count(*) AS neighbours "
				+ 	" FROM current_gen AS c JOIN offsets AS o GROUP BY x,y) "
				+	" WHERE neighbours IN (2,3)) AS next "
				+ " JOIN current_gen AS alive "
				+ " ON next.x = alive.x AND next.y = alive.y OR neighbours = 3 "
				+ " GROUP BY next.x,next.y;");
	}
}
