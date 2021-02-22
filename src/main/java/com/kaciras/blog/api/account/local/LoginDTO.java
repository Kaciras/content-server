package com.kaciras.blog.api.account.local;

import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
final class LoginDTO {

	@NotNull
	public final String name;

	// 密码要 HASH，所以限制一下长度
	@Length(min = 8, max = 128)
	@NotNull
	public final String password;

	public final boolean remember;
}
