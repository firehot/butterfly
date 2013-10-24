package org.red5.core;
// default package
// Generated 21-Sep-2013 16:53:03 by Hibernate Tools 3.4.0.CR1

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * GcmUsers generated by hbm2java
 */
@Entity
@Table(name = "gcm_users")
public class GcmUsers implements java.io.Serializable {

	private String gcmRegId;
	private String email;

	public GcmUsers() {
	}

	public GcmUsers(String gcmRegId, String email) {
		this.gcmRegId = gcmRegId;
		this.email = email;
	}

	@Id
	@Column(name = "gcm_reg_id", unique = true, nullable = false)
	public String getGcmRegId() {
		return this.gcmRegId;
	}

	public void setGcmRegId(String gcmRegId) {
		this.gcmRegId = gcmRegId;
	}

	@Column(name = "email", nullable = false, length = 45)
	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
