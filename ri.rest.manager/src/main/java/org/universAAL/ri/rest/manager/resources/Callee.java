/*
	Copyright 2015 ITACA-SABIEN, http://www.tsb.upv.es
	Instituto Tecnologico de Aplicaciones de Comunicacion 
	Avanzadas - Grupo Tecnologias para la Salud y el 
	Bienestar (SABIEN)
	
	See the NOTICE file distributed with this work for additional 
	information regarding copyright ownership
	
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	  http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */
package org.universAAL.ri.rest.manager.resources;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.JaxbAdapter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.universAAL.middleware.service.ServiceResponse;
import org.universAAL.middleware.service.owls.profile.ServiceProfile;
import org.universAAL.ri.rest.manager.Activator;
import org.universAAL.ri.rest.manager.wrappers.CalleeWrapper;
import org.universAAL.ri.rest.manager.wrappers.UaalWrapper;
import org.universAAL.ri.rest.manager.wrappers.SpaceWrapper;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
@Path("uaal/spaces/{id}/service/callees/{subid}")
public class Callee {

    @XmlAttribute
    @PathParam("subid")
    private String id;

    @XmlElement(name = "link")
    @XmlJavaTypeAdapter(JaxbAdapter.class)
    private Link self;
    
    @XmlElement(name = "callback")
    private String callback;
    
    @XmlElement(name = "profile")
    private String profile;

    public Link getSelf() {
	return self;
    }

    public void setSelf(Link self) {
	this.self = self;
    }

    public String getId() {
	return id;
    }

    public void setId(String id) {
	this.id = id;
    }
    
    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public Callee(String id, String subid, String callback, String serial) {
	setId(subid);
	setSelf(Link.fromPath("/uaal/spaces/"+id+"/service/callees/"+subid).rel("self").build());
	setCallback(callback);
	setProfile(serial);
    }

    public Callee() {
	
    }
    
    //===============REST METHODS===============
    
    @GET
    @Produces(Activator.TYPES)
    public Callee getCalleeResource(@PathParam("id") String id, @PathParam("subid") String subid){
	SpaceWrapper tenant = UaalWrapper.getInstance().getTenant(id);
	if(tenant!=null){
	    CalleeWrapper wrapper = tenant.getServiceCallee(subid);
	    if(wrapper!=null){
		return wrapper.getResource();
	    }
	}
	return null;
    }
    
    @DELETE
    public Response deleteCalleeResource(@PathParam("id") String id, @PathParam("subid") String subid){
	SpaceWrapper tenant = UaalWrapper.getInstance().getTenant(id);
	if(tenant!=null){
	    tenant.removeServiceCallee(subid);
	    Activator.getPersistence().removeCallee(id, subid);
	    return Response.ok().build();//.nocontent?
	}
	return Response.status(Status.NOT_FOUND).build();
    }
    
    @POST
    @Consumes(Activator.TYPES_TXT)
    public Response executeCalleeResponse(@PathParam("id") String id, @PathParam("subid") String subid, String sresp){
	SpaceWrapper tenant = UaalWrapper.getInstance().getTenant(id);
	if(tenant!=null){
	    CalleeWrapper ceewrap = tenant.getServiceCallee(subid);
	    if(ceewrap!=null){
		ServiceResponse sr=(ServiceResponse) Activator.getParser().deserialize(sresp);
		if(sr!=null){
		    ceewrap.handleResponse(sr);
		    return Response.ok().build();
		}else{
		    return Response.status(Status.BAD_REQUEST).build();
		}
	    }else{
		return Response.status(Status.NOT_FOUND).build();
	    }
	}else{
	    return Response.status(Status.NOT_FOUND).build();
	}
    }
    
    @PUT
    @Consumes(Activator.TYPES)
    public Response putCalleeResource(@PathParam("id") String id, @PathParam("subid") String subid, Callee cee) throws URISyntaxException{
	//The cee generated from the PUT body does not contain any "link" elements, but I wouldnt have allowed it anyway
	if (subid.equals(cee.id)) {// Do not allow changes to id
	    SpaceWrapper tenant = UaalWrapper.getInstance().getTenant(id);
	    if (tenant != null) {
		if(Activator.getParser()!=null){
		    if (cee.getProfile() != null) {
			ServiceProfile sp = (ServiceProfile) Activator
				.getParser().deserialize(cee.getProfile());
			if (sp != null) { //Just check that they are OK
			    CalleeWrapper original = tenant.getServiceCallee(subid);
			    if (original != null) {//Can only change existing ones
				cee.setSelf(Link.fromPath("/uaal/spaces/"+id+"/service/callees/"+cee.getId()).rel("self").build());
				original.setResource(cee);
				if(tenant.updateServiceCallee(original)){
				    Activator.getPersistence().storeCallee(id, cee);
				    return Response.created(new URI("uaal/spaces/"+id+"/service/callees/"+cee.getId())).build();
				}else{
				    return Response.notModified().build();
				}
			    } else {
				return Response.status(Status.NOT_FOUND).build();
			    }
			} else {
			    return Response.status(Status.BAD_REQUEST).build();
			}
		    } else {
			return Response.status(Status.BAD_REQUEST).build();
		    }
		}else{
		    return Response.serverError().build();
		}
	    } else {
		return Response.status(Status.NOT_FOUND).build();
	    }
	} else {
	    return Response.notModified().build();
	}
    }
    
}
