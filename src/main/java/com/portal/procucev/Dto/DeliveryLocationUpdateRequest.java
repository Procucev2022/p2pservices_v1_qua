package com.portal.procucev.Dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryLocationUpdateRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;

	private String rfqId;

	private String city;

	private String state;

	private String pincode;

	private String address;

	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
	private Date deliveryDate;
}
