/*********************************************************************
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   Kentyou - initial implementation
**********************************************************************/
package org.eclipse.sensinact.northbound.filters.sensorthings.antlr.impl.paths;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.sensinact.northbound.filters.sensorthings.antlr.impl.UnsupportedRuleException;
import org.eclipse.sensinact.prototype.snapshot.ProviderSnapshot;
import org.eclipse.sensinact.prototype.snapshot.ResourceSnapshot;

/**
 * @author thoma
 *
 */
public class ObservationPathHandler {

    private final ProviderSnapshot provider;
    private final ResourceSnapshot resource;

    private final Map<String, Function<String[], Object>> subPartHandlers = Map.of("datastream", this::subDatastream,
            "featureofinterest", this::subFeatureOfInterest);

    public ObservationPathHandler(final ProviderSnapshot provider, final ResourceSnapshot resource) {
        this.provider = provider;
        this.resource = resource;
    }

    public Object handle(final String path) {
        final String[] parts = path.toLowerCase().split("/");
        if (parts.length == 1) {
            switch (path) {
            case "id":
                // Provider~Service~Resource~Timestamp
                return String.join("~", provider.getName(), resource.getService().getName(), resource.getName(),
                        PathUtils.timestampToString(resource.getValue().getTimestamp()));

            default:
                return PathUtils.getResourceLevelField(provider, resource, path);
            }
        } else {
            final Function<String[], Object> handler = subPartHandlers.get(parts[0]);
            if (handler == null) {
                throw new UnsupportedRuleException("Unsupported path: " + path);
            }
            return handler.apply(Arrays.copyOfRange(parts, 1, parts.length));
        }
    }

    private Object subDatastream(final String[] parts) {
        if (parts.length == 1) {
            switch (parts[0]) {
            case "id":
                // Provider~Service~Resource
                return String.join("~", provider.getName(), resource.getService().getName(), resource.getName());

            default:
                return PathUtils.getResourceLevelField(provider, resource, parts[0]);
            }
        } else {
            throw new UnsupportedRuleException(
                    "Unexpected field in Observation Datastream: " + String.join("/", parts));
        }
    }

    private Object subFeatureOfInterest(final String[] parts) {
        if (parts.length == 1) {
            switch (parts[0]) {
            case "id":
                // Provider
                return provider.getName();

            default:
                return PathUtils.getResourceLevelField(provider, resource, parts[0]);
            }
        } else {
            throw new UnsupportedRuleException(
                    "Unexpected field in Observation Datastream: " + String.join("/", parts));
        }
    }
}
