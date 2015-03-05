package life;

import java.util.Arrays;
import java.util.List;

import life.models.CellModel;
import life.models.Game;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = GameOfLifeApplication.class)
@WebAppConfiguration
public class GameOfLifeApplicationTests {
	
	@Autowired
	JdbcTemplate db;
	
	@Autowired
	Game game;
	
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Before
	public void clearDB() {
		db.execute("DROP VIEW IF EXISTS next");
		db.execute("DROP TABLE IF EXISTS offsets");
		db.execute("DROP VIEW IF EXISTS alive");
		db.execute("DROP TABLE IF EXISTS survive");
		db.execute("DROP TABLE IF EXISTS born");
		db.execute("DROP TABLE IF EXISTS generation");
		db.execute("DROP TABLE IF EXISTS cells");
		
		game.setup();
	}

	@Test
	public void setup() {
		int result = db.queryForObject("SELECT COUNT(*) FROM cells", Integer.class);
		Assert.assertEquals(0, result);
		
		db.update("INSERT INTO cells(x,y,gen) VALUES (1,2,0)");
		result = db.queryForObject("SELECT COUNT(*) FROM alive", Integer.class);
		Assert.assertEquals(1, result);
		
		db.update("INSERT INTO cells(x,y,gen) VALUES (2,1,0)");
		boolean canSurvive = db.queryForObject(
				" SELECT can_survive "
				+ " FROM next "
				+ "WHERE x = 2 AND y = 2", Boolean.class);
		Assert.assertTrue(canSurvive);
	}
	
	@Test
	public void blinker() {
		List<CellModel> a = Arrays.asList(new CellModel[]{ new CellModel(0, -1), new CellModel(0,0), new CellModel(0,1) });
		List<CellModel> b = Arrays.asList(new CellModel[]{ new CellModel(-1, 0), new CellModel(0,0), new CellModel(1,0) });
		game.setCells(a, 0);
		
		List<CellModel> currentExpected = a;
		
		assertEqual(currentExpected, game.getCells(-1, -1, 3, 3, 0));
		
		for (int i = 1; i < 6; i++) {
			currentExpected = currentExpected == a ? b : a;
			game.update();
			Assert.assertEquals(i, game.currentGen());
			assertEqual(currentExpected, game.getCells(-1, -1, 3, 3, i));
		}
	}
	
	private void assertEqual(List<CellModel> expected, List<CellModel> actual) {
		Assert.assertEquals(expected.size(), actual.size());
		Assert.assertTrue(actual.containsAll(expected));
	}
}
