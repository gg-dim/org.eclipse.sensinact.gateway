/*
* Copyright (c) 2020 Kentyou.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
*    Kentyou - initial API and implementation
 */
package org.eclipse.sensinact.gateway.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.sensinact.gateway.common.primitive.InvalidValueException;
import org.eclipse.sensinact.gateway.common.primitive.Modifiable;
import org.eclipse.sensinact.gateway.util.UriUtils;

/**
 * Linked {@link Resource} implementation
 * 
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 */
public class LinkedResourceImpl extends ResourceImpl {
	protected final ResourceImpl targetedResource;

	/**
	 * Constructor
	 * 
	 * @param modelInstance
	 * @param resourceConfig
	 * @param targetedResource
	 * @param service
	 * @throws InvalidResourceException
	 */
	public LinkedResourceImpl(ModelInstance<?> modelInstance, ResourceConfig resourceConfig,
			ResourceImpl targetedResource, ServiceImpl service) throws InvalidResourceException {
		super(modelInstance, resourceConfig, service);
		this.targetedResource = targetedResource;

	}

	@Override
	public void buildAttributes(ResourceConfig resourceConfig) {
		String defaultAttributeName = resourceConfig.getTypeConfig()
				.<String>getConstantValue(Resource.ATTRIBUTE_DEFAULT_PROPERTY, false);

		super.setDefault(defaultAttributeName);
		try {
			super.elements.add(new Attribute(super.modelInstance.mediator(), this, Resource.NAME, String.class,
					UriUtils.getLeaf(super.uri), Modifiable.FIXED, true));

		} catch (InvalidValueException e) {
			super.modelInstance.mediator().error(e, e.getMessage());
		}
	}

	@Override
	public Attribute getAttribute(String name) {
		Attribute attribute = null;
		if ((attribute = super.getAttribute(name)) == null) {
			attribute = this.targetedResource.getAttribute(name);
		}
		return attribute;
	}

	@Override
	public AttributeDescription getDescription(String name) {
		AttributeDescription description = null;
		Attribute primitive = getAttribute(name);
		if (primitive != null) {
			description = (AttributeDescription) primitive.getDescription();

		} else {
			description = this.targetedResource.getDescription(name);
		}
		return description;
	}

	@Override
	public List<AttributeDescription> getAllDescriptions() {
		List<AttributeDescription> descriptions = super.getAllDescriptions();

		if (this.targetedResource == null) {
			return descriptions;
		}
		List<AttributeDescription> linkedDescriptions = this.targetedResource.getAllDescriptions();

		List<AttributeDescription> allDescriptions = new ArrayList<AttributeDescription>();

		allDescriptions.addAll(descriptions);
		allDescriptions.addAll(linkedDescriptions);
		return allDescriptions;
	}

	@Override
	public Resource.Type getType() {
		return this.targetedResource.getType();
	}

	@Override
	public void registerLink(String path) {
		super.modelInstance.mediator().debug("Unable to register a link for a LinkedResourceImpl");
	}
}
