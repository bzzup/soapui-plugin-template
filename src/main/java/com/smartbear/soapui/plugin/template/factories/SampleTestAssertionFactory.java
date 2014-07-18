package com.smartbear.soapui.plugin.template.factories;

import net.sf.json.JSONSerializer;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionCategoriesTableModel;
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

public class SampleTestAssertionFactory extends AbstractTestAssertionFactory {

    private static final String ASSERTION_ID = "SampleTestAssertionID";
    private static final String ASSERTION_LABEL = "JSON contains one element";

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
                "Asserts that JSON contains only 1 element" );
    }

    @Override
    public String getCategory() {
        return AssertionCategoryMapping.VALIDATE_RESPONSE_CONTENT_CATEGORY;
    }

    public static class SampleTestAssertion  extends WsdlMessageAssertion implements ResponseAssertion
    {
        /**
         * Assertions need to have a constructor that takes a TestAssertionConfig and the ModelItem to be asserted
         */

        public SampleTestAssertion(TestAssertionConfig assertionConfig, Assertable modelItem)
        {
            super( assertionConfig, modelItem, true, false, false, true );
        }

        @Override
        protected String internalAssertResponse(MessageExchange messageExchange, SubmitContext submitContext) throws AssertionException {

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
            
            String content = messageExchange.getResponse().getContentAsString();
            content = content.trim();
            
            if (!content.startsWith("[") && !content.endsWith("]")) {
    	    	content = "["+content+"]";
    	    }
            
            if (JSONSerializer.toJSON(content).size() != 1) {
            	throw new AssertionException( new AssertionError("JSON contains more than 1 element: " + JSONSerializer.toJSON(content).size() + "-> " + content));
            }
            
            return "JSON contains 1 element";
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
