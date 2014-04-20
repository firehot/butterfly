package org.red5.core.utils;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.red5.core.dbModel.GcmUsers;

public class JPAUtils {
	
	private static EntityManagerFactory entityManagerFactory;
	private static ThreadLocal<EntityManager> threadLocalEntityManager = new ThreadLocal<EntityManager>() {
		protected EntityManager initialValue() {
			return getFactory().createEntityManager();
		};
	};

	public static void beginTransaction() {
		threadLocalEntityManager.get().getTransaction().begin();
	}
	
	private static EntityManagerFactory getFactory() {
		if (entityManagerFactory == null || entityManagerFactory.isOpen() == false) {
			entityManagerFactory = Persistence
				.createEntityManagerFactory("ButterFly_Red5");			
		}
		return entityManagerFactory;
	}
	
	public static EntityManager getEntityManager() {
		
		return threadLocalEntityManager.get();
	}
	
	public static void commit() {
		threadLocalEntityManager.get().getTransaction().commit();
	}

	public static void closeEntityManager() {
		if (threadLocalEntityManager.get().isOpen() == true) {
			threadLocalEntityManager.get().close();
		}
		threadLocalEntityManager.remove();
	}


}
