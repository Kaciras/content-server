package net.kaciras.blog.domain.user;

import lombok.RequiredArgsConstructor;
import net.kaciras.blog.domain.Utils;
import net.kaciras.blog.infrastructure.io.DBUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
class UserRepository {

	private final User guest = new User(0, "游客");

	private final UserDao userDao;

	public void add(User user) {
		userDao.insert(user);
	}

	@Nullable
	public User get(int id) {
		if (id == 0) {
			return guest;
		}
		return userDao.select(id);
	}

	public void delete(int id) {
		DBUtils.checkEffective(userDao.delete(id));
	}

	public User getByName(String name) {
		return userDao.selectByName(name);
	}
}
