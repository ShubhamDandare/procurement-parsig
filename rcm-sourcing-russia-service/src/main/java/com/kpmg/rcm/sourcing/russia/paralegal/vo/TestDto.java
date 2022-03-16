package com.kpmg.rcm.sourcing.russia.paralegal.vo;

import java.util.Date;

public class TestDto {
	
	String url;
	Date effectiveDate;
	String resultObj;
	
	public TestDto(String url, Date effectiveDate) {
		super();
		this.url = url;
		this.effectiveDate = effectiveDate;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public Date getEffectiveDate() {
		return effectiveDate;
	}
	public void setEffectiveDate(Date effectiveDate) {
		this.effectiveDate = effectiveDate;
	}
	public String getResultObj() {
		return resultObj;
	}
	public void setResultObj(String resultObj) {
		this.resultObj = resultObj;
	}

}
