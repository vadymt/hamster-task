package vlt.hamster.netty.server;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import vlt.hamster.netty.status.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.ImmediateEventExecutor;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * Main server handler. Handles input and output. Handles HTTP requests.
 * 
 * @author vlt
 * 
 */
public class HamsterServerHandler extends ChannelInboundHandlerAdapter {
    private static Timer timer = new HashedWheelTimer();
    private static ByteBuf content = null;
    /**
     * Channel group used to determine the number of Channels active
     */
    private static DefaultChannelGroup allChannels = new DefaultChannelGroup(
	    "netty-receiver", ImmediateEventExecutor.INSTANCE);
    private static final String HELLO = "/hello";
    private static final String STATUS = "/status";
    private static final String REDIRECT_REGEXP = "/redirect[?]url=.*";
    private Dao dao = new Dao();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
	allChannels.add(ctx.channel());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
	ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
	    throws Exception {
	FullHttpResponse response = null;
	String requestUri;
	if (msg instanceof HttpRequest) {
	    HttpRequest req = (HttpRequest) msg;
	    boolean keepAlive = isKeepAlive(req);
	    requestUri = req.getUri().toLowerCase();
	    /*
	     * Request recording routine
	     */
	    ServerRequestRecord sReqRecord = new ServerRequestRecord(
		    ((InetSocketAddress) ctx.channel().remoteAddress())
			    .getHostString());
	    dao.mergeServerRequestRecord(sReqRecord);
	    if (is100ContinueExpected(req)) {
		ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
	    }
	    switch (requestUri) {
	    case HELLO:
		/*
		 * If HelloWorld
		 */
		content = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(
			"Hello World!", CharsetUtil.UTF_8));
		response = new DefaultFullHttpResponse(HTTP_1_1, OK,
			content.duplicate());
		/*
		 * Start the timer for /hello page.
		 */
		timer.newTimeout(new HelloWorldTimerTask(ctx, response,
			keepAlive), 10, TimeUnit.SECONDS);
		return;

		/*
		 * if status
		 */
	    case STATUS:
		content = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(
			generateStatus(), CharsetUtil.UTF_8));
		response = new DefaultFullHttpResponse(HTTP_1_1, OK,
			content.duplicate());
		writeToResponse(ctx, response, keepAlive);
		return;
	    default:
		/*
		 * If redirect
		 */
		if (requestUri.matches(REDIRECT_REGEXP)) {
		    QueryStringDecoder qsd = new QueryStringDecoder(requestUri);
		    List<String> redirectUrl = qsd.parameters().get("url");
		    response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
		    response.headers().set(LOCATION, redirectUrl);
		    /*
		     * Redirect database routine
		     */
		    dao.mergeRedirectRequestRecord(new RedirectRequestRecord(
			    redirectUrl.get(0)));
		} else {
		    /*
		     * If request URI is not handled by server.
		     */
		    response = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN);
		}
		writeToResponse(ctx, response, keepAlive);
	    }
	}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	    throws Exception {
	cause.printStackTrace();
	ctx.close();
    }

    /**
     * Generates HTML for /status page.
     * 
     * @return String with HTML content for /status page
     * @throws SQLException
     */
    private String generateStatus() throws SQLException {
	List<ServerRequestRecord> serverRequestList = dao.getServerRequest();
	List<RedirectRequestRecord> redirectRequestList = dao
		.getRedirectRequest();
	StringBuilder sb = new StringBuilder();
	sb.append(
		"<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"><head>")
		.append(
		/*
		 * Table style
		 */
		"<head><style>")
		.append("#table_srequests{float:left;}#table_rrequests{float:left;}")
		.append("#table_sconnections{width: 60%;}")
		.append("table,td{border: 1px solid black;}")
		.append("tbody {height: 300px; overflow: auto;}")
		.append("th{border: 0px; width: 183px;}")
		.append("td {width: 150px; padding: 3px 10px; height:40px}")
		.append("caption{font-size: 16pt; font: bold;}")
		.append("thead > tr, tbody{ display:block;}}</style></head>")
		/*
		 * Requests count
		 */
		.append("Server request count: ")
		.append(dao.getServerRequestcount())
		.append("<br>Server unique requests count: ")
		.append(dao.getServerRequestUniqueCount())
		/*
		 * Table of server requests
		 */
		.append("<br><table id=\"table_srequests\"><caption>Server requests(last ")
		.append(Dao.NUMBER_OF_REQUESTS_OUTPUT).append(")</caption>")
		.append("<thead><tr> <th>IP</th><th>Count</th>")
		.append("<th>Last Request</th> </tr></thead><tbody> ");
	for (ServerRequestRecord record : serverRequestList) {
	    sb.append("<tr><td>")
		    .append(record.getSourceIp())
		    .append("</td><td>")
		    .append(record.getRequestCount())
		    .append("</td><td>")
		    .append(DateFormat.getDateTimeInstance().format(
			    record.getLastRequest())).append("</td></tr>");
	}
	sb.append("</tbody></table>");
	/*
	 * Table of redirect requests
	 */
	sb.append(
		"<table id=\"table_rrequests\"><caption>Redirect requests(last ")
		.append(Dao.NUMBER_OF_REQUESTS_OUTPUT).append(")</caption>")
		.append("<thead><tr><th>URL</th>")
		.append("<th>Count</th> </tr></thead><tbody> ");
	for (RedirectRequestRecord record : redirectRequestList) {
	    sb.append("<tr><td>").append(record.getRedirectUrl())
		    .append("</td><td>").append(record.getRedirectCount())
		    .append("</td>");
	}
	sb.append("</tbody></table>");
	/*
	 * Table of server connections columns src_ip, URI, timestamp,
	 * sent_bytes, received_bytes, speed (bytes/sec)
	 */
	sb.append(
		"<table id=\"table_sconnections\"><caption>Last 16 connections")
		.append("</caption>")
		.append("<tr> <th>IP</th><th>URI</th>")
		.append("<th>TimeStamp</th><th>Sent</th><th>Recieved</th><th>Speed</th> </tr> ");
	ListIterator<ServerConnectionRecord> iterator = HamsterChannelTrafficShapingHandler
		.getServerConnectionListIterator();
	synchronized (iterator) {
	    while (iterator.hasPrevious()) {
		ServerConnectionRecord item = iterator.previous();
		sb.append("<tr><td>")
			.append(item.getSourceIp())
			.append("</td><td>")
			.append(item.getUri())
			.append("</td><td>")
			.append(DateFormat.getDateTimeInstance().format(
				item.getTimestamp())).append("</td><td>")
			.append(item.getSentBytes()).append("</td><td>")
			.append(item.getRecievedBytes()).append("</td><td>")
			.append(item.getThroughput()).append("</td></tr>");
	    }
	}
	sb.append("</table>");
	sb.append("<br>Open connections: ").append(allChannels.size());
	return sb.toString();
    }

    /**
     * Writes response to current ChannelHandlerContext
     * 
     * @param ctx
     *            current ChannelHandlerContext
     * @param response
     * @param keepAlive
     *            determines if connection is keep-alive or not.
     */
    private void writeToResponse(ChannelHandlerContext ctx,
	    FullHttpResponse response, boolean keepAlive) {
	response.headers().set(CONTENT_TYPE, "text/html");
	response.headers().set(CONTENT_LENGTH,
		response.content().readableBytes());
	if (!keepAlive) {
	    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
	} else {
	    response.headers().set(CONNECTION,
		    Values.KEEP_ALIVE + ":timeout=10");
	    ctx.write(response);
	}
    }

    /**
     * {@link io.netty.util.TimerTask TinerTask} implementation which is used to
     * make 10 seconds delay for /hello page.
     * 
     * @author vlt
     * 
     */
    private class HelloWorldTimerTask implements TimerTask {
	private ChannelHandlerContext ctx;
	private FullHttpResponse response;
	private boolean keepAlive;

	public HelloWorldTimerTask(ChannelHandlerContext ctx,
		FullHttpResponse response, boolean keepAlive) {
	    setCtx(ctx);
	    setResponse(response);
	    setKeepAlive(keepAlive);
	}

	@Override
	public void run(Timeout timeout) throws Exception {
	    writeToResponse(ctx, response, keepAlive);
	    ctx.flush();
	}

	public void setCtx(ChannelHandlerContext ctx) {
	    this.ctx = ctx;
	}

	public void setResponse(FullHttpResponse response) {
	    this.response = response;
	}

	public void setKeepAlive(boolean keepAlive) {
	    this.keepAlive = keepAlive;
	}
    }
}
