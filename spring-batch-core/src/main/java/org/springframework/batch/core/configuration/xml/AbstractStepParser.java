/*
 * Copyright 2006-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.configuration.xml;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Internal parser for the &lt;step/&gt; elements inside a job. A step element
 * references a bean definition for a {@link org.springframework.batch.core.Step} and goes on to (optionally)
 * list a set of transitions from that step to others with &lt;next on="pattern"
 * to="stepName"/&gt;. Used by the {@link JobParser}.
 * 
 * @see JobParser
 * 
 * @author Dave Syer
 * @author Thomas Risberg
 * @since 2.0
 */
public abstract class AbstractStepParser {

	/**
	 * @param stepElement
	 * @param taskletRef
	 * @param parserContext
	 */
	protected AbstractBeanDefinition parseTaskletRef(Element stepElement, String taskletRef, ParserContext parserContext, String jobRepositoryRef) {

    	RootBeanDefinition bd = new RootBeanDefinition("org.springframework.batch.core.step.tasklet.TaskletStep", null, null);

        if (StringUtils.hasText(taskletRef)) {
            RuntimeBeanReference taskletBeanRef = new RuntimeBeanReference(taskletRef);
            bd.getPropertyValues().addPropertyValue("tasklet", taskletBeanRef);
        }

        checkStepAttributes(stepElement, bd);

        RuntimeBeanReference jobRepositoryBeanRef = new RuntimeBeanReference(jobRepositoryRef);
        bd.getPropertyValues().addPropertyValue("jobRepository", jobRepositoryBeanRef);

        String transactionManagerRef = stepElement.getAttribute("transaction-manager");
        RuntimeBeanReference transactionManagerBeanRef = new RuntimeBeanReference(transactionManagerRef);
        bd.getPropertyValues().addPropertyValue("transactionManager", transactionManagerBeanRef);
		
        handleListenersElement(stepElement, bd, parserContext, "stepExecutionListeners");
        
        bd.setRole(BeanDefinition.ROLE_SUPPORT);
        
        bd.setSource(parserContext.extractSource(stepElement));
        
        return bd;

    }

