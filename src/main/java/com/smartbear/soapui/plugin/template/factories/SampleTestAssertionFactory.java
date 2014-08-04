package com.smartbear.soapui.plugin.template.factories;

import net.sf.json.JSONSerializer;

import org.apache.xmlbeans.XmlObject;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionCategoryMapping;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionListEntry;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.AbstractTestAssertionFactory;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.ResponseAssertion;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

public class SampleTestAssertionFactory extends AbstractTestAssertionFactory {

    private static final String ASSERTION_ID = "SampleTestAssertionID";
    private static final String ASSERTION_LABEL = "JSON contains required amount of elements";
    

    public SampleTestAssertionFactory()
    {
        super( ASSERTION_ID, ASSERTION_LABEL, SampleTestAssertion.class);
    }

    @Override
    public Class<? extends WsdlMessageAssertion> getAssertionClassType() {
        return SampleTestAssertion.class;
    }

    @Override
    public AssertionListEntry getAssertionListEntry() {
        return new AssertionListEntry(ASSERTION_ID, ASSERTION_LABEL,
                "Asserts that JSON contains required amount of elements" );
    }

    @Override
    public String getCategory() {
        return AssertionCategoryMapping.VALIDATE_RESPONSE_CONTENT_CATEGORY;
    }

    public static class SampleTestAssertion extends WsdlMessageAssertion implements ResponseAssertion
    {
        /**
         * Assertions need to have a constructor that takes a TestAssertionConfig and the ModelItem to be asserted
         */

    	private String Elements;
    	
        public SampleTestAssertion(TestAssertionConfig assertionConfig, Assertable modelItem)
        {
            super( assertionConfig, modelItem, false, true, false, false );
            XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(getConfiguration());
            Elements = reader.readString("Elements", "1");
        }

        @Override
        protected String internalAssertResponse(MessageExchange messageExchange, SubmitContext submitContext) throws AssertionException {
        	int elementsValue;
            try
            {
                String content = messageExchange.getResponse().getContentAsString();
                if(StringUtils.isNullOrEmpty(content))
                    return "Response is empty - not a valid JSON response";

                JSONSerializer.toJSON(messageExchange.getResponse().getContentAsString());
            }
            catch( Exception e )
            {
                throw new AssertionException( new AssertionError( "JSON Parsing failed; [" + e.toString() + "]" ));
            }
            
            try {
            	elementsValue = Integer.valueOf(Elements);
            }
            catch (Exception e){
            	throw new AssertionException( new AssertionError( "Can't parse elements to int" ));
            }
            
            String content = messageExchange.getResponse().getContentAsString();
            content = content.trim();
            
            if (!content.startsWith("[") && !content.endsWith("]")) {
    	    	content = "["+content+"]";
    	    }

            if (JSONSerializer.toJSON(content).size() != elementsValue) {
            	throw new AssertionException( new AssertionError("JSON contains more than "+elementsValue+" element: " + JSONSerializer.toJSON(content).size() + "-> " + content));
            }
            
            return "JSON contains "+elementsValue+" element";
        }
        
        public boolean configure() {
            String value = Elements;

            if (value == null || value.trim().length() == 0) {
                value = "1";
            }

            value = UISupport.prompt("Specify required amount of elements", "Configure JSON Assertion", value);
            	
            try {
                Long.parseLong(value);
                Elements = value;

            } catch (Exception e) {
                return false;
            }
            this.setName("JSON contains "+ value + " element(s)");
            setConfiguration(createConfiguration());
            return true;
        }
        
        protected XmlObject createConfiguration() {
            XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
            return builder.add("Elements", Elements).finish();
        }
        
        public String getElements() {
            return Elements;
        }

        public void setElements(String elements) {
        	Elements = elements;
            setConfiguration(createConfiguration());
        }

        @Override
        protected String internalAssertRequest(MessageExchange messageExchange, SubmitContext submitContext) throws AssertionException {
            return null;
        }

        @Override
        protected String internalAssertProperty(TestPropertyHolder testPropertyHolder, String s, MessageExchange messageExchange, SubmitContext submitContext) throws AssertionException {
            return null;
        }
    }
}
