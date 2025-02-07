package com.kaciras.blog.api.discuss;

import com.kaciras.blog.api.MappingListView;
import com.kaciras.blog.api.config.BindConfig;
import com.kaciras.blog.api.notice.NoticeService;
import com.kaciras.blog.api.user.UserRepository;
import com.kaciras.blog.infra.RequestUtils;
import com.kaciras.blog.infra.exception.PermissionException;
import com.kaciras.blog.infra.exception.RequestArgumentException;
import com.kaciras.blog.infra.principal.RequirePermission;
import com.kaciras.blog.infra.principal.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 评论的数据结构是一颗含有不同类型对象的树，根节点是主题，下面的节点是评论。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/discussions")
class DiscussionController {

	private final TopicRegistration topics;
	private final DiscussionRepository repository;
	private final ViewModelMapper mapper;

	private final NoticeService noticeService;

	private final UserRepository userRepository;

	@BindConfig("discussion")
	@Setter
	private DiscussionOptions options;

	/**
	 * 验证查询参数是否合法，该方法只检查用户的请求，对于内部查询不限制。
	 * 查询必须包含的过滤条件，如果是管理则可以无视。
	 *
	 * @param query 查询对象
	 */
	private void verifyQuery(DiscussionQuery query) {
		if (query.getNestId() == null && (query.getType() == null || query.getObjectId() == null)) {
			SecurityContext.require("POWER_QUERY");
		}
		if (query.getState() != DiscussionState.VISIBLE) {
			SecurityContext.require("POWER_QUERY");
		}

		// 若前端没设置 Pageable，Spring 会创建一个默认的
		if (query.getPageable().getPageSize() > 30) {
			throw new RequestArgumentException("查询的数量过多");
		}
	}

	/**
	 * 查询评论列表，因为是公共 API 所以有一些限制以防止查询结果过多。
	 *
	 * <h2>Sort 的绑定</h2>
	 * 请求中包含 sort=a,b,DESC 会解析为两个 Order，对应 a, b 两个字段，都是 DESC 降序。
	 * 如果要混合升降顺序，得使用多个 sort 参数：sort=f0,ASC&sort=f1,DESC
	 * Qualifier, SortDefault, SortDefaults 可以改变一些默认的行为，SpringBoot 也提供了对参数名的配置。
	 */
	@GetMapping
	public MappingListView<Integer, DiscussionVO> getList(@Valid DiscussionQuery query, Pageable pageable) {
		query.setPageable(pageable);
		verifyQuery(query);

		var session = new QueryWorker(repository, topics, mapper);
		var items = session.execute(query);
		var total = repository.count(query);

		return new MappingListView<>(total, items, session.getObjects());
	}

	@PostMapping
	public ResponseEntity<DiscussionVO> publish(
			HttpServletRequest request,
			@Valid @RequestBody PublishDTO input) {
		if (options.disabled) {
			throw new PermissionException("已禁止评论");
		}
		if (options.loginRequired) {
			SecurityContext.requireLogin();
		}

		var discussion = mapper.fromInput(input);
		discussion.setUserId(SecurityContext.getUserId());
		discussion.setState(options.moderation
				? DiscussionState.MODERATION
				: DiscussionState.VISIBLE);
		discussion.setAddress(RequestUtils.addressFrom(request));

		// 获取主题，同时检查是否存在
		Discussion parent;
		Topic topic;
		var pid = discussion.getParent();
		if (pid != 0) {
			parent = repository.get(pid).orElseThrow(RequestArgumentException::new);
			topic = topics.get(parent);
		} else {
			parent = null;
			topic = topics.get(discussion);
		}

		repository.add(discussion);

		var activity = mapper.createActivity(discussion, topic);
		activity.setUser(userRepository.get(discussion.getUserId()));
		if (parent != null) {
			// 父评论存在，则 Nest 一定不为 null。
			var nestRoot = repository.get(discussion.getNestId()).orElseThrow();
			activity.setTopicFloor(nestRoot.getNestFloor());
			activity.setParentEmail(parent.getEmail());
			activity.setParentUser(userRepository.get(parent.getUserId()));
		}
		noticeService.notify(activity);

		return ResponseEntity
				.created(URI.create("/discussions/" + discussion.getId()))
				.body(mapper.toViewObject(discussion));
	}

	/**
	 * 批量更新评论的状态，在后台可能有用。
	 */
	@RequirePermission
	@PatchMapping
	@Transactional
	public ResponseEntity<Void> update(@RequestBody UpdateDTO data) {
		for (var id : data.ids) {
			repository.updateState(id, data.state);
		}
		return ResponseEntity.noContent().build();
	}
}
