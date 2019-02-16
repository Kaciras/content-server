package net.kaciras.blog.api.article;

import lombok.RequiredArgsConstructor;
import net.kaciras.blog.api.DeletedState;
import net.kaciras.blog.infrastructure.event.article.ArticleCreatedEvent;
import net.kaciras.blog.infrastructure.event.article.ArticleUpdatedEvent;
import net.kaciras.blog.infrastructure.exception.ResourceDeletedException;
import net.kaciras.blog.infrastructure.message.MessageClient;
import net.kaciras.blog.infrastructure.principal.SecurityContext;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ArticleAppService {

	private final ArticleRepository repository;
	private final MessageClient messageClient;

	public List<Article> getList(ArticleListQuery query) {
		if (query.getDeletion() != DeletedState.FALSE) {
			SecurityContext.require("SHOW_DELETED");
		}
		return repository.findAll(query);
	}

	/**
	 * 获取一个文章，同时检查文章的删除状态以及用户是否具有查看删除文章的权限。
	 *
	 * @param id 文章ID
	 * @param outside 是否由外部访问的
	 * @return 文章对象
	 * @throws ResourceDeletedException 如果文章被标记为删除，且用户没有查看权限
	 */
	public Article getArticle(int id, boolean outside) {
		var article = repository.get(id);

		if (article.isDeleted()
				&& SecurityContext.checkSelf(article.getUserId(), "SHOW_DELETED")) {
			throw new ResourceDeletedException();
		}
		if (outside) {
			article.recordView(); // 增加浏览量
		}
		return article;
	}

	public String getContent(int id) {
		return repository.get(id).getContent();
	}

	public void addNew(Article article, int draftId) {
		repository.add(article);
		messageClient.send(new ArticleCreatedEvent(article.getId(), draftId, article.getCategory()));
	}

	public void updateContent(ArticleContentBase content, int id) {
		repository.update(article);
		messageClient.send(new ArticleUpdatedEvent(id, update.getDraftId(), update.getCategory()));
	}

	public void updateDeletion(int id, boolean deletion) {
		repository.get(id).updateDeleted(deletion);
	}
}
