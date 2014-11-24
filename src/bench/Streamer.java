package bench;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.Callable;

public class Streamer implements Callable<Void> {

    private final String id;
    private final String thing;
    private final long thingsToStream;
    private final String host;
    private final int port;

    public Streamer( String id,
                     String host,
                     int port,
                     String thing,
                     long thingsToStream ) {

        this.id = id;
        this.host = host;
        this.port = port;
        this.thing = thing;
        this.thingsToStream = thingsToStream;
    }

    public Void call() throws InterruptedException {

        SocketChannel channel = null;

        try {

            // delaying for 1 to 5 seconds, not to hit netty IO thread at the same time
            Thread.sleep( new Random().nextInt( 4000 ) + 1000 );

            System.out.println( "[streamer " + id + "]: connecting to " + host + ":" + port );

            channel = SocketChannel.open();
            channel.socket().setSendBufferSize( 16 * 1024 * 1024 );
            channel.connect( new InetSocketAddress( host, port ) );

            System.out.println( "[streamer " + id + "]:     ...[CONNECTED]" );

            // channel.configureBlocking( false );
            Long count = 0L;
            ByteBuffer binaryTruth = ByteBuffer.wrap( thing.getBytes() );
            // binaryTruth.order( ByteOrder.LITTLE_ENDIAN );

            System.out.println( "[streamer " + id + "]: streaming away " + thingsToStream + " \"" +
                                binaryTruth.capacity() + " byte\" things" );

            for ( long i = 0; i < thingsToStream; i++ ) {

                channel.write( binaryTruth );
                binaryTruth.flip();

                count++;
                if ( count % 100000 == 0 ) {
                    System.out.println( "[streamer " + id + "]: sent " + count + " ultimate truths" );
                }
            }
        } catch ( IOException ioe ) {
            ioe.printStackTrace();

        } finally {

            if ( channel != null ) {
                try {
                    channel.close();
                } catch ( IOException ioe ) {
                    ioe.printStackTrace();
                }
            }
        }

        return null;
    }
}
