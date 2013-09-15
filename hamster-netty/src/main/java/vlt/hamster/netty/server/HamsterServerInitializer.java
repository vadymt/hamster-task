package vlt.hamster.netty.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;

/**
 * {@link io.netty.channel.Channel Channel} intializer for this application.
 * Used for adding handlers to the channel pipeline.
 * 
 * @author vlt
 * @see ChannelInitializer
 */

public class HamsterServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
	ChannelPipeline p = ch.pipeline();
	p.addLast("codec", new HttpServerCodec());
	p.addLast("traffic", new HamsterChannelTrafficShapingHandler(
		AbstractTrafficShapingHandler.DEFAULT_CHECK_INTERVAL));
	p.addLast("handler", new HamsterServerHandler());
    }

}
