package com.smartbear.soapui.plugin.template.factories;

import java.util.HashMap;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
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

public class ParseJSONFactory extends AbstractTestAssertionFactory {

    private static final String ASSERTION_ID = "JSONTestAssertionID";
    private static final String ASSERTION_LABEL = "JSON contains required key - value";
    

    public ParseJSONFactory()
    {
        super( ASSERTION_ID, ASSERTION_LABEL, JSONTestAssertion.class);
    }

    @Override
    public Class<? extends WsdlMessageAssertion> getAssertionClassType() {
        return JSONTestAssertion.class;
    }

    @Override
    public AssertionListEntry getAssertionListEntry() {
        return new AssertionListEntry(ASSERTION_ID, ASSERTION_LABEL,
                "Asserts that JSON contains required element. 'KEY.KEY.KEY = VALUE' format " );
    }

    @Override
    public String getCategory() {
        return AssertionCategoryMapping.VALIDATE_RESPONSE_CONTENT_CATEGORY;
    }

    public static class JSONTestAssertion extends WsdlMessageAssertion implements ResponseAssertion
    {
        /**
         * Assertions need to have a constructor that takes a TestAssertionConfig and the ModelItem to be asserted
         */

    	private String key;
    	private String value;
    	private String result;
    	private HashMap<String, String> keyValueMap = new HashMap<String, String>();
    	
        public JSONTestAssertion(TestAssertionConfig assertionConfig, Assertable modelItem)
        {
            super( assertionConfig, modelItem, true, true, true, false );
            XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(getConfiguration());
            key = reader.readString("key", "");
            value = reader.readString("value", "");
        }

        @Override
        protected String internalAssertResponse(MessageExchange messageExchange, SubmitContext submitContext) throws AssertionException {
            //Validate JSON has correct format
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
            
            //Convert to array
            if (!content.startsWith("[") && !content.endsWith("]")) {
    	    	content = "["+content+"]";
    	    }
            
            JSONObject json;
			try {
				json = (JSONObject) JSONArray.fromObject(content).get(0);
			} catch (Exception e) {
				throw new AssertionException( new AssertionError("Can't parse content to JSONObject"));
			}
            
            findKeys(json);
            
            String[] hierKeys = key.split("\\.");
            
			try {
				for (String elementKey : hierKeys) {
					if (json.get(elementKey).getClass() == JSONArray.class) {
						if (((JSONArray) json.get(elementKey)).get(0).getClass() == JSONObject.class) {
							json = (JSONObject) ((JSONArray) json.get(elementKey)).get(0);
						} else if (((JSONArray) json.get(elementKey)).get(0).getClass() == String.class) {
							result = getStringValues((JSONArray) json.get(elementKey));
						}

					} else {
						result = json.getString(elementKey);
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new AssertionException(new AssertionError(key + " : " + value + " not found"));
			}

			if (!result.equals(value)) {
				throw new AssertionException(new AssertionError("Expected "
						+ value + " != " + result));
			}

            return "OK";
        }
        
    	private String getStringValues(JSONArray array) {
    		String result = "";
    		for (int i = 0; i < array.size(); i++) {
    			result += "\""+array.getString(i)+"\",";
    		}
    		return result.substring(0, result.length()-1);
    	}
        
		private void findKeys(JSONObject object) {
			for (Object key : object.keySet()) {
				if (object.get(key).getClass() == JSONObject.class) {
					findKeys((JSONObject) object.get(key));
				} else {
					keyValueMap.put(String.valueOf(key), String.valueOf(object.get(key)));
				}
			}
		}
        
        public boolean configure() {
            String valueKey = key;
            String valueValue = value;
            

            if (valueKey == null || valueKey.trim().length() == 0) {
                valueKey = "id";
            }
            
            if (valueValue == null || valueValue.trim().length() == 0) {
            	valueValue = "1";
            }
            
            valueKey = UISupport.prompt("KEY", "Configure JSON Assertion", valueKey);
            valueValue = UISupport.prompt("VALUE", "Configure JSON Assertion", valueValue);
            
            key = valueKey;
            value = valueValue;
            
            
            	
            setConfiguration(createConfiguration());
            return true;
        }
        
        public String getKey() {
            return key;
        }

        public void setKey(String elements) {
        	key = elements;
            setConfiguration(createConfiguration());
        }
        
        public String getValue() {
            return value;
        }

        public void setValue(String elements) {
        	value = elements;
            setConfiguration(createConfiguration());
        }
        
        protected XmlObject createConfiguration() {
            XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
            builder.add("key", key);
            builder.add("value", value);
            return builder.finish();
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
