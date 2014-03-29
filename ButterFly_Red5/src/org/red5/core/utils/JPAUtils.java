package org.red5.core.utils;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.red5.core.dbModel.GcmUsers;

public class JPAUtils {
	
	private static EntityManager entityManager;
	private static EntityManagerFactory entityManagerFactory;


	public static void beginTransaction() {
		getEntityManager().getTransaction().begin();
	}
	
	public static EntityManager getEntityManager() {
		
		if (entityManager == null || entityManager.isOpen() == false) {
			if (entityManagerFactory == null || entityManagerFactory.isOpen() == false) {
				entityManagerFactory = Persistence
					.createEntityManagerFactory("ButterFly_Red5");			
			}
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
