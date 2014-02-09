package org.red5.core.manager;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.red5.core.Application;
import org.red5.core.dbModel.GcmUsers;
import org.red5.core.dbModel.RegIDs;
import org.red5.core.utils.JPAUtils;

public class UserManager {

	Application red5App;

	public UserManager(Application red5App) {
		this.red5App = red5App;
	}

	/**
	 * @param mail
	 * @return user with reg ids of mail in the table if mail is not exist, null
	 *         returns else return GcmUsers of mail
	 */
	public GcmUsers getRegistrationIdList(String mail) {

		EntityManager entityManager = JPAUtils.getEntityManager();
		GcmUsers result = null;
		try {

			Query query = entityManager
					.createQuery("FROM GcmUsers where email= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();
			if (results.size() > 0) {
				GcmUsers gcmUsers = (GcmUsers) results.get(0);
				result = gcmUsers;
			}

			JPAUtils.closeEntityManager();
		} catch (NoResultException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public boolean registerUser(String register_id, String mail) {
		boolean result;
		try {
			JPAUtils.beginTransaction();
			Query query = JPAUtils.getEntityManager().createQuery(
					"FROM GcmUsers where email= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();
			if (results.size() > 0) {
				GcmUsers gcmUsers = (GcmUsers) results.get(0);
				RegIDs regid = new RegIDs(register_id);
				gcmUsers.addRegID(regid);
			} else {
				GcmUsers gcmUsers = new GcmUsers(mail);
				RegIDs regid = new RegIDs(register_id);
				gcmUsers.addRegID(regid);
				JPAUtils.getEntityManager().persist(gcmUsers);
			}
			JPAUtils.commit();
			JPAUtils.closeEntityManager();

			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;

	}

	/**
	 * @param register_id
	 *            new register id
	 * @param mail
	 *            user mail
	 * @param oldRegID
	 *            old register id
	 * @return true if the user is updated succesfully , false if fails
	 */
	public boolean updateUser(String register_id, String mail, String oldRegID) {
		boolean result;
		try {
			JPAUtils.beginTransaction();

			Query query = JPAUtils.getEntityManager().createQuery(
					"FROM GcmUsers where email= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();

			// if user is found
			if (results.size() > 0) {
				GcmUsers gcmUsers = (GcmUsers) results.get(0);

				// if reg id doesnt exist for the user
				if (gcmUsers.getRegIDs().size() == 0) {
					RegIDs regid = new RegIDs(register_id);
					gcmUsers.addRegID(regid);
				} else {
					// update the reg id of the user using the old reg id
					for (RegIDs regid : gcmUsers.getRegIDs()) {
						if (regid.getGcmRegId().equals(oldRegID)) {
							regid.setGcmRegId(register_id);
						}
					}
				}

			} else {
				// user doesnt exist, create user and add reg id
				GcmUsers gcmUsers = new GcmUsers(mail);
				RegIDs regid = new RegIDs(register_id);
				gcmUsers.addRegID(regid);
				JPAUtils.getEntityManager().persist(gcmUsers);
			}

			JPAUtils.commit();
			JPAUtils.closeEntityManager();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;

	}
	
	public int getUserCount(String mail) {

		int result = 0;
		try {

			Query query = JPAUtils.getEntityManager().createQuery(
					"FROM GcmUsers where email= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();
			result = results.size();
			JPAUtils.closeEntityManager();

		} catch (NoResultException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}
	
	public boolean deleteUser(int userId) {
		boolean result;
		try {
			JPAUtils.beginTransaction();

			GcmUsers foundUser = JPAUtils.getEntityManager().find(GcmUsers.class, userId);
			
			JPAUtils.getEntityManager().remove(foundUser);
			
			JPAUtils.commit();
			JPAUtils.closeEntityManager();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}

}
