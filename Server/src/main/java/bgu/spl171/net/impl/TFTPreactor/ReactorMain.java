package bgu.spl171.net.impl.TFTPreactor;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.api.MessagesTypes.TFTPMsg;
import bgu.spl171.net.api.TftpEncoderDecoder;
import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.TftpProtocol;
import bgu.spl171.net.srv.Server;

import java.util.function.Supplier;

/**
 * Created by Naor on 19/01/2017.
 */
public class ReactorMain {

        public static void main(String[] args) {
            int port = Integer.parseInt(args[0]);
            Supplier<BidiMessagingProtocol<TFTPMsg>> protocolSupplier = TftpProtocol::new;
            Supplier<MessageEncoderDecoder<TFTPMsg>> encoderDecoderSupplier = TftpEncoderDecoder::new;
            Server.reactor(Runtime.getRuntime().availableProcessors(), port, protocolSupplier, encoderDecoderSupplier).serve();
        }
}
