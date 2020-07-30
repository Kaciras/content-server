package com.kaciras.blog.api.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/notifications")
class NotificationController {

	private final NotificationRepository repository;

	@GetMapping
	Object getAll() {
		return repository.getAll();
	}

	@DeleteMapping
	ResponseEntity<Void> clear() {
		repository.clear();
		return ResponseEntity.noContent().build();
	}
}
