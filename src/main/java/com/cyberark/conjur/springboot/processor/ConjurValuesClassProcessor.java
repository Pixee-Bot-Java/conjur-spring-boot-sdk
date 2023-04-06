package com.cyberark.conjur.springboot.processor;

import java.lang.reflect.Field;
import java.util.List;

import javax.annotation.Nullable;

import com.cyberark.conjur.springboot.annotations.ConjurValues;
import com.cyberark.conjur.springboot.core.env.ConjurPropertySource;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;
/**
 * 
 * Custom annotation ConjurValues class processor.
 *
 */
public class ConjurValuesClassProcessor implements BeanPostProcessor {

	private static Logger logger = LoggerFactory.getLogger(ConjurPropertySource.class);

	private final ConjurRetrieveSecretService conjurRetrieveSecretService;

	public ConjurValuesClassProcessor(ConjurRetrieveSecretService conjurRetrieveSecretService) {
		this.conjurRetrieveSecretService = conjurRetrieveSecretService;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

		Class<?> managedBeanClass = bean.getClass();

		List<Field> fieldList = FieldUtils.getFieldsListWithAnnotation(managedBeanClass, ConjurValues.class);
		
		for (Field field : fieldList) {
			if (field.isAnnotationPresent(ConjurValues.class)) {
				ReflectionUtils.makeAccessible(field);
				String[] variableId = field.getDeclaredAnnotation(ConjurValues.class).keys();
				byte[] result = null;
				try {
					result = conjurRetrieveSecretService.retriveMultipleSecretsForCustomAnnotation(variableId);

					field.set(bean, result);

				} catch (Exception e1) {
					logger.error(e1.getMessage());
				}

			}
		}

		return bean;
	}

	@Nullable
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}