	/**
	 * @param element
	 * @param parserContext
	 */
	protected AbstractBeanDefinition parseTaskletElement(Element stepElement, Element element, ParserContext parserContext, String jobRepositoryRef) {

    	RootBeanDefinition bd;
    	
    	boolean isFaultTolerant = false;

        String skipLimit = element.getAttribute("skip-limit");
        if (!isFaultTolerant) {
        	isFaultTolerant = checkIntValueForFaultToleranceNeeded(skipLimit);
        }
        String retryLimit = element.getAttribute("retry-limit");
        if (!isFaultTolerant) {
        	isFaultTolerant = checkIntValueForFaultToleranceNeeded(retryLimit);
        }
        String cacheCapacity = element.getAttribute("cache-capacity");
        if (!isFaultTolerant) {
        	isFaultTolerant = checkIntValueForFaultToleranceNeeded(cacheCapacity);
        }
        String isReaderTransactionalQueue = element.getAttribute("is-reader-transactional-queue");
        if (!isFaultTolerant && StringUtils.hasText(isReaderTransactionalQueue)) {
        	if ("true".equals(isReaderTransactionalQueue)) {
				isFaultTolerant = true;
        	}
        }
        checkExceptionElementForFaultToleranceNeeded(element, "skippable-exception-classes");
        checkExceptionElementForFaultToleranceNeeded(element, "retryable-exception-classes");
        checkExceptionElementForFaultToleranceNeeded(element, "fatal-exception-classes");
		
        if (isFaultTolerant) {
        	bd = new RootBeanDefinition("org.springframework.batch.core.step.item.FaultTolerantStepFactoryBean", null, null);
        }
        else {
        	bd = new RootBeanDefinition("org.springframework.batch.core.step.item.SimpleStepFactoryBean", null, null);
        }

		// now, set the properties on the new bean 
        checkStepAttributes(stepElement, bd);

        String readerBeanId = element.getAttribute("reader");
        if (StringUtils.hasText(readerBeanId)) {
            RuntimeBeanReference readerRef = new RuntimeBeanReference(readerBeanId);
            bd.getPropertyValues().addPropertyValue("itemReader", readerRef);
        }

        String processorBeanId = element.getAttribute("processor");
        if (StringUtils.hasText(processorBeanId)) {
            RuntimeBeanReference processorRef = new RuntimeBeanReference(processorBeanId);
            bd.getPropertyValues().addPropertyValue("itemProcessor", processorRef);
        }

        String writerBeanId = element.getAttribute("writer");
        if (StringUtils.hasText(writerBeanId)) {
            RuntimeBeanReference writerRef = new RuntimeBeanReference(writerBeanId);
            bd.getPropertyValues().addPropertyValue("itemWriter", writerRef);
        }

        String taskExecutorBeanId = element.getAttribute("task-executor");
        if (StringUtils.hasText(taskExecutorBeanId)) {
            RuntimeBeanReference taskExecutorRef = new RuntimeBeanReference(taskExecutorBeanId);
            bd.getPropertyValues().addPropertyValue("taskExecutor", taskExecutorRef);
        }

        RuntimeBeanReference jobRepositoryBeanRef = new RuntimeBeanReference(jobRepositoryRef);
        bd.getPropertyValues().addPropertyValue("jobRepository", jobRepositoryBeanRef);

        String transactionManagerRef = stepElement.getAttribute("transaction-manager");
        RuntimeBeanReference tx = new RuntimeBeanReference(transactionManagerRef);
        bd.getPropertyValues().addPropertyValue("transactionManager", tx);

        String commitInterval = element.getAttribute("commit-interval");
        if (StringUtils.hasText(commitInterval)) {
            bd.getPropertyValues().addPropertyValue("commitInterval", commitInterval);
        }

        if (StringUtils.hasText(skipLimit)) {
            bd.getPropertyValues().addPropertyValue("skipLimit", skipLimit);
        }

        if (StringUtils.hasText(retryLimit)) {
            bd.getPropertyValues().addPropertyValue("retryLimit", retryLimit);
        }

        if (StringUtils.hasText(cacheCapacity)) {
            bd.getPropertyValues().addPropertyValue("cacheCapacity", cacheCapacity);
        }

        String transactionAttribute = element.getAttribute("transaction-attribute");
        if (StringUtils.hasText(transactionAttribute)) {
            bd.getPropertyValues().addPropertyValue("transactionAttribute", transactionAttribute);
        }

        if (StringUtils.hasText(isReaderTransactionalQueue)) {
        	if (isFaultTolerant) {
        		bd.getPropertyValues().addPropertyValue("isReaderTransactionalQueue", isReaderTransactionalQueue);
        	}
        }

        handleExceptionElement(element, parserContext, bd, "skippable-exception-classes", "skippableExceptionClasses", isFaultTolerant);
        
        handleExceptionElement(element, parserContext, bd, "retryable-exception-classes", "retryableExceptionClasses", isFaultTolerant);
        
        handleExceptionElement(element, parserContext, bd, "fatal-exception-classes", "fatalExceptionClasses", isFaultTolerant);

        handleListenersElement(stepElement, bd, parserContext, "listeners");
        
        handleRetryListenersElement(element, bd, parserContext);
        
        handleStreamsElement(element, bd, parserContext);
        
        bd.setRole(BeanDefinition.ROLE_SUPPORT);
        
        bd.setSource(parserContext.extractSource(stepElement));

        return bd;
        
	}

	private void checkStepAttributes(Element stepElement, RootBeanDefinition bd) {
		String startLimit = stepElement.getAttribute("start-limit");
        if (StringUtils.hasText(startLimit)) {
            bd.getPropertyValues().addPropertyValue("startLimit", startLimit);
        }
        String allowStartIfComplete = stepElement.getAttribute("allow-start-if-complete");
        if (StringUtils.hasText(allowStartIfComplete)) {
            bd.getPropertyValues().addPropertyValue("allowStartIfComplete", allowStartIfComplete);
        }
	}

	private boolean checkIntValueForFaultToleranceNeeded(String stringValue) {
		if (StringUtils.hasText(stringValue)) {
        	int value = Integer.valueOf(stringValue);
        	if (value > 0) {
				return true;
        	}
        }
		return false;
	}

