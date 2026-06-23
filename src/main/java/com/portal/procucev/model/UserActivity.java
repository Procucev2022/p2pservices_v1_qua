package com.portal.procucev.model;

import java.time.LocalDateTime;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "user_activity")
public class UserActivity {
	

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "id")
	    private Long id;

	    @Column(name = "user_name")
	    private String userName;

	    @Column(name = "mobile_num")
	    private String mobileNum;

	    @Column(name = "login_time")
	    private LocalDateTime loginTime;
	    
	    @Column(name = "gmt_bfs")
	    private String gmtBfs;

	    @Column(name = "operation_type")
	    private String operationType;

	    @Column(name = "operation_sub_type")
	    private String operationSubType;

	    @Column(name = "reg_source")
	    private String registeredSource;

	    @Column(name = "role")
	    private String role;

	    @Column(name = "rfq_created_time")
	    private LocalDateTime rfqCreatedTime;

	    @Column(name = "dummy1")
	    private String dummy1;

	    @Column(name = "dummy2")
	    private String dummy2;

	    public UserActivity() {
	    }


}
