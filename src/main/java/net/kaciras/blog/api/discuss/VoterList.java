package net.kaciras.blog.api.discuss;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 点赞者列表
 */
@RequiredArgsConstructor
@Configurable
public class VoterList {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private VoteDAO voteDAO;

	private final long discussion;

	/**
	 * 点赞，一个用户只能点赞一次
	 *
	 * @param userId 点赞用户的id
	 * @return 如果成功则为true，若是已经点赞过或出现其他错误则返回false。
	 */
	public boolean add(int userId) {
		try {
			voteDAO.insertRecord(discussion, userId);
			voteDAO.increaseVote(discussion);
			return true;
		} catch (DataIntegrityViolationException ex) {
			return false;
		}
	}

	/**
	 * 取消点赞，只有先点赞了才能取消
	 *
	 * @param userId 点赞用户的id
	 * @return 如果成功则为true，若是用户还未点赞过则返回false。
	 */
	public boolean remove(int userId) {
		if (voteDAO.deleteRecord(discussion, userId) > 0) {
			voteDAO.descreaseVote(discussion);
			return true;
		}
		return false;
	}

	public boolean contains(int userId) {
		var res = voteDAO.contains(discussion, userId);
		return res != null && res;
	}
}
