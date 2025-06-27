package com.portal.procucev.model;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="vistiors_data")
public class VisitorsData extends Procucev{

	private static final long serialVersionUID = 1L;
	
	@Column(name = "login_time")
	private Date loginTime;
	
	@ManyToOne
	private User user;

}
