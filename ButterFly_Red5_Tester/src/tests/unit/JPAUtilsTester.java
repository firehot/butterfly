package tests.unit;

import static org.junit.Assert.*;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.red5.core.utils.JPAUtils;

public class JPAUtilsTester {
	
	@Test
	public void testReInitialize() {
		EntityManager entityManager = JPAUtils.getEntityManager();
		
		assertNotNull(entityManager);		
		assertEquals(entityManager, JPAUtils.getEntityManager());
		assertTrue(JPAUtils.getEntityManager().isOpen());

		JPAUtils.closeEntityManager();
		
		
		EntityManager entityManager2 = JPAUtils.getEntityManager();
		
		assertNotNull(entityManager2);
		assertEquals(entityManager2, JPAUtils.getEntityManager());
		assertNotEquals(entityManager, JPAUtils.getEntityManager());
		assertTrue(JPAUtils.getEntityManager().isOpen());
	
		JPAUtils.closeEntityManager();
		
	}

}
