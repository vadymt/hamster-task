/*���������� ����������� http-������ �� ���������� netty (http://netty.io/), �� ��������� ������������:

 1. �� ������� �� http://somedomain/hello ������ �Hello World� ����� 10 ������
 2. �� ������� �� http://somedomain/redirect?url=<url> ���������� ������������� �� ��������� url
 3. �� ������� �� http://somedomain/status �������� ����������:
 - ����� ���������� ��������
 - ���������� ���������� �������� (�� ������ �� IP)
 - ������� �������� �� ������ IP � ���� ������� � �������� � IP, ���-�� ��������, ����� ���������� �������
 - ���������� ������������� �� url'��  � ���� �������, � ��������� url, ���-�� �������������
 - ���������� ����������, �������� � ������ ������
 - � ���� ������� ��� �� 16 ��������� ������������ ����������, ������� src_ip, URI, timestamp,  sent_bytes, received_bytes, speed (bytes/sec)

 ��� ��� (������ � ������������� ������������� � ��������� ����) �������� �� github, ��������� � �����:
 - ���������� ��� ������� � ��������� ����������
 - ��������� ��� �������� ������� /status � ������� ����������
 - �������� ���������� ���������� ������� ab �c 100 �n 10000 �http://somedomain/status"*/
package vlt.hamster.netty.server;

import java.sql.SQLException;

import vlt.hamster.netty.status.Dao;
import io.netty.bootstrap.*;
import io.netty.channel.*;
import io.netty.channel.nio.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Main server class. Contains server configuration and main method.
 * 
 * @author vlt
 * 
 */

public class HamsterServer {
    private static Dao dao = new Dao();
    /**
     * Port that server will use.
     */
    private final int port;

    public HamsterServer(int port) {
	this.port = port;
    }

    public void run() throws Exception {
	/*
	 * Start database
	 */
	Dao.getConnection();
	/*
	 * Configure server
	 */
	EventLoopGroup bossGroup = new NioEventLoopGroup();
	EventLoopGroup workerGroup = new NioEventLoopGroup();
	try {
	    ServerBootstrap b = new ServerBootstrap();
	    b.option(ChannelOption.SO_BACKLOG, 1024);
	    b.group(bossGroup, workerGroup)
		    .channel(NioServerSocketChannel.class)
		    .childHandler(new HamsterServerInitializer());
	    Channel ch = b.bind(port).sync().channel();
	    ch.closeFuture().sync();
	} finally {
	    bossGroup.shutdownGracefully();
	    workerGroup.shutdownGracefully();
	}
	
    }

    public static void main(String[] args) throws Exception {
	int port;
	if (args.length > 0) {
	    port = Integer.parseInt(args[0]);
	} else {
	    port = 80;
	}
	/*
	 * Shut down database on server stop
	 */
	Runtime.getRuntime().addShutdownHook(new Thread() {
	    public void run() {
		 try {	       
		    Dao.shutdownDb();
		} catch (SQLException e) {
		}
	    }
	});
	new HamsterServer(port).run();
    }

    public static Dao getDao() {
	return dao;
    }

    public static void setDao(Dao dao) {
	HamsterServer.dao = dao;
    }

}
