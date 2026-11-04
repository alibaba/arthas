package com.alibaba.arthas.spring.endpoints;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import com.taobao.arthas.agent.attach.ArthasAgent;

/**
 * 
 * @author hengyunabc 2020-06-24
 *
 */
@Endpoint(id = "arthas")
public class ArthasEndPoint {

	@Autowired(required = false)
	private ArthasAgent arthasAgent;

	@Autowired(required = false)
	private HashMap<String, String> arthasConfigMap;

	@ReadOperation
	public Map<String, Object> invoke() {
		Map<String, Object> result = new HashMap<String, Object>();

		if (arthasConfigMap != null) {
			result.put("arthasConfigMap", arthasConfigMap);
		}

		String errorMessage = arthasAgent.getErrorMessage();
		if (errorMessage != null) {
			result.put("errorMessage", errorMessage);
		}

		return result;
	}

}
