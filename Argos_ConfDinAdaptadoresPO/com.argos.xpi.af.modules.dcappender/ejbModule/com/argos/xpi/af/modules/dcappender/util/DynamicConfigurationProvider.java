package com.argos.xpi.af.modules.dcappender.util;

import java.util.List;
import java.util.Map;

import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.engine.interfaces.messaging.api.Message;

public interface DynamicConfigurationProvider {

	public List<DynamicConfigurationAttribute> execute(Message message,
			Map<String, String> parameters, ModuleData moduleData) throws DynamicConfigurationProviderException;
	

}
