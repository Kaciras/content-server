package com.kaciras.blog.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaciras.blog.infra.principal.SecurityContextFilter;
import com.kaciras.blog.infra.principal.WebPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableLoadTimeWeaving;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// LTW 简直毒瘤啊，各种毛病烦死人
@EnableLoadTimeWeaving
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SnapshotAssertion.ContextHolder.class)
public abstract class AbstractControllerTest {

	public static final WebPrincipal ADMIN = new WebPrincipal(WebPrincipal.ADMIN_ID);
	public static final WebPrincipal ANONYMOUS = new WebPrincipal(WebPrincipal.ANONYMOUS_ID);
	public static final WebPrincipal LOGINED = new WebPrincipal(666);

	protected MockMvc mockMvc;

	@Autowired
	protected WebApplicationContext wac;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected SnapshotAssertion snapshot;

	@BeforeEach
	void setup() {
		var requestTemplate = get("/")
				.principal(ANONYMOUS)
				.contentType(MediaType.APPLICATION_JSON)
				.characterEncoding("UTF-8");

		mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(requestTemplate)
				.addFilter(new SecurityContextFilter())
				.alwaysDo(r ->r.getResponse().setCharacterEncoding("UTF-8"))
				.build();
	}

	/**
	 * 把对象序列化为 JSON 字符串，因为比较长所以提取单独一个方法。
	 *
	 * @param value 对象
	 * @return JSON 字符串
	 */
	protected String toJson(Object value) throws JsonProcessingException {
		return objectMapper.writeValueAsString(value);
	}
}
