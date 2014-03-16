package org.red5.core.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.red5.core.Application;
import org.red5.core.dbModel.GcmUserMails;
import org.red5.core.dbModel.GcmUsers;
import org.red5.core.dbModel.RegIds;
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
	public Set<RegIds> getRegistrationIdList(String mail) {

		EntityManager entityManager = JPAUtils.getEntityManager();
		Set<RegIds> result = null;
		try {

			Query query = entityManager
					.createQuery("FROM GcmUserMails WHERE mail= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();
			if (results.size() > 0) {
				GcmUsers gcmUsers = ((GcmUserMails) results.get(0)).getGcmUsers();
				result = gcmUsers.getRegIdses();
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
					"FROM GcmUserMails WHERE mail IN :email");
			
			String[] mails = mail.split(",");
			List<String> mailList = new ArrayList<String>(Arrays.asList(mails));
			
			query.setParameter("email", mailList);
			List<GcmUserMails> results = query.getResultList();
			
			if (results.size() > 0) {
				GcmUserMails userMail = (GcmUserMails) results.get(0);
				RegIds regid = new RegIds(register_id);
				result = addRegID(userMail.getGcmUsers(), regid);

				if (result == true) {
					JPAUtils.getEntityManager().persist(regid);
				}
				
				//remove already existing mails from list
				for (GcmUserMails gcmUserMail : results) {
					mailList.remove(gcmUserMail.getMail());
				}
				//now, mailList have only emails that is not associated with that user
				//add non-existing mails to mail table
				for (int i = 0; i < mailList.size(); i++) {
					GcmUserMails userMails = new  GcmUserMails(userMail.getGcmUsers(), mailList.get(i));
					addGcmUserMail(userMail.getGcmUsers(), userMails);
					JPAUtils.getEntityManager().persist(userMails);
				}
				

			} else {
				GcmUsers gcmUsers = new GcmUsers();
				JPAUtils.getEntityManager().persist(gcmUsers);
				
				
				for (int i = 0; i < mails.length; i++) {
					GcmUserMails userMails = new  GcmUserMails();
					userMails.setMail(mails[i]);
					addGcmUserMail(gcmUsers, userMails);
					JPAUtils.getEntityManager().persist(userMails);
				}
				RegIds regid = new RegIds(register_id);
				addRegID(gcmUsers, regid);
				
				JPAUtils.getEntityManager().persist(regid);
				result = true;
			}
			JPAUtils.commit();
			JPAUtils.closeEntityManager();

			
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
					"FROM GcmUserMails WHERE mail IN :email");
			
			String[] mails = mail.split(",");
			List<String> mailList = new ArrayList<String>(Arrays.asList(mails));
			
			query.setParameter("email", mailList);

			List<GcmUserMails> results = query.getResultList();

			// if user is found
			if (results.size() > 0) {
				GcmUserMails gcmUserMail = (GcmUserMails) results.get(0);
				
				Set<RegIds> regIdses = gcmUserMail.getGcmUsers().getRegIdses();
				
				boolean found = false;
				// update the reg id of the user using the old reg id
				for (RegIds regid : regIdses) {
					if (regid.getGcmRegId().equals(oldRegID)) {
						regid.setGcmRegId(register_id);
						found = true;
						break;
					}
				}
				if (found == false) {
					RegIds regid = new RegIds(register_id);
					addRegID(gcmUserMail.getGcmUsers(), regid);
					JPAUtils.getEntityManager().persist(regid);
				}

			} else {
				//if user does not exists
				GcmUsers gcmUsers = new GcmUsers();
				JPAUtils.getEntityManager().persist(gcmUsers);
				
				for (int i = 0; i < mails.length; i++) {
					GcmUserMails userMails = new  GcmUserMails();
					userMails.setMail(mails[i]);
					addGcmUserMail(gcmUsers, userMails);
					JPAUtils.getEntityManager().persist(userMails);
				}
				RegIds regid = new RegIds(register_id);
				addRegID(gcmUsers, regid);
				
				JPAUtils.getEntityManager().persist(regid);
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



	public boolean deleteRegId(String regId) {
		boolean result;
		try {
			JPAUtils.beginTransaction();

			Query query = JPAUtils.getEntityManager().createQuery("DELETE FROM RegIds WHERE gcmRegId=:regId");
			query.setParameter("regId", regId);
			query.executeUpdate();

			JPAUtils.commit();
			JPAUtils.closeEntityManager();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}

	public boolean addGcmUserMail(GcmUsers gcmUsers, GcmUserMails gMails) {
		boolean found = false;
		Set<GcmUserMails> gcmUserMailses = gcmUsers.getGcmUserMailses();
		if (gcmUserMailses != null) {
			for (GcmUserMails gcmMail : gcmUserMailses) {
				if (gcmMail.getMail().equals(gMails.getMail())) {
					found = true;
					break;
				}
			}
		}
		if (found == false) {
			gcmUsers.getGcmUserMailses().add(gMails);
			gMails.setGcmUsers(gcmUsers);
			return true;
		}
		return false;
	}

	public boolean addRegID(GcmUsers gcmUsers, RegIds regid) {
		boolean found = false;
		Set<RegIds> regIds = gcmUsers.getRegIdses();
		if (regIds != null) {
			for (RegIds regId : regIds) {
				if (regId.getGcmRegId().equals(regid.getGcmRegId())) {
					found = true;
					break;
				}
			}
		}
		if (found == false) {
			gcmUsers.getRegIdses().add(regid);
			regid.setGcmUsers(gcmUsers);
			return true;
		}
		return false;
	}
	
	public GcmUsers getGcmUserByMail(String broadcasterMail) {
		EntityManager entityManager = JPAUtils.getEntityManager();
		GcmUsers result = null;
		try {

			Query query = entityManager
					.createQuery("FROM GcmUserMails WHERE mail= :email");
			query.setParameter("email", broadcasterMail);
			List results = query.getResultList();
			if (results.size() > 0) {
				result = ((GcmUserMails) results.get(0)).getGcmUsers();
				
			}

			JPAUtils.closeEntityManager();
		} catch (NoResultException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
		
	}

	public GcmUsers getGcmUserByRegId(String regId) {
		EntityManager entityManager = JPAUtils.getEntityManager();
		GcmUsers result = null;
		try {

			Query query = entityManager
					.createQuery("FROM RegIds WHERE gcmRegId= :regId");
			query.setParameter("regId", regId);
			List results = query.getResultList();
			if (results.size() > 0) {
				result = ((RegIds) results.get(0)).getGcmUsers();
			}

			JPAUtils.closeEntityManager();
		} catch (NoResultException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

}
