package com.argos.xpi.af.modules.dcappender.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;

import com.argos.xpi.af.modules.dcappender.util.AuditLogHelper;
import com.argos.xpi.af.modules.dcappender.util.DynamicConfigurationAttribute;
import com.argos.xpi.af.modules.dcappender.util.DynamicConfigurationProvider;
import com.argos.xpi.af.modules.dcappender.util.DynamicConfigurationProviderException;
import com.argos.xpi.af.modules.dcappender.util.http.HttpRequest;
import com.argos.xpi.af.modules.dcappender.util.http.HttpResponse;
import com.argos.xpi.af.modules.dcappender.util.http.HttpClient;
import com.argos.xpi.af.modules.dcappender.util.http.HttpClientException;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.tc.logging.Location;

public class DynamicConfigurationProviderSOAP implements DynamicConfigurationProvider {
	
private static final String PARAMETER_USERNAME = "SoapAuth.username";
private static final String PARAMETER_PASSWORD = "SoapAuth.password";
private static final String PARAMETER_URL = "SoapAuth.url";
private static final String PARAMETER_XPATH_IDSESSION = "SoapAuth.xpath.idsession";
private static final String PARAMETER_XPATH_SERVER_URL = "SoapAuth.xpath.serverurl";
private static final String PARAMETER_WS_URL_PATH = "wsUrl.path";
private static final String PARAMETER_WS_NAME = "ws.name";


private static final String PARAMETER_DC_ATTR_NAME = "attribute.name"; //TserverLocation
private static final String PARAMETER_DC_ATTR_NAMESPACE = "attribute.namespace"; //Http... Namespace donde queda



private static final Location TRACE = Location
		.getLocation(DynamicConfigurationProviderSOAP.class.getName());

@Override
public List<DynamicConfigurationAttribute> execute(Message message,
	Map<String, String> parameters, ModuleData moduleData)
	throws DynamicConfigurationProviderException {

String username = "";
String password = "";
String url = "";
String wsUrlPath = "";
String xpathIdSession = "";
String xpathServerUrl = "";
String wsUrl = "";
String wsName = "";


String SIGNATURE = "execute(Message message, Map<String, String> parameters)";
TRACE.entering(SIGNATURE, new Object[] { message,parameters });

MessageKey messageKey = message.getMessageKey();

AuditLogHelper audit = new AuditLogHelper(messageKey);
List<DynamicConfigurationAttribute> dcAttributes = new ArrayList<DynamicConfigurationAttribute>();
	DynamicConfigurationAttribute dcAttribute = null;

String parameterNamespace = null;
String parameterName = null;
String idSessionValue = null;
String serverUrlValue = null;




for (Map.Entry<String, String> parameter : parameters.entrySet()) {
	if(parameter.getKey().equals(PARAMETER_URL)) {
		url = parameter.getValue();
	} else if (parameter.getKey().equals(PARAMETER_USERNAME)) {
		username = parameter.getValue();				
	} else if (parameter.getKey().equals(PARAMETER_WS_URL_PATH)) {
		wsUrlPath = parameter.getValue();				
	} else if (parameter.getKey().equals(PARAMETER_WS_NAME)) {
		wsName = parameter.getValue();				
	} else if (parameter.getKey().equals(PARAMETER_XPATH_IDSESSION)) {
		xpathIdSession = parameter.getValue();				
	} else if (parameter.getKey().equals(PARAMETER_XPATH_SERVER_URL)) {
		xpathServerUrl = parameter.getValue();				
	} else if (parameter.getKey().equals(PARAMETER_PASSWORD)) {
		password = parameter.getValue();
	} else if (parameter.getKey().equals(PARAMETER_DC_ATTR_NAMESPACE)) {
		parameterNamespace = parameter.getValue();
		TRACE.debugT("ModuleParameter "+ parameter.getKey() +" : "+parameter.getValue());
	} else if (parameter.getKey().equals(PARAMETER_DC_ATTR_NAME)) {
		parameterName = parameter.getValue();
		TRACE.debugT("ModuleParameter "+ parameter.getKey() +" : "+parameter.getValue());
	} else {
		// Reserved for future use
	}
}

String postdata = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
	+ "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:argos.com:transaccionales\">"
	+ "<soapenv:Header/>"
	+ "<soapenv:Body>"
	+ "<urn:LoginAutenticacionReqSF>"
	+ "<username>"
	+ username
	+ "</username>"
	+ "<Password>"
	+ password
	+ "</Password>"
	+ "</urn:LoginAutenticacionReqSF>"
	+ "</soapenv:Body>" + "</soapenv:Envelope>";



// Set default values if not already set by Module Parameter
if (!parameters.containsKey("http.request.url"))
parameters.put("http.request.url", url);

if (!parameters.containsKey("http.request.postdata"))
parameters.put("http.request.postdata", postdata);

if (!parameters.containsKey("http.request.header.soapAction"))
parameters.put("http.request.header.soapAction","SOAPAction: \"Login\"");



		
		try {
			
			HttpRequest request = new HttpRequest(parameters);
			HttpResponse response = HttpClient.doRequest(request, parameters);
			idSessionValue = response.getXpathResponse(xpathIdSession);
			audit.addAuditLogEntry(AuditLogStatus.SUCCESS, "Id Session recuperado");
			audit.addAuditLogEntry(AuditLogStatus.SUCCESS, String.format(idSessionValue));
			
			serverUrlValue = response.getXpathResponse(xpathServerUrl);
			audit.addAuditLogEntry(AuditLogStatus.SUCCESS, "URL Servicio recuperada");
			audit.addAuditLogEntry(AuditLogStatus.SUCCESS, String.format(serverUrlValue));
			


		} catch (HttpClientException e) {
			audit.addAuditLogEntry(AuditLogStatus.ERROR, e.getMessage());
			throw new DynamicConfigurationProviderException(e.getMessage());
		}
	
	


try{
URL soapUrl = new URL(serverUrlValue);
wsUrl = soapUrl.getProtocol()+ "://"+ soapUrl.getHost() + "/"  + wsUrlPath;
audit.addAuditLogEntry(AuditLogStatus.SUCCESS, String.format(wsUrl));

audit.addAuditLogEntry(AuditLogStatus.SUCCESS, String.format(message.toString()));

soapEnvelopeWithIdSession(message, idSessionValue, wsName, moduleData );


}
catch(MalformedURLException mue){
	
}


if (parameterNamespace != null && !parameterNamespace.isEmpty()
&& parameterName != null && !parameterName.isEmpty()
&& wsUrl != null && !wsUrl.isEmpty()) {

dcAttribute = new DynamicConfigurationAttribute(parameterNamespace,
	parameterName, wsUrl);
if (dcAttribute.isDynamicConfigurationAttributeComplete()) {
dcAttributes.add(dcAttribute);
}else{
throw new DynamicConfigurationProviderException(String.format("Invalid configuratoin, check attribute %s an %s",PARAMETER_DC_ATTR_NAME,PARAMETER_DC_ATTR_NAMESPACE));
}

}


		
TRACE.exiting(SIGNATURE);


return dcAttributes;

}



private void soapEnvelopeWithIdSession(Message message, String idSession, String wsName, ModuleData moduleData  ){
	
	ByteArrayOutputStream payloadOut = null;
	
	MessageKey messageKey = message.getMessageKey();

	AuditLogHelper audit = new AuditLogHelper(messageKey);
	
	String strXml = "";
	
	
	try{
	String payloadStr = new String(message.getDocument().getContent(),"UTF-8");
	audit.addAuditLogEntry(AuditLogStatus.SUCCESS, "Payload antes de adicionar header");
	audit.addAuditLogEntry(AuditLogStatus.SUCCESS, String.format(payloadStr));
	
	strXml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"" 
	   + "xmlns:arg=\"http://soap.sforce.com/schemas/class/"+wsName+"\">"
	   +"<soapenv:Header>"
	   + "<arg:SessionHeader>"
	   +      "<arg:sessionId>"+idSession+"</arg:sessionId>"
	   +   "</arg:SessionHeader>"
	   + "</soapenv:Header>"
	   + "<soapenv:Body>"
	   +			payloadStr
	   + "</soapenv:Body>"
	+"</soapenv:Envelope>";
	
	
	audit.addAuditLogEntry(AuditLogStatus.SUCCESS, "SOAP Envelop desarrollado");
	audit.addAuditLogEntry(AuditLogStatus.SUCCESS, String.format(strXml));
	
	payloadOut = new ByteArrayOutputStream();
    payloadOut.write(strXml.getBytes());
    message.getDocument().setContent(payloadOut.toByteArray());
    moduleData.setPrincipalData(message);
    
	}
	catch(Exception e){
		
	}
	
}

}
