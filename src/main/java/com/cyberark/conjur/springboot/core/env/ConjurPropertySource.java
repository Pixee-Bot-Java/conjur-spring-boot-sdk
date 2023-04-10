package com.cyberark.conjur.springboot.core.env;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cyberark.conjur.sdk.ApiException;
import com.cyberark.conjur.sdk.endpoint.SecretsApi;
import com.cyberark.conjur.springboot.constant.ConjurConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;


/**
 * 
 * This class resolves the secret value for give vault path at application load
 * time from the conjur vault.
 *
 */
public class ConjurPropertySource
//extends PropertySource<Object> {
//consider the following alternative if miss rates are excessive
		extends EnumerablePropertySource<Object>{

	private String vaultInfo = "";

	private String vaultPath = "";

	private SecretsApi secretsApi;

	private List<String> properties;

	private static final String authTokenFile=System.getenv("CONJUR_AUTHN_TOKEN_FILE");

	private static final String authApiKey = System.getenv("CONJUR_AUTHN_API_KEY");
	
	private static final Logger logger = LoggerFactory.getLogger(ConjurPropertySource.class);
	/**
	 * a hack to support seeding environment for the file based api token support in
	 * downstream java
	 */
	static {

		// a hack to support seeding environment for the file based api token support in
		// downstream java
		if (authTokenFile != null) {
		Map<String, String> conjurParameters = new HashMap<String, String>();
		byte[] apiKey = null;
		try (BufferedReader br = new BufferedReader(new FileReader(authTokenFile))){
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			apiKey =  sb.toString().getBytes();
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
		}

		conjurParameters.put("CONJUR_AUTHN_API_KEY",new String(apiKey).trim());
		try {
		loadEnvironmentParameters(conjurParameters);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	
	}
    else if (authApiKey == null && authTokenFile == null) {
		 logger.error(ConjurConstant.CONJUR_APIKEY_ERROR);

	}
	}
	/**
	 * Sets the external environment variable.
	 * 
	 * @param newenv - setting for API_KEY
	 * @throws NoSuchFieldException     -- class doesn't have a field of a specified
	 *                                  name
	 * @throws SecurityException        --indicate a security violation.
	 * @throws IllegalArgumentException -- a method has been passed an illegal or
	 *                                  inappropriate argument.
	 * @throws IllegalAccessException   -- excuting method does not have access to
	 *                                  the definition of the specified class,
	 *                                  field, method or constructor.
	 */
	public static void loadEnvironmentParameters(Map<String, String> newenv)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Class[] classes = Collections.class.getDeclaredClasses();
		Map<String, String> env = System.getenv();
		for (Class cl : classes) {
			if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
				Field field = cl.getDeclaredField("m");
				field.setAccessible(true);
				Object obj = field.get(env);
				Map<String, String> map = (Map<String, String>) obj;
				map.putAll(newenv);
				
			}
		}
	}

	protected ConjurPropertySource(String vaultPath) {
		super(vaultPath + "@");
		this.vaultPath = vaultPath;

	}

	protected ConjurPropertySource(String vaultPath, String vaultInfo, AnnotationMetadata importingClassMetadata) throws ClassNotFoundException {
		super(vaultPath + "@" + vaultInfo);
		this.vaultPath = vaultPath;
		this.vaultInfo = vaultInfo;
		List<String> properties = new ArrayList<>();
		Class<?> annotatedClass = ClassUtils.forName((importingClassMetadata).getClassName(), getClass().getClassLoader());
		for (Field field : annotatedClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(Value.class)) {
				String value = field.getAnnotation(Value.class).value();
				properties.add(value);
			}
		}
		this.properties = properties;
	}

	@Override
	public String[] getPropertyNames() {
		return new String[0];
	}

	/**
	 * Method which resolves @value annotation queries and return result in the form of byte array.
	 */

	@Override
	public Object getProperty(String key) {
		String secretValue;
		key = ConjurConfig.getInstance().mapProperty(key);

		ConjurConnectionManager.getInstance();
		if (null == secretsApi) {
			secretsApi = new SecretsApi();
		}
    
		byte[] result = null;
		if(propertyExists(key)){
			key = ConjurConfig.getInstance().mapProperty(key);
			ConjurConnectionManager.getInstance();
			try {
				String secretValue = secretsApi.getSecret(ConjurConstant.CONJUR_ACCOUNT, ConjurConstant.CONJUR_KIND,
						vaultPath + key);
				result = secretValue != null ? secretValue.getBytes() : null;
			} catch (ApiException ae) {
				logger.warn("Failed to get property from Conjur for: " + key);
				logger.warn("Reason: " + ae.getResponseBody());
				logger.warn(ae.getMessage());
			}
		}
		return result;
	}

	public void setSecretsApi(SecretsApi secretsApi) {
		this.secretsApi = secretsApi;
	}

	private boolean propertyExists(String key) {
		return properties.stream()
				.anyMatch(property -> property.contains(key));
	}

	public void setSecretsApi(SecretsApi secretsApi) {
		this.secretsApi = secretsApi;
	}
}