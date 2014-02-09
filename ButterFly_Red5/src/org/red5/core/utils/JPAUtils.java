package org.red5.core.utils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JPAUtils {
	
	private static EntityManager entityManager;


	public static void beginTransaction() {
		getEntityManager().getTransaction().begin();
	}
	
	public static EntityManager getEntityManager() {
		if (entityManager == null) {
			EntityManagerFactory entityManagerFactory = Persistence
					.createEntityManagerFactory("ButterFly_Red5");
			entityManager = entityManagerFactory.createEntityManager();
		}
		return entityManager;
	}
	
	public static void commit() {
		getEntityManager().getTransaction().commit();
	}

	public static void closeEntityManager() {
		getEntityManager().close();
		entityManager = null;
	}
}
