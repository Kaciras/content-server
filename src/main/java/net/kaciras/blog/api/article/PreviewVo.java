package net.kaciras.blog.api.article;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.kaciras.blog.api.category.SimpleCategoryVo;
import net.kaciras.blog.api.user.UserVo;
import net.kaciras.blog.infrastructure.codec.ImageRefrence;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 与ArticleVo相比，去掉了content属性
 */
@Data
public final class PreviewVo {

	private int id;
	private String urlTitle;

	private UserVo author;
	private String title;
	private ImageRefrence cover;
	private List<String> keywords;
	private String summary;

	private List<SimpleCategoryVo> cpath;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")
	private LocalDateTime create;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")
	private LocalDateTime update;

	private int vcnt;
	private int dcnt;
	private boolean deleted;
}
