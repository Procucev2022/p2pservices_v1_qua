package com.portal.procucev.Dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BFSItemDto {

	private List<String> description;
	
	private String location;
	
	private String pincode;
	
	private int quantity;
	
}
