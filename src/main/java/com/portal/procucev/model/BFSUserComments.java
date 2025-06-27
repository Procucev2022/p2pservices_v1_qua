package com.portal.procucev.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="bfs_user_comments")
public class BFSUserComments extends Procucev{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String comments;
	
	@Column(name="commented_by")
	private String commentedBy;
	
	private String companyName;
	
	@ManyToOne(fetch = FetchType.LAZY)
	private User user;
	
	@ManyToOne
	private BFSItems items;

	
	public BFSUserComments(String id,Date createdTS,String comments, String commentedBy, String companyName) {
		super();
		this.id=id;
		this.createdTS=createdTS;
		this.comments = comments;
		this.commentedBy = commentedBy;
		this.companyName = companyName;
	}
	
	
	
}
