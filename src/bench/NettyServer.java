package bench;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.nio.ByteOrder;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class NettyServer {

    public static String HOST = "0.0.0.0";
    public static Integer PORT = 4242;

    public static void main(String[] args) throws Exception {
        ChannelFactory factory =
            new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool());

        ServerBootstrap bootstrap = new ServerBootstrap(factory);

        bootstrap.setPipelineFactory( new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() {
                return Channels.pipeline( new DiscardServerHandler() );
            }
        });

        bootstrap.setOption( "child.tcpNoDelay", true );
        bootstrap.setOption( "child.keepAlive", true );
        bootstrap.setOption( "receiveBufferSize", 16 * 1024 * 1024 );
        bootstrap.setOption( "writeBufferLowWaterMark", 128 * 1024 * 1024 );
        bootstrap.setOption( "writeBufferHighWaterMark", 2 * 1024 * 1024 * 1024 );

        bootstrap.bind( new InetSocketAddress( HOST, PORT ) );
        System.out.println( "Netty server is ready to roll on port " + PORT );
    }
}

class DiscardServerHandler extends SimpleChannelHandler {

    Long count = 0L;
    Long start = System.nanoTime();

    @Override
    public void messageReceived( ChannelHandlerContext ctx, MessageEvent e ) {

        ( ( HeapChannelBuffer ) e.getMessage() ).array();
        count++;

        if ( count % 300000 == 0 ) {
          Long sinceSeconds = ( System.nanoTime() - start ) / ( 1 * 1000 * 1000 * 1000 );
          System.out.println( sinceSeconds + " seconds: received " + count + " ultimate truths" );
          System.out.println( "rate " + count / sinceSeconds + " truths/s\n" );
        }
    }

    @Override
    public void channelConnected( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
        start = System.nanoTime();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();

        Channel ch = e.getChannel();
        ch.close();
    }
}

