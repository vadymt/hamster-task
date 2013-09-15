package vlt.hamster.netty.status;

import java.sql.Timestamp;
import java.util.Calendar;

/**
 * This class is the representation of record of server connection. It holds
 * information about source IP address, time of last request from that address,
 * last URI and accounting information about sent, received bytes and speed. It
 * also reflects the structure of database table server_connection.
 * 
 * @author vlt
 * 
 */
public class ServerConnectionRecord {
    private long id;
    private String sourceIp;
    private String uri;
    private Timestamp timestamp;
    private long sentBytes;
    private long recievedBytes;
    private long throughput;

    public ServerConnectionRecord() {
	setRecievedBytes(0);
	setSentBytes(0);
	setThroughput(0);
	setTimestamp(new Timestamp(Calendar.getInstance().getTimeInMillis()));
	setSourceIp(null);
	setUri(null);
    }

    public ServerConnectionRecord(String sourceIp, String uri,
	    Timestamp timestamp, long sentBytes, long recievedBytes,
	    long throughput) {
	super();
	this.sourceIp = sourceIp;
	this.uri = uri;
	this.timestamp = timestamp;
	this.sentBytes = sentBytes;
	this.recievedBytes = recievedBytes;
	this.throughput = throughput;
    }

    public String getSourceIp() {
	return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
	this.sourceIp = sourceIp;
    }

    public String getUri() {
	return uri;
    }

    public void setUri(String uri) {
	this.uri = uri;
    }

    public Timestamp getTimestamp() {
	return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
	this.timestamp = timestamp;
    }

    public long getSentBytes() {
	return sentBytes;
    }

    public void setSentBytes(long sentBytes) {
	this.sentBytes = sentBytes;
    }

    public long getRecievedBytes() {
	return recievedBytes;
    }

    public void setRecievedBytes(long recievedBytes) {
	this.recievedBytes = recievedBytes;
    }

    public long getThroughput() {
	return throughput;
    }

    public void setThroughput(long throughput) {
	this.throughput = throughput;
    }

    public long getId() {
	return id;
    }

    public void setId(long id) {
	this.id = id;
    }

}
