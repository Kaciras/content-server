package com.kaciras.blog.api.user;

import com.kaciras.blog.api.SessionAttributes;
import com.kaciras.blog.infra.principal.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@RequiredArgsConstructor
@RestController
@RequestMapping("/session/user")
class SessionUserController {

	private final UserRepository repository;
	private final UserMapper userMapper;

	@GetMapping
	public UserVo get() {
		return userMapper.toUserVo(repository.get(SecurityContext.getUserId()));
	}

	@DeleteMapping
	public ResponseEntity<Void> logout(HttpSession session) {
		session.removeAttribute(SessionAttributes.USER_ID);
		return ResponseEntity.status(HttpStatus.RESET_CONTENT).build();
	}

	@PatchMapping
	public ResponseEntity<Void> patch(@RequestBody @Valid PatchMap patchMap) {
		SecurityContext.requireLogin();
		var user = repository.get(SecurityContext.getUserId());

		user.setName(patchMap.getName());
		user.setAvatar(patchMap.getAvatar());
		repository.update(user);

		return ResponseEntity.noContent().build();
	}
}
