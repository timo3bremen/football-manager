package com.example.manager.dto;

/**
 * Request DTO für Vertragsverhandlungen
 */
public class ContractNegotiationRequest {
	private long proposedSalary; // Gehalt pro Saison
	private int proposedContractLength; // 2-5 Saisons
	
	public ContractNegotiationRequest() {
	}
	
	public long getProposedSalary() {
		return proposedSalary;
	}
	
	public void setProposedSalary(long proposedSalary) {
		this.proposedSalary = proposedSalary;
	}
	
	public int getProposedContractLength() {
		return proposedContractLength;
	}
	
	public void setProposedContractLength(int proposedContractLength) {
		this.proposedContractLength = proposedContractLength;
	}
}
