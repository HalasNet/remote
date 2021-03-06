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
package org.universAAL.ri.rest.manager.wrappers;

import java.util.Hashtable;

import org.universAAL.middleware.container.ModuleContext;
import org.universAAL.middleware.service.CallStatus;
import org.universAAL.middleware.service.ServiceCall;
import org.universAAL.middleware.service.ServiceCallee;
import org.universAAL.middleware.service.ServiceResponse;
import org.universAAL.middleware.service.owls.profile.ServiceProfile;
import org.universAAL.ri.rest.manager.push.PushManager;
import org.universAAL.ri.rest.manager.resources.Callee;

public class CalleeWrapper extends ServiceCallee{
    
    public static final String PROP_ORIGIN_CALL = "http://ontology.universAAL.org/uAAL.owl#originCall";

    public static Hashtable<String,ServiceResponse> pendingCalls=new Hashtable<String,ServiceResponse>();
    
    private Callee resource;
    private String tenant;

    public Callee getResource() {
        return resource;
    }

    public void setResource(Callee resource) {
        this.resource = resource;
    }

    public CalleeWrapper(ModuleContext context,
	    ServiceProfile[] realizedServices, Callee r, String t) {
	super(context, realizedServices);
	resource=r;
	tenant=t;
    }

    @Override
    public void communicationChannelBroken() {
	// TODO Auto-generated method stub
	
    }

    @Override
    public ServiceResponse handleCall(ServiceCall call) {
	try {
	    ServiceResponse srlock = new ServiceResponse(CallStatus.responseTimedOut);
	    pendingCalls.put(call.getURI(), srlock);
	    
	    String callback=resource.getCallback();
	    if(callback==null || callback.isEmpty()){
		//Use generic callback of the tenant
		SpaceWrapper t = UaalWrapper.getInstance().getTenant(tenant);
		if(t!=null){
		    callback=t.getResource().getCallback();
		    if(callback==null){
			return new ServiceResponse(CallStatus.noMatchingServiceFound);
		    }
		}
	    }

	    PushManager.pushServiceCall(callback,resource.getId(), call);
	    
	    synchronized (srlock) {
		srlock.wait(30000);
		return pendingCalls.remove(call.getURI());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    pendingCalls.remove(call.getURI());
	    return new ServiceResponse(CallStatus.serviceSpecificFailure);
	}
//	return new ServiceResponse(CallStatus.succeeded);
    }

    public void handleResponse(ServiceResponse newresponse) {
	String originalcall = (String) newresponse.getProperty(PROP_ORIGIN_CALL);
	if (originalcall != null) {
	    ServiceResponse originalresponse = pendingCalls.get(originalcall);
	    if (originalresponse != null) {
		synchronized (originalresponse) {
		    pendingCalls.put(originalcall, newresponse);
		    originalresponse.notify();
		}
	    }
	}
    }

}
