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
package org.eclipse.sensinact.northbound.query.dto.result;

import java.util.List;

import org.eclipse.sensinact.northbound.query.api.AbstractResultDTO;
import org.eclipse.sensinact.northbound.query.api.EResultType;

/**
 * Description of all resources of all services of all providers
 */
public class ResultCompleteListDTO extends AbstractResultDTO {

    /**
     * List of providers IDs
     */
    // TODO: should be a "full" description of providers
    public List<String> providers;

    /**
     * Sets the result type
     */
    public ResultCompleteListDTO() {
        super(EResultType.COMPLETE_LIST);
    }
}
