package com.portal.procucev.model;


import java.io.Serializable;
import java.util.Date;
import java.util.UUID;


import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Data;

@Data
@MappedSuperclass
@EntityListeners({ Procucev.AbstractEntityListener.class })
public class Procucev implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	/*
	 * @GeneratedValue(strategy = GenerationType.AUTO, generator = "uuid2")
	 * 
	 * @GenericGenerator(name = "uuid2", strategy = "uuid2")
	 */
	@Column(name = "uuid")
	protected String id;

	@Column(name = "created_by")
	protected String createdBy;

	@Column(name = "last_modified_by")
	private String lastModifiedBy;

	@Column(name = "created_ts")
	@CreationTimestamp
	protected Date createdTS;

	@Column(name = "last_modified_ts")
	@UpdateTimestamp
	private Date lastModifiedTS;

	public Procucev() {
		super();
	}

	String uid() {
		if (id == null) {
			id = UUID.randomUUID().toString();
		}
		return id;
	}

	public static class AbstractEntityListener {
		@PrePersist
		public void onPrePersist(Procucev abstractEntity) {
			abstractEntity.uid();
		}
	}

}
