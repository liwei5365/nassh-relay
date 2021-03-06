/*
 * nassh-relay - Relay Server for tunneling ssh through a http endpoint
 * 
 * Website: http://relay.wsn.at
 *
 * Copyright 2014   zyclonite    networx
 *                  http://zyclonite.net
 * Developer: Lukas Prettenthaler
 */
package net.zyclonite.nassh.relay.util;

import java.util.Observable;
import java.util.Observer;
import net.zyclonite.nassh.relay.model.Session;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;

/**
 *
 * @author zyclonite
 */
public class TransferObserver implements Observer {

    private static final Log LOG = LogFactory.getLog(TransferObserver.class);
    private Object request = null;
    private Session session = null;

    public TransferObserver(final Session session, final Object request) {
        this.request = request;
        this.session = session;
    }

    @Override
    public void update(final Observable queue, final Object arg) {
        if (request instanceof HttpServerRequest) {
            final Buffer buffer = ((TransferQueue) queue).poll();
            queue.deleteObserver(this);
            final HttpServerRequest req = (HttpServerRequest)request;
            final String encodedBytes = Base64.encodeBase64URLSafeString(buffer.getBytes());
            req.response().setStatusCode(200);
            req.response().end(encodedBytes);
        }else if (request instanceof ServerWebSocket) {
            final Buffer buffer = ((TransferQueue) queue).poll();
            final ServerWebSocket ws = (ServerWebSocket)request;
            if (!ws.writeQueueFull()) {
                final Buffer ackbuffer = new Buffer();
                ackbuffer.setInt(0, session.getWrite_count());
                ackbuffer.setBuffer(4, buffer);
                ws.write(ackbuffer);
            }else{
                ws.pause();
            }
        }else{
            queue.deleteObserver(this);
        }
    }
}
