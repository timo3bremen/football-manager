package com.example.manager.dto;

/**
 * Response DTO für Vertragsverhandlungen
 * Enthält Feedback vom Spieler über Gehalt und Vertragslaufzeit
 */
public class ContractNegotiationResponse {
	private boolean accepted; // Wurde das Angebot angenommen?
	private boolean negotiationAborted; // Hat der Spieler die Verhandlung abgebrochen?
	private String message; // Nachricht an den User
	
	// Feedback für Gehalt: "happy" (😊), "neutral" (😐), "unhappy" (😢)
	private String salaryFeedback;
	
	// Feedback für Vertragslaufzeit: "happy" (😊), "neutral" (😐), "unhappy" (😢)
	private String contractLengthFeedback;
	
	// Erwartungen des Spielers (für Hinweise)
	private long minimumSalary;
	private int minimumContractLength;
	
	// Aktuelle Anzahl der Versuche
	private int attemptCount;
	
	public ContractNegotiationResponse() {
	}
	
	public ContractNegotiationResponse(boolean accepted, boolean negotiationAborted, String message, 
			String salaryFeedback, String contractLengthFeedback) {
		this.accepted = accepted;
		this.negotiationAborted = negotiationAborted;
		this.message = message;
		this.salaryFeedback = salaryFeedback;
		this.contractLengthFeedback = contractLengthFeedback;
	}
	
	public boolean isAccepted() {
		return accepted;
	}
	
	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}
	
	public boolean isNegotiationAborted() {
		return negotiationAborted;
	}
	
	public void setNegotiationAborted(boolean negotiationAborted) {
		this.negotiationAborted = negotiationAborted;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getSalaryFeedback() {
		return salaryFeedback;
	}
	
	public void setSalaryFeedback(String salaryFeedback) {
		this.salaryFeedback = salaryFeedback;
	}
	
	public String getContractLengthFeedback() {
		return contractLengthFeedback;
	}
	
	public void setContractLengthFeedback(String contractLengthFeedback) {
		this.contractLengthFeedback = contractLengthFeedback;
	}
	
	public long getMinimumSalary() {
		return minimumSalary;
	}
	
	public void setMinimumSalary(long minimumSalary) {
		this.minimumSalary = minimumSalary;
	}
	
	public int getMinimumContractLength() {
		return minimumContractLength;
	}
	
	public void setMinimumContractLength(int minimumContractLength) {
		this.minimumContractLength = minimumContractLength;
	}
	
	public int getAttemptCount() {
		return attemptCount;
	}
	
	public void setAttemptCount(int attemptCount) {
		this.attemptCount = attemptCount;
	}
}
