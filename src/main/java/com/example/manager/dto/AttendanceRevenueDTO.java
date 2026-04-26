package com.example.manager.dto;

/**
 * DTO for attendance revenue details
 */
public class AttendanceRevenueDTO {
	private Long teamId;
	private String teamName;
	private long totalAttendance;
	private long regularAttendance;
	private long vipAttendance;
	private long regularRevenue;
	private long vipRevenue;
	private long totalRevenue;
	private int fanSatisfaction;
	private double occupancyRate;

	public AttendanceRevenueDTO() {
	}

	public AttendanceRevenueDTO(Long teamId, String teamName, long totalAttendance, long regularAttendance,
			long vipAttendance, long regularRevenue, long vipRevenue, long totalRevenue, int fanSatisfaction,
			double occupancyRate) {
		this.teamId = teamId;
		this.teamName = teamName;
		this.totalAttendance = totalAttendance;
		this.regularAttendance = regularAttendance;
		this.vipAttendance = vipAttendance;
		this.regularRevenue = regularRevenue;
		this.vipRevenue = vipRevenue;
		this.totalRevenue = totalRevenue;
		this.fanSatisfaction = fanSatisfaction;
		this.occupancyRate = occupancyRate;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	public long getTotalAttendance() {
		return totalAttendance;
	}

	public void setTotalAttendance(long totalAttendance) {
		this.totalAttendance = totalAttendance;
	}

	public long getRegularAttendance() {
		return regularAttendance;
	}

	public void setRegularAttendance(long regularAttendance) {
		this.regularAttendance = regularAttendance;
	}

	public long getVipAttendance() {
		return vipAttendance;
	}

	public void setVipAttendance(long vipAttendance) {
		this.vipAttendance = vipAttendance;
	}

	public long getRegularRevenue() {
		return regularRevenue;
	}

	public void setRegularRevenue(long regularRevenue) {
		this.regularRevenue = regularRevenue;
	}

	public long getVipRevenue() {
		return vipRevenue;
	}

	public void setVipRevenue(long vipRevenue) {
		this.vipRevenue = vipRevenue;
	}

	public long getTotalRevenue() {
		return totalRevenue;
	}

	public void setTotalRevenue(long totalRevenue) {
		this.totalRevenue = totalRevenue;
	}

	public int getFanSatisfaction() {
		return fanSatisfaction;
	}

	public void setFanSatisfaction(int fanSatisfaction) {
		this.fanSatisfaction = fanSatisfaction;
	}

	public double getOccupancyRate() {
		return occupancyRate;
	}

	public void setOccupancyRate(double occupancyRate) {
		this.occupancyRate = occupancyRate;
	}
}
