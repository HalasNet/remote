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
import java.util.ArrayList;
import java.util.Enumeration;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.JaxbAdapter;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.universAAL.ri.rest.manager.Activator;
import org.universAAL.ri.rest.manager.wrappers.UaalWrapper;
import org.universAAL.ri.rest.manager.wrappers.SpaceWrapper;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "spaces")
@Path("/uaal/spaces")
public class Spaces {
    
    @XmlElement(name = "link")
    @XmlJavaTypeAdapter(JaxbAdapter.class)
    private Link self=Link.fromPath("/uaal/spaces").rel("self").build();	// Link to self
//    private Link self=Link.fromUriBuilder(UriBuilder.fromPath("/uaal/spaces").host("http://localhost:9000")).rel("self").build();

    @XmlElement(name = "space") // @XmlElementRef?
    private ArrayList<Space> spaces;						// List of spaces

    public ArrayList<Space> getSpaces() {
	return spaces;
    }

    public void setSpaces(ArrayList<Space> spaces) {
	this.spaces = spaces;
    }
    
    public Spaces(){

    }
    
    //===============REST METHODS===============
    
    @GET		// GET localhost:9000/uaal/spaces      (redirected from Uaal class)
    @Produces(Activator.TYPES)
    public Spaces getSpacesResource(){
	Spaces allspaces=new Spaces();
        ArrayList<Space> spaces = new ArrayList<Space>();
        
        Enumeration<SpaceWrapper> tenants = UaalWrapper.getInstance().getTenants();
        while (tenants.hasMoreElements()){
            spaces.add(tenants.nextElement().getResource());
        }
        
        /*if(Activator.tenantMngr!=null){
            Map<String, String> tenants = Activator.tenantMngr.getTenants();
            Set<String> tenantNames = tenants.keySet();
            for(String tenantName:tenantNames){
        	spaces.add(new Space(tenantName));
            }
        }*/
	
	allspaces.setSpaces(spaces);
	return allspaces;
    }
    
    @POST	// POST localhost:9000/uaal/spaces      <Body: Space>
    @Consumes(Activator.TYPES)
    public Response addSpaceResource(Space space) throws URISyntaxException{
	//The space generated from the POST body does not contain any "link" elements, but I wouldnt have allowed it anyway
	//Set the links manually, like in the space constructor
	space.setSelf(Link.fromPath("/uaal/spaces/"+space.getId()).rel("self").build());
	space.setContext(Link.fromPath("/uaal/spaces/"+space.getId()+"/context").rel("context").build());
	space.setService(Link.fromPath("/uaal/spaces/"+space.getId()+"/service").rel("service").build());
	UaalWrapper.getInstance().addTenant(new SpaceWrapper(space));
	if(Activator.getTenantMngr()!=null){
	    Activator.getTenantMngr().registerTenant(space.getId(), "SpaceWrapper created through REST interface");
	}
	Activator.getPersistence().storeSpace(space, (String)null);
	return Response.created(new URI("uaal/spaces/"+space.getId())).build();
    }
    
    @Path("/{id}")		// GET localhost:9000/uaal/spaces/123     (Redirects to Space class)
    @Produces(Activator.TYPES)
    public Space getSpaceResourceLocator(){
	return new Space();
    }

}
