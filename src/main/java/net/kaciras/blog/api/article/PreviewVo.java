package net.kaciras.blog.api.article;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.kaciras.blog.infrastructure.codec.ImageReference;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 与ArticleVo相比，去掉了content属性
 */
@ToString(of = {"id", "title"})
@Getter
@Setter
public final class PreviewVo {

	private int id;
	private String urlTitle;

	private String title;
	private ImageReference cover;
	private List<String> keywords;
	private String summary;

	private List<SimpleCategoryVo> cpath;

	private LocalDateTime create;
	private LocalDateTime update;

	private int vcnt;
	private int dcnt;
	private boolean deleted;
}
