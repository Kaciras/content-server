package net.kaciras.blog.api.article;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.kaciras.blog.infra.codec.ImageReference;

import java.time.Instant;
import java.util.List;

// TODO: 跟ArticleVo越来越像了
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

	private Instant create;
	private Instant update;

	private int vcnt;
	private int dcnt;
	private boolean deleted;

	private String content;
}
