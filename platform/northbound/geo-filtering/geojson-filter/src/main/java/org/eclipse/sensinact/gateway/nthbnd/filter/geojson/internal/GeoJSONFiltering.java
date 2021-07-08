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
package org.eclipse.sensinact.gateway.nthbnd.filter.geojson.internal;

import org.eclipse.sensinact.gateway.common.bundle.Mediator;
import org.eclipse.sensinact.gateway.core.Filtering;
import org.eclipse.sensinact.gateway.core.LocationResource;
import org.eclipse.sensinact.gateway.core.Resource;
import org.eclipse.sensinact.gateway.util.LocationUtils;
import org.eclipse.sensinact.gateway.util.json.JSONObjectStatement;
import org.eclipse.sensinact.gateway.util.json.JSONTokenerStatement;
import org.eclipse.sensinact.gateway.util.json.JSONValidator;
import org.eclipse.sensinact.gateway.util.json.JSONValidator.JSONToken;
import org.eclipse.sensinact.gateway.util.location.Segment;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link Filtering} implementation allowing to apply a location discrimination
 * to the result object to be filtered
 *
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 */
public class GeoJSONFiltering implements Filtering {
    //********************************************************************//
    //						NESTED DECLARATIONS			  			      //
    //********************************************************************//
    //********************************************************************//
    //						ABSTRACT DECLARATIONS						  //
    //********************************************************************//
    //********************************************************************//
    //						STATIC DECLARATIONS							  //
    //********************************************************************//
    private static final JSONObjectStatement STATEMENT = 
    		new JSONObjectStatement(new JSONTokenerStatement(
			    "{" + 
			    " \"type\": \"Feature\"," + 
			    " \"properties\": {" + 
			    "	    \"name\": $(name)" + 
			    "  }," + 
			    "  \"geometry\": {" + 
			    "     \"type\": \"Point\"," + 
			    "     \"coordinates\": [ $(longitude),$(latitude)] " + 
			    "  }" + 
			    "}"));

    //********************************************************************//
    //						INSTANCE DECLARATIONS						  //
    //********************************************************************//
    private Mediator mediator;

    /**
     * Constructor
     *
     * @param mediator the {@link Mediator} allowing the
     *                 GeoJSONFiltering to be instantiated to interact with
     *                 the OSGi host environment
     */
    public GeoJSONFiltering(Mediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public boolean handle(String type) {
        return "geojson".equals(type);
    }

    @Override
    public String apply(String definition, Object result) {
        JSONObject obj = new JSONObject(definition);
        boolean output = obj.optBoolean("output");
        if (!output) {
            return String.valueOf(result);
        }

        JSONValidator validator = new JSONValidator(String.valueOf(result));
        StringBuilder builder = new StringBuilder();
        builder.append("{\"type\": \"FeatureCollection\", \"features\": [");

        int count = 0;
        int index = 0;

        Set<String> names = new HashSet<String>();
        Map<Integer, String> locationMap = new HashMap<Integer, String>();
        Map<Integer, String> nameMap = new HashMap<Integer, String>();

        while (true) {
            JSONToken token = validator.nextToken();
            if (token == null) {
                break;
            }
            if (token.ordinal() == JSONToken.JSON_OBJECT_OPENING.ordinal()) {
                count++;
            }
            if (token.ordinal() == JSONToken.JSON_OBJECT_CLOSING.ordinal()) {
                Integer ind = new Integer(count);
                nameMap.remove(ind);
                locationMap.remove(ind);
                count--;
            }
            if (token.ordinal() == JSONToken.JSON_OBJECT_ITEM.ordinal() && token.getContext().key.equals(Resource.NAME)) {
                Integer ind = new Integer(count);
                String name = (String) token.getContext().value;
                if (name != null) {
                    nameMap.put(ind, name);
                }
                String location = null;
                if ((location = locationMap.get(ind)) != null && !names.contains(name) && writeLocation(name, location, index, builder)) {
                    index++;
                }
            }
            if (token.ordinal() == JSONToken.JSON_OBJECT_ITEM.ordinal() && token.getContext().key.equals(LocationResource.LOCATION)) {
                Integer ind = new Integer(count);
                String location = (String) token.getContext().value;
                if (location != null) {
                    locationMap.put(ind, location);
                }
                String name = null;
                if ((name = nameMap.get(ind)) != null && !names.contains(name) && writeLocation(name, location, index, builder)) {
                    index++;
                }
            }
        }
        builder.append("]}");
        return builder.toString();
    }

    boolean writeLocation(String name, String location, int index, StringBuilder builder) {
        try {
        	char[] seps = new char[] {':',',',' '};
        	int pos = 0;
        	for(;pos < seps.length; pos++) {
        		int sep = location.indexOf(':');
        		if(sep >= 0)
        			break;
        	}
        	if(pos == seps.length)
        		return false;
        	
        	char sep = seps[pos];        	
            String[] locationElements = location.split(new String(new char[] {sep}));
            double latitude = Double.parseDouble(locationElements[0]);
            double longitude = Double.parseDouble(locationElements[1]);
            STATEMENT.apply("latitude", latitude);
            STATEMENT.apply("longitude", longitude);
            STATEMENT.apply("name", name);
            if (index > 0) 
                builder.append(",");
            
            builder.append(STATEMENT.toString());
            return true;

        } catch (Exception e) {
            mediator.error(e);

        } finally {
            STATEMENT.reset();
        }
        return false;
    }

    @Override
    public String getLDAPComponent(String definition) {
        String ldapFilter = null;
        try {
            JSONObject obj = new JSONObject(definition);
            double lat = obj.getDouble("latitude");
            double lng = obj.getDouble("longitude");
            double distance = obj.getDouble("distance");

            Segment rad0 = null;
            Segment rad90 = null;
            int bearing0 =  Double.valueOf(LocationUtils.fromReverseClockedDegreesAngleToNorthOrientedBearing(0)).intValue();
            int bearing90 =  Double.valueOf(LocationUtils.fromReverseClockedDegreesAngleToNorthOrientedBearing(90)).intValue();
            if (distance < 200) {
                rad0 = LocationUtils.getSphericalEarthModelCoordinates(lat, lng, bearing0	, distance);
                rad90 = LocationUtils.getSphericalEarthModelCoordinates(lat, lng, bearing90, distance);
            } else {
                rad0 = LocationUtils.getElipsoidEarthModelCoordinates(lat, lng, bearing0,distance);
                rad90 = LocationUtils.getElipsoidEarthModelCoordinates(lat, lng, bearing90, distance);
            }
            double diffLat = Math.abs((rad0.getLat1() - rad0.getLat2()));
            double diffLng = Math.abs((rad90.getLng1() - rad90.getLng2()));

            ldapFilter = String.format("(&(latitude <= %s)(latitude >= %s)(longitude <= %s)(longitude >= %s))",
            		(lat + diffLat), (lat - diffLat), (lng + diffLng), (lng - diffLng));
            
        } catch (Exception e) {
        	e.printStackTrace();
            return null;
        }
        return ldapFilter;
    }
}
