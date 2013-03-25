package bench;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SimpleServer {
    
  public static final Long THINGS_TO_EXPECT = 10L * 1000 * 1000;
  public static final String HOST = "0.0.0.0";
  public static final String ULTIMATE_TRUTH = 
      "Did you know that the Answer to the Ultimate Question of Life, the Universe, and Everything is 42? Did you?";

  public static void main( String[] args) {

    ServerSocketChannel channel = null;
  
    if ( args.length >= 1 ) {

      try {
 
        Integer port = Integer.parseInt( args[0] );

        Long thingsToExpect = THINGS_TO_EXPECT;
        if ( args.length > 1 ) thingsToExpect = Long.parseLong( args[1] );
    
        channel = ServerSocketChannel.open();
        channel.socket().bind( new InetSocketAddress ( "0.0.0.0", 4242 ) );
        channel.socket().setReceiveBufferSize( 16 * 1024 * 1024 );
    
        System.out.print( "Listening on " + HOST + ":" + port + " for incoming...." );
        SocketChannel client = channel.accept();
        System.out.println( "[Client Connected]" );

        // channel.configureBlocking( false );
        Long count = 0L;

        Long start = System.nanoTime();

        for ( long i = 0; i < thingsToExpect; i++ ) {

          ByteBuffer truth = ByteBuffer.allocate( ULTIMATE_TRUTH.length() );
          client.read( truth );

          count++;
          if ( count % 1000000 == 0 ) {
            Long sinceSeconds = ( System.nanoTime() - start ) / ( 1 * 1000 * 1000 * 1000 );
            System.out.println( sinceSeconds + " seconds: received " + count + " ultimate truths" );
            System.out.println( "rate " + count / sinceSeconds + " truths/s\n" );
          }
        }
      } 
  
      catch ( IOException ioe ) {
        ioe.printStackTrace();
  
      } finally {
          
        if ( channel != null ) {
          try {
            channel.close();
          } 
          catch ( IOException ioe ) {
            ioe.printStackTrace();
          }
        }
      }
    }
    else {
      System.out.println( "usage: Server port [number of things to expect]" );
    }
  }
}
