package com.kaciras.blog.api.friend;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.kaciras.blog.infra.codec.ImageReference;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;

@AllArgsConstructor(onConstructor_ = @JsonCreator)
public final class FriendLink {

	@URL
	public String url;

	@Length(min = 1, max = 16)
	public String name;

	@NotNull
	public ImageReference favicon;

	@NotNull
	public ImageReference background;

	/** 用于验证对方是否加本站为友链的页面，是完整的URL，为null则不验证  */
	@URL
	public String friendPage;

	/** 成为朋友的时间，在Controller里设置 */
	public Instant createTime;

	public String getHost() {
		return URI.create(url).getHost();
	}

	@Override
	public boolean equals(Object o) {
		var that = (FriendLink) o;
		return getHost().equals(that.getHost());
	}

	@Override
	public int hashCode() {
		return Objects.hash(url);
	}
}
