package life.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class Index {
	
	@RequestMapping("/")
	String index() {
		return "index";
	}
	
	@RequestMapping("/hello")
	String hello(Model model) {
		model.addAttribute("name", "World");
		return "hello";
	}
	
	@RequestMapping("/hello/{name}")
	String hello(@PathVariable String name, Model model) {
		model.addAttribute("name", name);
		return "hello";
	}
}
