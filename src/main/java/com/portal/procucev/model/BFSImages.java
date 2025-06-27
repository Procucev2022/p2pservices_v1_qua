package com.portal.procucev.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Data
@NoArgsConstructor
@Entity
@Table(name="bfs_images")
public class BFSImages extends Procucev{
	private static final long serialVersionUID = 1L;

	@Column(name = "file_name")
	private String fileName;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	private BFSItems bfs;
	
	@Basic(fetch = FetchType.LAZY)
	@Lob
	private byte[] file;
	
}
