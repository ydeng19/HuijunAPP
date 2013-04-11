package edu.asu.mobicloud.mcosgi;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Jackson {

	public static synchronized ObjectMapper getInstance() {
		if (mapper == null) {
			mapper = new ObjectMapper();
		}
		return mapper;
	}

	private Jackson() {
	}

	private static ObjectMapper mapper = null;

}