	private boolean checkExceptionElementForFaultToleranceNeeded(Element element, String subElementName) {
		String exceptions = 
        	DomUtils.getChildElementValueByTagName(element, subElementName);
        if (StringUtils.hasLength(exceptions)) {
        	return true;
        }
		return false;
	}

	private void handleExceptionElement(Element element, ParserContext parserContext, BeanDefinition bd, 
			String subElementName, String propertyName, boolean isFaultTolerant) {
		String exceptions = 
        	DomUtils.getChildElementValueByTagName(element, subElementName);
        if (StringUtils.hasLength(exceptions)) {
        	if (isFaultTolerant) {
		        String[] exceptionArray = StringUtils.tokenizeToStringArray(
		        		StringUtils.delete(exceptions, ","), "\n");
		        if (exceptionArray.length > 0) {
		        	bd.getPropertyValues().addPropertyValue(propertyName, exceptionArray);
		        }
        	}
        	else {
				parserContext.getReaderContext().error(subElementName + " can only be specified for fault-tolerant " +
						"configurations providing skip-limit, retry-limit or cache-capacity", element);
        	}
        }
	}

	@SuppressWarnings("unchecked")
	private void handleListenersElement(Element element, BeanDefinition bd, ParserContext parserContext, String property) {
		Element listenersElement = 
        	DomUtils.getChildElementByTagName(element, "listeners");
		if (listenersElement != null) {
			CompositeComponentDefinition compositeDef =
				new CompositeComponentDefinition(listenersElement.getTagName(), parserContext.extractSource(element));
			parserContext.pushContainingComponent(compositeDef);
			List<Object> listenerBeans = new ArrayList<Object>(); 
			handleStepListenerElements(parserContext, listenersElement,
					listenerBeans);
	        ManagedList arguments = new ManagedList();
	        arguments.addAll(listenerBeans);
        	bd.getPropertyValues().addPropertyValue(property, arguments);
        	parserContext.popAndRegisterContainingComponent();
		}
	}

	@SuppressWarnings("unchecked")
	private void handleRetryListenersElement(Element element, BeanDefinition bd, ParserContext parserContext) {
		Element listenersElement = 
        	DomUtils.getChildElementByTagName(element, "retry-listeners");
		if (listenersElement != null) {
			CompositeComponentDefinition compositeDef =
				new CompositeComponentDefinition(listenersElement.getTagName(), parserContext.extractSource(element));
			parserContext.pushContainingComponent(compositeDef);
			List<Object> retryListenerBeans = new ArrayList<Object>(); 
			handleRetryListenerElements(parserContext, listenersElement,
					retryListenerBeans);
	        ManagedList arguments = new ManagedList();
	        arguments.addAll(retryListenerBeans);
        	bd.getPropertyValues().addPropertyValue("retryListeners", arguments);
        	parserContext.popAndRegisterContainingComponent();
		}
	}

