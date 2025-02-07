package com.kaciras.blog.api.friend;

import com.kaciras.blog.api.notice.ActivityType;
import com.kaciras.blog.api.notice.NoticeService;
import com.kaciras.blog.infra.autoconfigure.RedisUtilsAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static com.kaciras.blog.api.friend.TestHelper.createFriend;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.noInteractions;

@ActiveProfiles("test")
@Import({RedisUtilsAutoConfiguration.class, FriendValidateService.class})
@AutoConfigureJson
@DataRedisTest(properties = "app.origin=https://blog.example.com")
final class FriendValidateServiceTest {

	@MockitoBean
	private NoticeService notification;

	@MockitoBean
	private FriendRepository repository;

	@MockitoBean
	private Clock clock;

	@MockitoBean
	private FriendValidator validator;

	@Autowired
	private FriendValidateService service;

	@Autowired
	private RedisConnectionFactory redis;

	@BeforeEach
	void flushDb() {
		redis.getConnection().serverCommands().flushDb();
	}

	/**
	 * 添加一条友链记录到验证服务。
	 *
	 * @param domain     友链域名，注意不能重复哦
	 * @param friendPage 对方的友链页
	 * @param time       添加的时间
	 * @return 返回创建的友链对象
	 */
	private FriendLink addRecord(String domain, String friendPage, Instant time) {
		when(clock.instant()).thenReturn(time);

		var friend = createFriend(domain, friendPage, time);
		service.addForValidate(friend);
		when(repository.findByHost(eq(domain))).thenReturn(friend);

		// 恢复到当前时间
		when(clock.instant()).thenReturn(Instant.now());
		return friend;
	}

	@Test
	void remove() {
		addRecord("example.com", null, Instant.EPOCH);
		service.removeFromValidate("example.com");

		service.startValidation();
		verify(validator, never()).visit(any());
	}

	@Test
	void shouldValidate() {
		var now = Instant.now();
		var f1 = addRecord("1.com", null, Instant.EPOCH);
		addRecord("2.com", null, now);
		var f3 = addRecord("3.com", null, Instant.EPOCH);
		setValidateResult(false, null, null);

		when(clock.instant()).thenReturn(now);
		service.startValidation();

		verify(validator, times(2)).visit(any());
		verify(validator).visit(eq(f1.url));
		verify(validator).visit(eq(f3.url));
	}

	private void setValidateResult(boolean alive, URI newUrl, String html) {
		var rv = new FriendSitePage(alive, newUrl, null, html);
		when(validator.visit(any())).thenReturn(CompletableFuture.completedFuture(rv));
	}

	@Test
	void allowSmallFailedCount() {
		addRecord("example.com", null, Instant.EPOCH);
		setValidateResult(false, null, null);

		service.startValidation();

		verify(notification, noInteractions()).notify(any());
	}

	@Test
	void notAlive() {
		var friend = addRecord("example.com", null, Instant.EPOCH);
		setValidateResult(false, null, null);

		for (int i = 0; i < 4; i++) {
			when(clock.instant()).thenReturn(Instant.MAX, Instant.EPOCH);
			service.startValidation();
		}

		var captor = ArgumentCaptor.forClass(FriendAccident.class);
		verify(notification).notify(captor.capture());

		var activity = captor.getValue();
		assertThat(activity.getType()).isEqualTo(FriendAccident.Type.LOST);
		assertThat(activity.getUrl()).isEqualTo(friend.url);
		assertThat(activity.getName()).isEqualTo(friend.name);
		assertThat(activity.getActivityType()).isEqualTo(ActivityType.FRIEND);
	}

	@Test
	void siteMoved() {
		var friend = addRecord("example.com", null, Instant.EPOCH);
		var newUrl = URI.create("https://new.home");
		setValidateResult(true, newUrl, null);

		service.startValidation();

		var captor = ArgumentCaptor.forClass(FriendAccident.class);
		verify(notification).notify(captor.capture());

		var activity = captor.getValue();
		assertThat(activity.getType()).isEqualTo(FriendAccident.Type.MOVED);
		assertThat(activity.getUrl()).isEqualTo(friend.url);
		assertThat(activity.getName()).isEqualTo(friend.name);
		assertThat(activity.getNewUrl()).isEqualTo(newUrl);
	}

	@Test
	void gotDumped() {
		var friend = addRecord("example.com", "", Instant.EPOCH);
		setValidateResult(true, null, "<html></html>");

		service.startValidation();

		var captor = ArgumentCaptor.forClass(FriendAccident.class);
		verify(notification).notify(captor.capture());

		var activity = captor.getValue();
		assertThat(activity.getType()).isEqualTo(FriendAccident.Type.ABANDONED_ME);
		assertThat(activity.getUrl()).isEqualTo(friend.url);
		assertThat(activity.getName()).isEqualTo(friend.name);
	}

	@Test
	void hasMyLink() throws Exception {
		addRecord("example.com", "", Instant.EPOCH);
		var html = Files.readString(new ClassPathResource("friend-validate-2.html").getFile().toPath());
		setValidateResult(true, null, html);

		service.startValidation();

		verify(notification, noInteractions()).notify(any());
	}

	// 有记录但没有对应的友链，因为检查任务是异步的，所以可能有这种边界情况。
	@Test
	void danglingRecord() {
		addRecord("example.com", null, Instant.EPOCH);
		var newUrl = URI.create("https://new.home");
		setValidateResult(true, newUrl, null);
		when(repository.findByHost(any())).thenReturn(null);

		service.startValidation();

		verify(notification, noInteractions()).notify(any());
	}
}
