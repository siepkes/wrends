/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2012-2016 ForgeRock AS.
 */

/**
 * APIs for implementing REST to LDAP gateways. The API is implemented by
 * {@link org.forgerock.opendj.rest2ldap.SubResourceImpl} which is using a pre-established
 * {@link org.forgerock.opendj.ldap.Connection} encapsulated in the
 * {@link org.forgerock.opendj.rest2ldap.AuthenticatedConnectionContext}. This context is injected by the
 * {@link org.forgerock.opendj.rest2ldap.authz.ProxiedAuthV2Filter} depending on the
 * {@link org.forgerock.services.context.SecurityContext} injected by one of the configured authorization filters.
 */
package org.forgerock.opendj.rest2ldap;

