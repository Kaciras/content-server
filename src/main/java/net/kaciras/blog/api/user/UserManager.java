package net.kaciras.blog.api.user;

import lombok.RequiredArgsConstructor;
import net.kaciras.blog.api.principle.AuthType;
import net.kaciras.blog.infrastructure.DBUtils;
import net.kaciras.blog.infrastructure.codec.ImageRefrence;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@RequiredArgsConstructor
@Service
public class UserManager {

	private final UserRepository repository;
	private final UserMapper mapper;

	// TODO: 在哪转换对象
	public UserVo getUser(int id) {
		return mapper.toUserVo(DBUtils.checkNotNullResource(repository.get(id)));
	}

	public int createNew(String name, AuthType authType, InetAddress ip) {
		var user = new User();
		user.setName(name);
		user.setHead(ImageRefrence.parse("akalin.jpg"));
		user.setAuthType(authType);
		user.setRegisterIP(ip);

		repository.add(user);
		return user.getId();
	}
}
