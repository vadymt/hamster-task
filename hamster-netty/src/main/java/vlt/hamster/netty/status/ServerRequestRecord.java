package vlt.hamster.netty.status;

import java.sql.Timestamp;
import java.util.Calendar;

/**
 * This class is the representation of record of server request. It holds
 * information about source IP address, time of last request from that address
 * and number of requests made from that IP address. It also reflects the
 * structure of database table server_request.
 * 
 * @author vlt
 * 
 */

public class ServerRequestRecord {
    private long id;
    private String sourceIp;
    private Timestamp lastRequest;
    private long requestCount;

    public ServerRequestRecord(String sourceIp) {
	super();
	this.sourceIp = sourceIp;
	this.lastRequest = new Timestamp(Calendar.getInstance()
		.getTimeInMillis());
    }

    public long getId() {
	return id;
    }

    public void setId(long id) {
	this.id = id;
    }

    public String getSourceIp() {
	return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
	this.sourceIp = sourceIp;
    }

    public Timestamp getLastRequest() {
	return lastRequest;
    }

    public void setLastRequest(Timestamp lastRequest) {
	this.lastRequest = lastRequest;
    }

    public long getRequestCount() {
	return requestCount;
    }

    public void setRequestCount(long requestCount) {
	this.requestCount = requestCount;
    }

}
