package net.kaciras.blog.api.principal.local;

import lombok.RequiredArgsConstructor;
import net.kaciras.blog.api.SessionAttributes;
import net.kaciras.blog.api.Utils;
import net.kaciras.blog.api.principal.AuthType;
import net.kaciras.blog.api.principal.SessionService;
import net.kaciras.blog.api.user.UserManager;
import net.kaciras.blog.infrastructure.exception.RequestArgumentException;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.net.InetAddress;
import java.net.URI;
import java.sql.SQLException;
import java.time.Clock;

@RequiredArgsConstructor
@RestController
@RequestMapping("/accounts")
class AccountController {

	private static final long CAPTCHA_LIFE_TIME = 5 * 60 * 1000;

	private final Clock clock;

	private final AccountRepository repository;
	private final SessionService sessionService;
	private final UserManager userManager;

	@PostMapping
	public ResponseEntity<Void> post(@Valid @RequestBody RegisterRequest dto,
									 HttpServletRequest request,
									 HttpServletResponse response) {
		checkCaptcha(request.getSession(true), dto.getCaptcha());

		var account = createAccount(dto, Utils.addressFromRequest(request));
		sessionService.putUser(request, response, account.getId(), true);
		return ResponseEntity.created(URI.create("/accounts/" + account.getId())).build();
	}

	@Transactional
	protected Account createAccount(RegisterRequest request, InetAddress ip) {
		try {
			var id = userManager.createNew(request.getName(), AuthType.Local, ip);
			var account = Account.create(id, request.getName(), request.getPassword());
			repository.add(account);
			return account;
		} catch (SQLException e) {
			throw new RequestArgumentException("用户名已被使用");
		}
	}

	/**
	 * 检查用户输入的验证码是否正确且在有效期内。
	 * 【注意】会话中的验证码是一次性的，在该方法里会被移除。
	 *
	 * @param session 会话
	 * @param value   用户输入的验证码
	 * @throws RequestArgumentException 如果检查出错误
	 */
	private void checkCaptcha(HttpSession session, @NonNull String value) {
		var except = session.getAttribute(SessionAttributes.CAPTCHA);
		session.removeAttribute(SessionAttributes.CAPTCHA);
		if (!value.equals(except)) {
			throw new RequestArgumentException("验证码错误");
		}

		var time = (long) session.getAttribute(SessionAttributes.CAPTCHA_TIME);
		if (clock.millis() - time > CAPTCHA_LIFE_TIME) {
			throw new RequestArgumentException("验证码已过期，请重试");
		}
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginVo,
								   HttpServletRequest request,
								   HttpServletResponse response) {
		var account = repository.findByName(loginVo.getName());

		if (account == null || !account.checkLogin(loginVo.getPassword())) {
			throw new RequestArgumentException("密码错误或用户不存在");
		}
		sessionService.putUser(request, response, account.getId(), loginVo.isRemember());
		return ResponseEntity.created(URI.create("/session/user")).build();
	}
}
