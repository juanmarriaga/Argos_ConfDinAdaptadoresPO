package com.argos.xpi.af.modules.dcappender;


import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.swing.JOptionPane;

import com.argos.xpi.af.modules.dcappender.util.AuditLogHelper;
import com.argos.xpi.af.modules.dcappender.util.DynamicConfigurationAttribute;
import com.argos.xpi.af.modules.dcappender.util.DynamicConfigurationProvider;
import com.argos.xpi.af.modules.dcappender.util.DynamicConfigurationProviderException;
import com.argos.xpi.af.modules.dcappender.util.DynamicConfigurationProviderFactory;
import com.argos.xpi.af.modules.dcappender.util.DynamicConfigurationProviderFactoryException;
import com.argos.xpi.af.modules.dcappender.util.DynamicConfigurationProviderType;
import com.sap.aii.adapter.xi.ms.XIMessage;
import com.sap.aii.af.lib.mp.module.Module;
import com.sap.aii.af.lib.mp.module.ModuleContext;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;
import com.sap.aii.af.sdk.xi.mo.xmb.DynamicConfiguration;
import com.sap.aii.af.sdk.xi.mo.xmb.XMBMessageOperator;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.engine.interfaces.messaging.api.exception.MessagingException;
import com.sap.tc.logging.Location;

/**
 * Session Bean implementation class AddDynamicConfigurationBean
 */