	@SuppressWarnings("unchecked")
	private void handleRetryListenerElements(ParserContext parserContext,
			Element element, List<Object> beans) {
		List<Element> listenerElements = 
			DomUtils.getChildElementsByTagName(element, "listener");
		if (listenerElements != null) {
			for (Element listenerElement : listenerElements) {
				String id = listenerElement.getAttribute("id");
				String listenerRef = listenerElement.getAttribute("ref");
				String className = listenerElement.getAttribute("class");
				checkListenerElementAttributes(parserContext, element,
						listenerElement, id, listenerRef, className);
				if (StringUtils.hasText(listenerRef)) {
			        BeanReference bean = new RuntimeBeanReference(listenerRef);
					beans.add(bean);
				}
				else if (StringUtils.hasText(className)) {
					RootBeanDefinition beanDef = new RootBeanDefinition(className, null, null);
					if (!StringUtils.hasText(id)) {
						id = parserContext.getReaderContext().generateBeanName(beanDef);
					}
					beans.add(beanDef);
				}
				else {
					parserContext.getReaderContext().error("Neither 'ref' or 'class' specified for <" + listenerElement.getTagName() + "> element", element);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void handleStepListenerElements(ParserContext parserContext,
			Element element, List<Object> beans) {
		List<Element> listenerElements = 
			DomUtils.getChildElementsByTagName(element, "listener");
		if (listenerElements != null) {
			for (Element listenerElement : listenerElements) {
				BeanDefinitionBuilder listenerBuilder = 
					BeanDefinitionBuilder.genericBeanDefinition("org.springframework.batch.core.listener.StepListenerFactoryBean");
				String id = listenerElement.getAttribute("id");
				String listenerRef = listenerElement.getAttribute("ref");
				String className = listenerElement.getAttribute("class");
				checkListenerElementAttributes(parserContext, element,
						listenerElement, id, listenerRef, className);
				if (StringUtils.hasText(listenerRef)) {
			        listenerBuilder.addPropertyReference("delegate", listenerRef);
				}
				else if (StringUtils.hasText(className)) {
					RootBeanDefinition beanDef = new RootBeanDefinition(className, null, null);
					listenerBuilder.addPropertyValue("delegate", beanDef);
				}
				else {
					parserContext.getReaderContext().error("Neither 'ref' or 'class' specified for <" + listenerElement.getTagName() + "> element", element);
				}
				
				ManagedMap metaDataMap = new ManagedMap();
				String[] methodNameAttributes = new String[] {
						"before-step-method",
						"after-step-method",
						"before-chunk-method",
						"after-chunk-method",
						"before-read-method",
						"after-read-method",
						"on-read-error-method",
						"before-process-method",
						"after-process-method",
						"on-process-error-method",
						"before-write-method",
						"after-write-method",
						"on-write-error-method",
						"on-skip-in-read-method",
						"on-skip-in-process-method",
						"on-skip-in-write-method"
				};
				for (String metaDataPropertyName : methodNameAttributes) {
					String listenerMethod = listenerElement.getAttribute(metaDataPropertyName);
					if(StringUtils.hasText(listenerMethod)){
						metaDataMap.put(metaDataPropertyName, listenerMethod);
					}
				}
				listenerBuilder.addPropertyValue("metaDataMap", metaDataMap);
				
				AbstractBeanDefinition beanDef = listenerBuilder.getBeanDefinition();
				if (!StringUtils.hasText(id)) {
					id = parserContext.getReaderContext().generateBeanName(beanDef);
				}
				beans.add(beanDef);
			}
		}
	}

	private void checkListenerElementAttributes(ParserContext parserContext,
			Element element, Element listenerElement, String id,
			String listenerRef, String className) {
		if ((StringUtils.hasText(id) || StringUtils.hasText(className)) 
				&& StringUtils.hasText(listenerRef)) {
			NamedNodeMap attributeNodes = listenerElement.getAttributes();
			StringBuilder attributes = new StringBuilder();
			for (int i = 0; i < attributeNodes.getLength(); i++) {
				if (i > 0) {
					attributes.append(" ");
				}
				attributes.append(attributeNodes.item(i));
			}
			parserContext.getReaderContext().error("Both 'ref' and " +
					(StringUtils.hasText(id) ? "'id'" : "'class'") +
					" specified; use 'class' with an optional 'id' or just 'ref' for <" + 
					listenerElement.getTagName() + "> element specified with attributes: " + attributes, element);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleStreamsElement(Element element, BeanDefinition bd, ParserContext parserContext) {
		Element streamsElement = 
        	DomUtils.getChildElementByTagName(element, "streams");
		if (streamsElement != null) {
			List<BeanReference> streamBeans = new ArrayList<BeanReference>(); 
			List<Element> streamElements = 
	        	DomUtils.getChildElementsByTagName(streamsElement, "stream");
			if (streamElements != null) {
				for (Element streamElement : streamElements) {
					String streamRef = streamElement.getAttribute("ref");
					if (StringUtils.hasText(streamRef)) {
				        BeanReference bean = new RuntimeBeanReference(streamRef);
						streamBeans.add(bean);
					}
					else {
						parserContext.getReaderContext().error("ref not specified for <" + streamElement.getTagName() + "> element", element);
					}
				}
			}
	        ManagedList arguments = new ManagedList();
	        arguments.addAll(streamBeans);
        	bd.getPropertyValues().addPropertyValue("streams", arguments);
		}
	}

}