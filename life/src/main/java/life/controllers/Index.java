package life.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class Index {

	@Autowired
	JdbcTemplate _db;
	
	@RequestMapping("/")
	String index() {
		return "index";
	}
}
