/*
 * Copyright (c) 2017 CEA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    CEA - initial API and implementation
 */
package org.eclipse.sensinact.gateway.remote.socket;

import org.eclipse.sensinact.gateway.common.bundle.Mediator;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * The ClientSocketThread is in charge of sending requests to the remote
 * connected SocketEndpoint's server
 *
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 * @author <a href="mailto:stephane.bergeon@cea.fr">Stéphane Bergeon</a>
 */
class ClientSocketThread implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ClientSocketThread.class);
    public Long TIMEOUT_DELAY;  //5s timeout
    public static final long REQUEST_PERIOD = 100;  //try requesting every 100ms
    private static final String UUID_PREFIX = "edpnt";
    private static final String UUID_KEY = "uuid";
    private boolean running;
    private SocketHolder holder = null;
    private final Map<String, String> requests;
    protected Mediator mediator;
    private final InetAddress remoteAddress;
    private final int remotePort;
    private final SocketEndpoint endpoint;

    ClientSocketThread(Mediator mediator, SocketEndpoint endpoint, String address, int port) throws IOException {

        LOG.debug("Instantiating ClientSocketThread for {} on local {}:{} and remote {}:{}",endpoint.getIdentifier(),endpoint.getLocalAddress(),endpoint.getLocalPort(),endpoint.getRemoteAddress(),endpoint.getRemotePort());

        this.mediator = mediator;
        this.endpoint = endpoint;
        this.requests = new HashMap<String, String>();
        this.remoteAddress = InetAddress.getByName(address);
        this.remotePort = port;
        this.TIMEOUT_DELAY=Long.parseLong(System.getProperty("clientSocketThreadTimeout","400"));
    }

    protected void stop() {
        this.running = false;
        this.close();
    }

    protected boolean running() {
        return this.running;
    }

    private boolean checkStatus() {
        return this.holder != null && this.holder.checkSocketStatus();
    }

    private void close() {
        if (this.holder != null) {
            this.holder.close();
        }
        this.requests.clear();
    }

    protected String request(JSONObject object) {
        String result = null;
        if (!running) {
            return result;
        }
        final String uuid = generateUUID();
        object.put(UUID_KEY, uuid);
        try {
            this.holder.write(object);
        } catch (IOException e) {
            this.mediator.error(e);
            if (!this.holder.checkSocketStatus()) {
                return result;
            }
        }
        long wait = TIMEOUT_DELAY;
        while (wait > 0) {
            synchronized (this.requests) {
                if (this.requests.containsKey(uuid)) {
                    result = this.requests.remove(uuid);
                    break;
                }
            }
            try {
                wait -= REQUEST_PERIOD;
                Thread.sleep(REQUEST_PERIOD);
            } catch (InterruptedException e) {
                Thread.interrupted();
                break;
            }
        }
        return result;
    }



    @Override
    public void run() {
        try {
            Socket s = new Socket();
            s.setReuseAddress(true);
            s.setSoTimeout(0);      
            s.setKeepAlive(true);
            
            s.connect(new InetSocketAddress(remoteAddress, remotePort));
            this.holder = new SocketHolder(mediator, s);
        } catch (IOException e) {
            LOG.error("Failed to bind to address ID {} address {}:{}",endpoint.getIdentifier(),remoteAddress,remotePort,e);
        } finally {
            if (checkStatus()) {
                this.running = true;
            }
        }
        while (running) {
            JSONObject object = null;
            try {
                object = this.holder.read();
                if (!JSONObject.NULL.equals(object)) {
                    final String uuid = (String) object.remove(UUID_KEY);
                    synchronized (this.requests) {
                        if (uuid != null) {
                            final long requestTime = getRequestTime(uuid);
                            final long now = System.currentTimeMillis();
                            final long delay = now - requestTime;
                            LOG.debug("Response delay is {} (timeout is configured to {}) on remote instance {}:{}",delay,TIMEOUT_DELAY,remoteAddress,remotePort);
                            if (delay < TIMEOUT_DELAY) {    //avoid to put requests which response are out of delay because will never be removed from map
                                this.requests.put(uuid, object.optString("response"));
                            } else {
                                LOG.debug("Timeout for response. Delay is {} (which larger than the timeout configured {}) for remote instance {}:{}",delay,TIMEOUT_DELAY,remoteAddress,remotePort);
                                //mediator.warn("void out of delay response for request " + uuid);
                            }
                        }
                    }
                }
            } catch (SocketException e) {
                LOG.error("Socket exception on remote ID {} address {}:{}",endpoint.getIdentifier(),remoteAddress,remotePort,e);
                break;
            } catch (IOException | JSONException e) {
                LOG.error("Socket exception on remote ID {} address {}:{}",endpoint.getIdentifier(),remoteAddress,remotePort,e);
            } finally {
                if (!this.checkStatus()) {
                    break;
                }
            }
        }
        this.close();
        if (running) {
            this.running = false;
            this.endpoint.clientDisconnected();
        }
    }

    /**
     * generate uuid
     *
     * @return the generated uuid from current timestamp and hashcode
     */
    private String generateUUID() {
        final long timestamp = System.currentTimeMillis();
        final String uuid = generateUUID(timestamp);
        return uuid;
    }

    /**
     * generate uuid for a given time stamp
     *
     * @param timestamp
     * @return the generated uuid from givn timestamp (for test purpose)
     */
    protected final String generateUUID(final long timestamp) {
        final long bluredTimestamp = timestamp + this.hashCode();
        final String uuid = new StringBuilder().append(UUID_PREFIX).append(bluredTimestamp).toString();
        return uuid;
    }

    /**
     * retrieve the time stamp from a given uuid
     *
     * @param uuid
     * @return the time stamp for the given uuid
     */
    protected final long getRequestTime(String uuid) {
        final String requestTime$ = uuid.substring(UUID_PREFIX.length());
        final long requestTime = Long.valueOf(requestTime$) - this.hashCode();
        return requestTime;
    }
}
