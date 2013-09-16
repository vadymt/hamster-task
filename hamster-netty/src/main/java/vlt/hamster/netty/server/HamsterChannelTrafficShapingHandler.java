package vlt.hamster.netty.server;

import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import vlt.hamster.netty.status.ServerConnectionRecord;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;

/**
 * Used for traffic accounting.
 * 
 * @author vlt
 * 
 * @see io.netty.handler.traffic.ChannelTrafficShapingHandler
 * @see io.netty.handler.traffic.AbstractTrafficShapingHandler
 */
public class HamsterChannelTrafficShapingHandler extends
	ChannelTrafficShapingHandler {
    /**
     * List of last 16 connections.
     */
    private static List<ServerConnectionRecord> serverConnectionList = Collections
	    .synchronizedList(new ArrayList<ServerConnectionRecord>());
    /**
     * Instance of {@link vlt.hamster.netty.status.ServerConnectionRecord
     * ServerConnectionRecord} used for accounting one channel.
     */
    private ServerConnectionRecord serverConnectionRecord = new ServerConnectionRecord();

    public HamsterChannelTrafficShapingHandler(long checkInterval) {
	super(checkInterval);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
	serverConnectionRecord.setSourceIp(((InetSocketAddress) ctx.channel()
		.remoteAddress()).getHostString());
	serverConnectionRecord.setTimestamp(new Timestamp(Calendar
		.getInstance().getTimeInMillis()));
	super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
	    throws Exception {
	if (msg instanceof HttpRequest) {
	    /*
	     * Traffic accounting is recorded in ServerConnectionRecord instance
	     */
	    String uri = ((HttpRequest) msg).getUri();
	    serverConnectionRecord.setUri(uri);
	    trafficAccounting();
	}
	super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	trafficAccounting();
	if(serverConnectionRecord.getUri() == null) {
	    serverConnectionRecord.setUri("No URI Accessed");
	}
	super.channelInactive(ctx);
    }

    /**
     * If there are more than 16 connections in list, removes the first one and
     * adds new one to the end.
     */
    private void addToConnectionList() {
	serverConnectionList.add(serverConnectionRecord);
	if (serverConnectionList.size() > 16) {
	    serverConnectionList.remove(0);
	}

    }

    /**
     * 
     * @return the backwards (from the last element) iterator of the list
     */
    public static ListIterator<ServerConnectionRecord> getServerConnectionListIterator() {
	/*
	 * The list copy is returned so no Concurrency exceptions will be thrown
	 * wgile iterating over changing list.
	 */
	List<ServerConnectionRecord> list = new ArrayList<>(
		serverConnectionList);
	return list.listIterator(list.size());
    }

    private void trafficAccounting() {
	TrafficCounter tc = trafficCounter();
	serverConnectionRecord.setRecievedBytes(Math.abs(tc
		.cumulativeReadBytes()));
	serverConnectionRecord.setSentBytes(tc.cumulativeWrittenBytes());
	serverConnectionRecord.setThroughput(tc.lastWrittenBytes() * 1000
		/ (tc.checkInterval()));
	serverConnectionRecord.setTimestamp(new Timestamp(Calendar
		.getInstance().getTimeInMillis()));
	/*
	 * Instance is added to List on last place (the last the newest)
	 */
	if (serverConnectionList.contains(serverConnectionRecord)) {
	    serverConnectionList.remove(serverConnectionRecord);
	    addToConnectionList();
	} else {
	    addToConnectionList();
	}
    }
}
