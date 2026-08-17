package com.portal.procucev.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Data
@NoArgsConstructor
@Entity
@Table(name = "rfq_documents")
public class RFQDocument extends Procucev {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Owning side back-reference. Excluded from toString to avoid infinite
	 * recursion with {@link Rfq#getRfqDocument()}.
	 */
	@ManyToOne
	@JsonBackReference(value = "rfq_rfqDocuments")
	@ToString.Exclude
	private Rfq rfq;

	@Lob
	@Column(name = "file_details", columnDefinition = "BLOB")
	@ToString.Exclude
	private byte[] fileDetails;

	private String fileName;

	@Basic(fetch = FetchType.LAZY)
	@Lob
	@ToString.Exclude
	private byte[] file;

	private Integer version;

	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof RFQDocument))
			return false;
		return id != null && id.equals(((RFQDocument) o).getId());
	}

	@Override
	public int hashCode() {
		return 31;
	}
}