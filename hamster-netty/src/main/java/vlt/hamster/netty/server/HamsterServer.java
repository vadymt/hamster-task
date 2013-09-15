/*Необходимо реализовать http-сервер на фреймворке netty (http://netty.io/), со следующим функционалом:

 1. По запросу на http://somedomain/hello отдает «Hello World» через 10 секунд
 2. По запросу на http://somedomain/redirect?url=<url> происходит переадресация на указанный url
 3. По запросу на http://somedomain/status выдается статистика:
 - общее количество запросов
 - количество уникальных запросов (по одному на IP)
 - счетчик запросов на каждый IP в виде таблицы с колонкам и IP, кол-во запросов, время последнего запроса
 - количество переадресаций по url'ам  в виде таблицы, с колонками url, кол-во переадресация
 - количество соединений, открытых в данный момент
 - в виде таблицы лог из 16 последних обработанных соединений, колонки src_ip, URI, timestamp,  sent_bytes, received_bytes, speed (bytes/sec)

 Все это (вместе с особенностями имплементации в текстовом виде) выложить на github, приложить к этому:
 - инструкции как билдить и запускать приложение
 - скриншоты как выглядят станицы /status в рабочем приложении
 - скриншот результата выполнения команды ab –c 100 –n 10000 “http://somedomain/status"*/
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
