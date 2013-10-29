package org.red5.core;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "reg_ids")
public class RegIDs implements java.io.Serializable {

	
	private int id;
	private String gcmRegId;
    public GcmUsers user;
	
	public RegIDs()
	{
		
	}
	
	
	@ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="emailID")
	public GcmUsers getUser() {
		return user;
	}

	public void setUser(GcmUsers user) {
		this.user = user;
	}

	public RegIDs(String gcmRegId)
	{
		this.gcmRegId = gcmRegId;
	}
	
	@Column(name = "gcm_reg_id",  nullable = false)
	public String getGcmRegId() {
		return gcmRegId;
	}
	public void setGcmRegId(String gcmRegId) {
		this.gcmRegId = gcmRegId;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name = "id")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
}
