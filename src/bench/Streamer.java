package bench;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Streamer {

  //public static final String ULTIMATE_TRUTH = 
  //    "Did you know that the Answer to the Ultimate Question of Life, the Universe, and Everything is 42? Did you?";

  public static final String ULTIMATE_TRUTH = 
      "Did you know that the Answer to the Ultimate Question of Life, the Universe, and Everything is 42? Did you?";

  public static void main( String[] args) {

    SocketChannel channel = null;
  
    if ( args.length == 2 ) {

      try {
 
        System.out.print( "connecting to " + args[0] + ":" + args[1] );

        String host = args[0];
        Integer port = Integer.parseInt( args[1] );
    
        channel = SocketChannel.open();
        channel.connect( new InetSocketAddress( host, port ) );
        System.out.println( "    ...[CONNECTED]" );
    
        // channel.configureBlocking( false );
        Long count = 0L;
        ByteBuffer binaryTruth = ByteBuffer.wrap( ULTIMATE_TRUTH.getBytes() );
        // binaryTruth.order( ByteOrder.LITTLE_ENDIAN );

        System.out.println( "streaming away " + binaryTruth.capacity() + " byte things");
        
        while ( true ) {

          channel.write( binaryTruth );
          binaryTruth.flip();

          count++;
          if ( count % 100000 == 0 ) {
            System.out.println( "sent " + count + " ultimate truths" );
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
      System.out.println( "usage: java Streamer host port" );
    }
  }
}
