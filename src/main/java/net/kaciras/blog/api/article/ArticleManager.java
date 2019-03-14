package net.kaciras.blog.api.article;

import lombok.RequiredArgsConstructor;
import net.kaciras.blog.infrastructure.exception.ResourceDeletedException;
import net.kaciras.blog.infrastructure.principal.SecurityContext;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ArticleManager {

	private final ArticleRepository repository;


	/**
	 * 获取一个文章，但会检查文章的删除状态以及用户是否具有查看删除文章的权限。
	 *
	 * @param id 文章ID
	 * @return 文章对象
	 * @throws ResourceDeletedException 如果文章被标记为删除，且用户没有查看权限
	 */
	public Article getLiveArticle(int id) {
		var article = repository.get(id);

		if (article.isDeleted()
				&& SecurityContext.checkSelf(article.getUserId(), "SHOW_DELETED")) {
			throw new ResourceDeletedException();
		}
		return article;
	}
}