@Stateless
public class AddDynamicConfigurationBean implements Module, SessionBean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 326059161189176393L;

	private static final Location TRACE = Location
			.getLocation(AddDynamicConfigurationBean.class.getName());

	private static final String PARAMETER_DC_PREFIX = "dc.";
	private static final String PARAMETER_PWD_PREFIX = "pwd";

	private static final List<String> parameterPrefixes = new ArrayList<String>();

	// Valid prefixes for parameter name for dynamic configuration provider
	static {
		parameterPrefixes.add(PARAMETER_DC_PREFIX);
		parameterPrefixes.add(PARAMETER_PWD_PREFIX + PARAMETER_DC_PREFIX);
		parameterPrefixes.add(PARAMETER_PWD_PREFIX + "." + PARAMETER_DC_PREFIX);
	}
	
	
	
	private static final String PARAMETER_DC_PROVIDER_CLASS = "class";

	/**
	 * Default constructor.
	 */
	public AddDynamicConfigurationBean() {
		// Not used in current implementation
	}

	
	public ModuleData process(ModuleContext moduleContext, ModuleData moduleData)
			throws ModuleException {

		com.sap.engine.interfaces.messaging.api.Message message = (com.sap.engine.interfaces.messaging.api.Message) moduleData
				.getPrincipalData();
		MessageKey messageKey = message.getMessageKey();
		AuditLogHelper audit = new AuditLogHelper(messageKey);
		
		audit.addAuditLogEntry(AuditLogStatus.SUCCESS,
				AddDynamicConfigurationBean.class.getSimpleName()
						+ ": Using custom bean. ");
		int ent = 7 / 0;
		try {
			AuditAccess auditTest = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();
			auditTest.addAuditLogEntry(messageKey, AuditLogStatus.SUCCESS, "AuditLog Nuevo test");
			
		} catch (MessagingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new ModuleException("Error en inicio de adaptador");
		}
		
		System.err.println("Esta Saliendo En el Log Lindo Cochito");
		

		// Get dynamic configuration provider class name and parameters
		String dcProviderClassName = this
				.getDynamicConfigurationProviderClassName(moduleContext);
		Map<String, String> dcProviderParameters = this
				.getDynamicConfigurationProviderParameters(moduleContext);
		// Add ChannelID as Parameter for further usage
		dcProviderParameters.put("ModuleContext.ChannelID", moduleContext.getChannelID());
		
		// Get XI message
		XIMessage xiMessage = (XIMessage) moduleData.getPrincipalData();
		com.sap.aii.af.sdk.xi.mo.Message xmbMessage = (com.sap.aii.af.sdk.xi.mo.Message) xiMessage
				.getXMBMessage();

		DynamicConfiguration dcAttributes = XMBMessageOperator
				.getDynamicConfiguration(xmbMessage, true);

		List<DynamicConfigurationAttribute> dcAdditionalAttributes = new ArrayList<DynamicConfigurationAttribute>();

		String dcProviderType = null;

		if (dcProviderClassName != null && !dcProviderClassName.isEmpty()) {
			// Custom class
			dcProviderType = DynamicConfigurationProviderType.DC_PROVIDER_CLASS;
		} else {
			// Default class
			dcProviderType = DynamicConfigurationProviderType.DC_PROVIDER_DEFAULT;
		}

		// Get additional dynamic configuration
		try {
			DynamicConfigurationProviderFactory dcProviderFactory = new DynamicConfigurationProviderFactory();
			DynamicConfigurationProvider dcProvider = dcProviderFactory
					.getDynamicConfigurationProvider(dcProviderType,
							dcProviderClassName);

			audit.addAuditLogEntry(AuditLogStatus.SUCCESS,
					AddDynamicConfigurationBean.class.getSimpleName()
							+ ": Using dynamic configuration provider class: "
							+ dcProvider.getClass().getCanonicalName());

			dcAdditionalAttributes = dcProvider.execute(message,
					dcProviderParameters, moduleData);

		} catch (DynamicConfigurationProviderFactoryException e) {
			throw new ModuleException(
					"Error when accessing dynamic configuration provider: "
							+ e.getMessage(), e);
		} catch (DynamicConfigurationProviderException e) {
			throw new ModuleException(
					"Error when executing dynamic configuration provider: "
							+ e.getMessage(), e);
		}

		// Update message dynamic configuration
		if (!dcAdditionalAttributes.isEmpty()) {
			audit
					.addAuditLogEntry(
							AuditLogStatus.SUCCESS,
							AddDynamicConfigurationBean.class.getSimpleName()
									+ ": Setting additional dynamic configuration attribute(s)...");

			for (DynamicConfigurationAttribute dcAdditionalAttribute : dcAdditionalAttributes) {
				TRACE
						.debugT("Set additional dynamic configuration attribute. Name: "
								+ dcAdditionalAttribute.name
								+ ", namespace: "
								+ dcAdditionalAttribute.namespace
								+ ", value: "
								+ dcAdditionalAttribute.value);

				dcAttributes
						.put(dcAdditionalAttribute.namespace,
								dcAdditionalAttribute.name,
								dcAdditionalAttribute.value);
			}

			moduleData.setPrincipalData(xiMessage);

		} else {
			audit
					.addAuditLogEntry(
							AuditLogStatus.SUCCESS,
							AddDynamicConfigurationBean.class.getSimpleName()
									+ ": No additional dynamic configuration attributes retrieved");

			TRACE
					.debugT("No additional dynamic configuration attributes retrieved");
		}

		return moduleData;
	}

	private String getDynamicConfigurationProviderClassName(
			ModuleContext moduleContext) {

		return moduleContext.getContextData(PARAMETER_DC_PROVIDER_CLASS);

	}

	private Map<String, String> getDynamicConfigurationProviderParameters(
			ModuleContext moduleContext) {

		Map<String, String> dcProviderParameters = new HashMap<String, String>();

		String channelConfigParameterName = null;
		String channelConfigParameterValue = null;

		Enumeration<?> channelConfigParameters = moduleContext
				.getContextDataKeys();

		while (channelConfigParameters.hasMoreElements()) {
			channelConfigParameterName = (String) channelConfigParameters
					.nextElement();
			channelConfigParameterValue = moduleContext
					.getContextData(channelConfigParameterName);
			for (String parameterPrefix : parameterPrefixes) {
				if (channelConfigParameterName.startsWith(parameterPrefix)) {
					dcProviderParameters.put(channelConfigParameterName
							.substring(channelConfigParameterName
									.indexOf(PARAMETER_DC_PREFIX)
									+ PARAMETER_DC_PREFIX.length()),
							channelConfigParameterValue);
				}
			}
		}

		return dcProviderParameters;

	}


	@Override
	public void ejbActivate() throws EJBException, RemoteException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void ejbPassivate() throws EJBException, RemoteException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void ejbRemove() throws EJBException, RemoteException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {
		// TODO Auto-generated method stub
		
	}
}
