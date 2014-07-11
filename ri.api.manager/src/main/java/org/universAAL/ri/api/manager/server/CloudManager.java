package org.universaal.ri.api.manager.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import org.universAAL.middleware.context.ContextEvent;
import org.universAAL.middleware.rdf.Resource;
import org.universAAL.middleware.rdf.TypeMapper;
import org.universAAL.middleware.service.CallStatus;
import org.universAAL.middleware.service.ServiceCall;
import org.universAAL.middleware.service.ServiceResponse;
import org.universAAL.middleware.service.owls.process.OutputBinding;
import org.universAAL.middleware.service.owls.process.ProcessOutput;
import org.universaal.ri.api.manager.Activator;
import org.universaal.ri.api.manager.RemoteAPI;

/**
 * Class that manages the push of callbacks to client remote node endpoints.
 * Currently it only does this for endpoints with HTTP servers accessible.
 * Android endpoints will require interfacing with Google Cloud Messaging
 * servers.
 * 
 * @author alfiva
 * 
 */
public class CloudManager {

    /**
     * Build a Context Event callbcak message and send it to the client remote
     * node endpoint.
     * 
     * @param remoteid
     *            The client remote node endpoint
     * @param event
     *            The serialized Context Event to send
     */
    public static void sendC(String remoteid, ContextEvent event) {
	StringBuilder strb = new StringBuilder();
	strb.append(RemoteAPI.KEY_METHOD).append("=").append(RemoteAPI.METHOD_SENDC)
		.append("&").append(RemoteAPI.KEY_PARAM).append("=").append(Activator.parser.serialize(event))
		.append("&").append(ContextEvent.PROP_RDF_SUBJECT).append("=").append(event.getSubjectURI())
		.append("&").append(ContextEvent.PROP_RDF_PREDICATE).append("=").append(event.getRDFPredicate())
		.append("&").append(ContextEvent.PROP_RDF_OBJECT).append("=").append(event.getRDFObject().toString());
	Activator.logI("CloudManager.sendC", "Sending message to remote node > SENDC, body: "+strb.toString());//TODO Log body?
	try {
	    sendPOST(remoteid, strb.toString());
	} catch (MalformedURLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    /**
     * Build a ServiceCall callback message and send it to the client remote
     * node endpoint.
     * 
     * @param remoteid
     *            The client remote node endpoint
     * @param call
     *            The serialized Service Call to send
     * @return The Service Response that the client remote node will have sent
     *         as response to the callback
     */
    public static ServiceResponse callS(String remoteid, ServiceCall call) {
	ServiceResponse sr = new ServiceResponse(CallStatus.serviceSpecificFailure);
	StringBuilder strb = new StringBuilder();
	strb.append(RemoteAPI.KEY_METHOD).append("=").append(RemoteAPI.METHOD_CALLS)
		.append("&").append(RemoteAPI.KEY_PARAM).append("=").append(Activator.parser.serialize(call));
	List inputs = (List) call.getProperty(ServiceCall.PROP_OWLS_PERFORM_HAS_DATA_FROM);
	if (inputs != null) {
	    for (Iterator i = inputs.iterator(); i.hasNext();) {
		Resource binding = (Resource) i.next(), in = (Resource) binding
			.getProperty(OutputBinding.PROP_OWLS_BINDING_TO_PARAM);
		if (in != null) {
		    strb.append("&").append(in.getURI()).append("=")
			    .append(call.getInputValue(in.getURI()));
		}
	    }
	}
	Activator.logI("CloudManager.callS", "Sending message to remote node > CALLS, body:  " + strb.toString());//TODO Logbody?
	try {
	    String response = sendPOST(remoteid, strb.toString());
	    InputStreamReader ir = new InputStreamReader(
		    new ByteArrayInputStream(response.getBytes()));
	    BufferedReader br = new BufferedReader(ir);
	    String line;
	    line = br.readLine();
	    while (line != null && !line.equals(RemoteAPI.FLAG_TURTLE)) {
		String[] parts = line.split("=", 2);
		if (parts.length == 2) {
		    if (!parts[0].equals(RemoteAPI.KEY_STATUS)) { //If status, we already handle with the serialized
			String[] resource = parts[1].split("@", 2);// TODO arrayindexerror
			if(resource[1].startsWith("http://www.w3.org/2001/XMLSchema")){
			    sr.addOutput(new ProcessOutput(parts[0],TypeMapper.getJavaInstance(resource[0], resource[1])));
			}else{
			    sr.addOutput(new ProcessOutput(parts[0], Resource.getResource(resource[1], resource[0])));
			}
			
		    }
		}
		line = br.readLine();
	    }
	    strb = new StringBuilder();
	    while (line != null) {
		// We only get here if there was something after TURTLE (and there was TURTLE)
		line = br.readLine();
		if (line != null)
		    strb.append(line);
	    }
	    br.close();
	    String serialized = strb.toString();
	    if (serialized.length() > 1) {
		Object parsedsr = Activator.parser.deserialize(serialized);
		if (parsedsr instanceof ServiceResponse) {
		    return (ServiceResponse) parsedsr;
		}
	    }
	    sr = new ServiceResponse(CallStatus.succeeded);
	} catch (MalformedURLException e) {
	    // TODO Auto-generated catch block
	    sr = new ServiceResponse(CallStatus.serviceSpecificFailure);
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    sr = new ServiceResponse(CallStatus.serviceSpecificFailure);
	    e.printStackTrace();
	}
	return sr;
    }
    
    /**
     * Method that performs the actual sending of a HTTP POST request to the
     * client remote node endpoint.
     * 
     * @param remoteid
     *            The client remote node endpoint
     * @param body
     *            The body of the HTTP POST, containing the formatted parameters
     * @return The String representation of the HTTP response, which will be
     *         empty for Context Event callbacks
     * @throws IOException
     *             If there was a problem in the connection sending or receivng
     *             information
     * @throws MalformedURLException
     *             If the URL to the client remote node endpoint could not be
     *             built
     */
    private static String sendPOST(String remoteid, String body) throws IOException, MalformedURLException {
	URL url = new URL(remoteid);
	HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	byte[] data = body.getBytes(Charset.forName("UTF-8"));
	conn.setRequestMethod("POST");
	conn.setInstanceFollowRedirects(false);
	conn.setDoOutput(true);
	conn.setDoInput(true);
	conn.setUseCaches(false);
	conn.setReadTimeout(30000);
	conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	conn.setRequestProperty("charset", "utf-8");
	conn.setRequestProperty("Content-Length", "" + Integer.toString(data.length));
	conn.setRequestProperty("Authorization", "placeholder");
	// conn.getOutputStream().write(data);
	// conn.disconnect();

	DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
	wr.write(data);
	wr.flush();
	wr.close();

	BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	String line, response = "";
	while ((line = rd.readLine()) != null) {
	    response = response + line + "\n";
	}
	rd.close();
	
	return response;
    }
}