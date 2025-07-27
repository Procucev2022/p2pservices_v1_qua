package com.portal.procucev.model;

import java.util.List;
import lombok.Data;

@Data
public class ApiResponse {
	 private String Status;
     private List<PostOffice> PostOffice;
}
