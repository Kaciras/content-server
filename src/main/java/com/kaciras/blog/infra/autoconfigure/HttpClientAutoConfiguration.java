package com.kaciras.blog.infra.autoconfigure;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.util.concurrent.Executor;

/**
 * 虽然 Spring 的 RestTemplate 也不错，但我还是喜欢原生的 HttpClient
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpClientProperties.class)
@RequiredArgsConstructor
public class HttpClientAutoConfiguration {

	private final HttpClientProperties properties;

	@Bean
	public HttpClient httpClient(ApplicationContext context) {
		var builder = HttpClient.newBuilder();

		// 考虑到配置文件没法设为 null 所以空字符串也排除。
		if (StringUtils.hasText(properties.executor)) {
			builder.executor(context.getBean(properties.executor, Executor.class));
		}

		if (properties.proxy != null) {
			var hostPort = properties.proxy.split(":", 2);
			var address = new InetSocketAddress(hostPort[0], Integer.parseInt(hostPort[1]));
			builder.proxy(ProxySelector.of(address));
		}

		if (properties.timeout != null) {
			builder.connectTimeout(properties.timeout);
		}

		return builder.followRedirects(HttpClient.Redirect.NORMAL).build();
	}
}
