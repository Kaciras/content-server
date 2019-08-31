package net.kaciras.blog.api.article.model;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kaciras.blog.api.Utils;
import net.kaciras.blog.infrastructure.exception.RequestArgumentException;
import net.kaciras.blog.infrastructure.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Repository
public class ArticleRepository {

	private final ArticleDAO articleDAO;
	private final KeywordDAO keywordDAO;
	private final ClassifyDAO classifyDAO;

	/**
	 * 查询一篇文章，如果文章不存在则抛出 ResourceNotFoundException 异常。
	 *
	 * @param id 文章ID
	 * @return 文章对象
	 * @throws ResourceNotFoundException 如果指定ID的文章不存在
	 */
	@NonNull
	public Article get(int id) {
		return articleDAO.selectById(id).orElseThrow(ResourceNotFoundException::new);
	}

	@Transactional
	public void add(@NonNull Article article) {
		try {
			articleDAO.insert(article);
			insertKeywords(article.getId(), article.getKeywords());
		} catch (DataIntegrityViolationException ex) {
			throw new RequestArgumentException();
		}
	}

	@Transactional
	public void update(@NonNull Article article) {
		try {
			Utils.checkEffective(articleDAO.update(article));
			keywordDAO.clear(article.getId());
			insertKeywords(article.getId(), article.getKeywords());
		} catch (DataIntegrityViolationException ex) {
			throw new RequestArgumentException();
		}
	}

	private void insertKeywords(int articleId, List<String> keywords) {
		keywords.stream().map(String::trim)
				.filter(kw -> !kw.isEmpty())
				.forEach(kw -> keywordDAO.insert(articleId, kw));
	}

	public List<Article> findAll(@NonNull ArticleListQuery query) {
		return articleDAO.selectPreview(query);
	}

	public int size() {
		return articleDAO.selectCount();
	}

	/**
	 * 获取所有的文章最后更新的时间，可以认为在此时间之后没有
	 * 任何文章做过改动，也没有新发表文章。
	 *
	 * @return 最后更新的时间
	 */
	public LocalDateTime lastUpdate() {
		return articleDAO.selectLastUpdateTime();
	}

	public int count(ArticleListQuery query) {
		if (query.getCategory() == 0 && query.isRecursive()) {
			return size();
		}
		return classifyDAO.selectCount(query);
	}
}
