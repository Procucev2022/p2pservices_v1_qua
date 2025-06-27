/**
 * 
 */
package com.portal.procucev.model;

import java.util.Arrays;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Harshitha
 *
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "rfq_comments")
public class RFQComment extends Procucev {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String comment;

	private String filetype;

	@Basic(fetch = FetchType.LAZY)
	@Lob
	private byte[] file;

	@ManyToOne(fetch = FetchType.LAZY)
	private Rfq rfq;

	@ManyToOne(fetch = FetchType.LAZY)
	private Organization commentby;

	@Column(name = "commented_user")
	private String commentedUser;

	@Column(name = "reply_to")
	private String replyTo;

	
}
