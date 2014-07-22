/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.opendj.ldap.spi;

import java.net.InetSocketAddress;

import org.forgerock.opendj.ldap.ConnectionFactory;

/**
 * Interface for all classes that actually implement
 * {@code LDAPConnectionFactory}.
 * <p>
 * An implementation class is provided by a {@code TransportProvider}.
 * <p>
 * The implementation can be automatically loaded using the
 * {@code java.util.ServiceLoader} facility if its provider extending
 * {@code TransportProvider} is declared in the provider-configuration file
 * {@code META-INF/services/org.forgerock.opendj.ldap.spi.TransportProvider}.
 */
public interface LDAPConnectionFactoryImpl extends ConnectionFactory {

    /**
     * Returns the address used by the connections created by this factory.
     *
     * @return The address used by the connections.
     */
    public InetSocketAddress getSocketAddress();

    /**
     * Returns the hostname used by the connections created by this factory.
     *
     * @return The hostname used by the connections.
     */
    public String getHostName();

    /**
     * Returns the remote port number used by the connections created by this
     * factory.
     *
     * @return The remote port number used by the connections.
     */
    public int getPort();

}