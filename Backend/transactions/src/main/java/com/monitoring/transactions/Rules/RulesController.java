package com.monitoring.transactions.Rules;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = {"http://localhost:8090", "http://10.9.65.182:8090"})
@RequestMapping("/rules")
public class RulesController {

	private final RulesServices rulesServices;

	public RulesController(RulesServices rulesServices) {
		this.rulesServices = rulesServices;
	}

	@GetMapping
	public List<Rules> getAllRules() {
		return rulesServices.getAllRules();
	}

	@GetMapping("/{id}")
	public Rules getRuleById(@PathVariable Long id) {
		return rulesServices.getRuleById(id);
	}

	@PostMapping
	public ResponseEntity<Rules> createRule(@RequestBody Rules rule) {
		Rules created = rulesServices.createRule(rule);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{id}")
	public Rules updateRule(@PathVariable Long id, @RequestBody Rules rule) {
		return rulesServices.updateRule(id, rule);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
		rulesServices.deleteRule(id);
		return ResponseEntity.noContent().build();
	}
}
