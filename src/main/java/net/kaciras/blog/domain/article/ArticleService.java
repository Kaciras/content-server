package net.kaciras.blog.domain.article;

import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import net.kaciras.blog.domain.DeletedState;
import net.kaciras.blog.domain.SecurtyContext;
import net.kaciras.blog.domain.permission.Authenticator;
import net.kaciras.blog.domain.permission.AuthenticatorFactory;
import net.kaciras.blog.infrastructure.event.article.ArticleCreatedEvent;
import net.kaciras.blog.infrastructure.event.article.ArticleUpdatedEvent;
import net.kaciras.blog.infrastructure.exception.PermissionException;
import net.kaciras.blog.infrastructure.exception.ResourceDeletedException;
import net.kaciras.blog.infrastructure.message.MessageClient;
import org.jetbrains.annotations.Async;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ArticleService {

	private final ArticleRepository articleRepository;
	private final ClassifyDAO classifyDAO;
	private final ArticleMapper articleMapper;

	private final MessageClient messageClient;

	private Observable<Article> hots;

	private Authenticator authenticator;

	@Autowired
	public void setAuthenticator(AuthenticatorFactory factory) {
		this.authenticator = factory.create("ARTICLE");
	}

	/**
	 * 检查用户是否有权限更改指定的的文章。
	 *
	 * @param article 文章
	 * @return 文章
	 * @throws PermissionException 如果没权限
	 */
	private Article requireModify(Article article) {
		authenticator.require("MODIFY");
		boolean noPerm = !authenticator.check("POWER_MODIFY");

		if (article.isDeleted() && noPerm) {
			throw new ResourceDeletedException();
		}
		if (SecurtyContext.isNotUser(article.getUserId()) && noPerm) {
			throw new PermissionException();
		}
		return article;
	}

	public Observable<Article> getHots() {
		return hots;
	}

	public Single<Article> getArticle(int id) {
		Article article = articleRepository.get(id);
		if (article.isDeleted() && !authenticator.check("SHOW_DELETED")) {
			throw new ResourceDeletedException();
		}
		return Single.just(article).doAfterSuccess(Article::recordView); //增加浏览量
	}

	@Async.Schedule
	@Scheduled(fixedDelay = 5 * 60 * 1000)
	void updateHotsTask() {
		ArticleListRequest request = new ArticleListRequest();
		request.setDesc(true);
		request.setSort("view_count");
		request.setCount(6);
		hots = articleRepository.findAll(request);
	}

	public Observable<Article> getList(ArticleListRequest request) {
		if (request.getDeletion() != DeletedState.FALSE) {
			authenticator.require("SHOW_DELETED");
		}
		return articleRepository.findAll(request);
	}

	public int getCountByCategories(List<Integer> ids) {
		return classifyDAO.selectCountByCategory2(ids);
	}

	@Deprecated
	public int getCountByCategories0(int id) {
		return classifyDAO.selectCountByCategory(id);
	}

	@Transactional
	public int publish(ArticlePublishDTO publish) {
		authenticator.require("PUBLISH");

		Article article = articleMapper.publishToArticle(publish);
		article.setUserId(SecurtyContext.getCurrentUser());

		articleRepository.add(article);
		article.setCategories(publish.getCategories());

		messageClient.send(new ArticleCreatedEvent(article.getId(), publish.getDraftId(), publish.getCategories()));
		return article.getId();
	}

	public void update(int id, ArticlePublishDTO publish) {
		Article a = articleRepository.get(id);
		requireModify(a);

		Article article = articleMapper.publishToArticle(publish);
		article.setId(id);
		article.setUserId(a.getUserId());
		articleRepository.update(article);

		if (publish.getCategories() != null) {
			article.setCategories(publish.getCategories());
		}

		messageClient.send(new ArticleUpdatedEvent(id, publish.getDraftId(), publish.getCategories()));
	}

	public void changeCategory(int id, List<Integer> categories) {
		requireModify(articleRepository.get(id)).setCategories(categories);
	}

	public void updateDeleteion(int id, boolean value) {
		Article article = articleRepository.get(id);
		if(SecurtyContext.isNotUser(article.getUserId())) {
			authenticator.require("POWER_MODIFY");
		} else {
			authenticator.require("PUBLISH");
		}

		if(value)
			article.delete();
		else
			article.recover();
	}
}
