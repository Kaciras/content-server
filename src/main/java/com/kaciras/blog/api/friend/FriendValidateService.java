package com.kaciras.blog.api.friend;

import com.kaciras.blog.api.notification.FriendAccident;
import com.kaciras.blog.api.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;

/**
 * 定时扫描对方的网站，检查是否嗝屁（默哀），以及单方面删除本站（为什么不跟人家做朋友了）。
 * <p>
 * 【安全性】
 * 发送请求可能暴露服务器的地址，这种情况下可以通过 app.http-client.proxy 设置代理。
 */
@RequiredArgsConstructor
@Service
public class FriendValidateService {

	private final NotificationService notificationService;

	private final RedisMap<String, ValidateRecord> validateMap;
	private final Clock clock;
	private final HttpClient httpClient;

	private final TaskScheduler taskScheduler;

	@Value("${app.validate-friend}")
	private boolean enable;

	@Value("${app.origin}")
	private String myOrigin;

	@PostConstruct
	private void init() {
		if (enable) {
			taskScheduler.scheduleAtFixedRate(this::queueValidateTask, Duration.ofDays(1));
		}
	}

	public void addForValidate(FriendLink friend) {
		var url = friend.url;
		validateMap.put(url.getHost(), new ValidateRecord(url, friend.friendPage, friend.createTime, 0));
	}

	public void removeFromValidate(String host) {
		validateMap.remove(host);
	}

	private void queueValidateTask() {
		var queue = new LinkedList<ValidateRecord>();
		validateMap.values().stream().filter(this::shouldValidate).forEach(queue::addFirst);
		validateFriendsAsync(queue);
	}

	private boolean shouldValidate(ValidateRecord record) {
		var p = record.failed > 0 ? Duration.ofDays(7) : Duration.ofDays(30);
		return record.validate.plus(p).isAfter(clock.instant());
	}

	private void validateFriendsAsync(Queue<ValidateRecord> queue) {
		if (queue.isEmpty()) {
			return;
		}
		var record = queue.remove();

		var userAgent = String.format("KacirasBlog Friend Validator (+%s/about/blogger#bot", myOrigin);
		var checkUrl = record.friendPage != null ? record.friendPage : record.url;

		var request = HttpRequest.newBuilder(checkUrl)
				.header("User-Agent", userAgent)
				.timeout(Duration.ofSeconds(10));

		httpClient
				.sendAsync(request.build(), BodyHandlers.ofString())
				.thenAccept(res -> this.handleAliveResponse(record, res))
				.thenRunAsync(() -> validateFriendsAsync(queue));
	}

	private void handleAliveResponse(ValidateRecord record, HttpResponse<String> response) {
		record.validate = clock.instant();

		if (response.statusCode() / 100 != 2) {
			record.failed++;

			if (record.failed > 3) {
				record.failed = 0;
				notificationService.reportFriend(record.url, FriendAccident.Type.Inaccessible);
			}
			updateRecordEntry(record);

		} else if (record.friendPage != null) {
			if (!checkMyLink(response.body())) {
				notificationService.reportFriend(record.url, FriendAccident.Type.AbandonedMe);
			}
			record.failed = 0;
			updateRecordEntry(record);
		}
	}

	/**
	 * 检查指定的HTML页面里是否存在本站的链接。
	 *
	 * @param html HTML页面
	 * @return 如果存在返回true
	 */
	private boolean checkMyLink(String html) {

		Predicate<Element> isLinkToMySite = el -> {
			var href = URI.create(el.attr("href"));
			var origin = href.getScheme() + "://" + href.getHost();
			return myOrigin.equals(origin);
		};

		return Jsoup.parse(html)
				.getElementsByTag("a")
				.stream()
				.anyMatch(isLinkToMySite);
	}

	// SpringDataRedis是真的垃圾……，写个事务都这么丑？
	private void updateRecordEntry(ValidateRecord record) {
		validateMap.getOperations().execute(new SessionCallback<>() {
			public Object execute(RedisOperations operations) {
				var host = record.url.getHost();

				operations.watch(validateMap.getKey());
				var exists = validateMap.containsKey(host);

				operations.multi();
				if (exists) {
					validateMap.put(host, record);
				}
				return operations.exec();
			}
		});
	}
}
