package com.smartbear.soapui.plugin.template.factories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.xmlbeans.XmlObject;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionCategoryMapping;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionListEntry;
import com.eviware.soapui.impl.wsdl.support.HelpUrls;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.AbstractTestAssertionFactory;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansion;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionUtils;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.ResponseAssertion;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.eviware.x.form.XForm;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormDialogBuilder;
import com.eviware.x.form.XFormFactory;

public class ParseJSONFactory extends AbstractTestAssertionFactory {

    private static final String ASSERTION_ID = "JSONTestAssertionID";
    private static final String ASSERTION_LABEL = "JSON contains required key - value";
    private static final String KEY_LABEL = "Key";
    private static final String VALUE_LABEL = "Value";
    

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
                "Asserts that JSON contains required element. 'KEY.KEY = VALUE', 'KEY.KEY.size() = VALUE' formats " );
    }

    @Override
    public String getCategory() {
        return AssertionCategoryMapping.VALIDATE_RESPONSE_CONTENT_CATEGORY;
    }
    
    @Override
    public boolean canAssert(TestPropertyHolder modelItem, String property) {
        String content = modelItem.getPropertyValue(property);
        return true;
    }

    public static class JSONTestAssertion extends WsdlMessageAssertion implements ResponseAssertion
    {
        /**
         * Assertions need to have a constructor that takes a TestAssertionConfig and the ModelItem to be asserted
         */

    	private XFormDialog dialog;
    	
    	private String key;
    	private String value;
    	private String result;
    	private HashMap<String, String> keyValueMap = new HashMap<String, String>();
    	
        public JSONTestAssertion(TestAssertionConfig assertionConfig, Assertable modelItem)
        {
            super( assertionConfig, modelItem, true, true, true, true );
            XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(getConfiguration());
            key = reader.readString("key", "");
            value = reader.readString("value", "");
        }

        @Override
        protected String internalAssertResponse(MessageExchange messageExchange, SubmitContext submitContext) throws AssertionException {
        	return doAssert(submitContext, messageExchange.getResponse().getContentAsString(), "Response");
        	
        }
        
        private String doAssert(SubmitContext context, String content, String type) throws AssertionException {
        	String propValue = PropertyExpander.expandProperties(context, value); 
        	if (propValue == null) {
        		propValue = "";
        	} 
        	
        	try
            {
                if(StringUtils.isNullOrEmpty(content))
                    return "Response is empty - not a valid JSON response";
                JSONSerializer.toJSON(content);
            }
            catch( Exception e )
            {
                throw new AssertionException( new AssertionError( "JSON Parsing failed; [" + e.toString() + "]" ));
            }

        	content = content.trim();
            
            //Convert to array
            if (!content.startsWith("[") && !content.endsWith("]")) {
    	    	content = "["+content+"]";
    	    }
            
            JSONObject json;
            JSONArray jsonAr = null;
            
			try {
				json = (JSONObject) JSONArray.fromObject(content).get(0);
			} catch (Exception e) {
				throw new AssertionException( new AssertionError("Can't parse content to JSONObject"));
			}
            
            findKeys(json);
            
            String[] hierKeys = key.split("\\.");
            
			try {
				for (String elementKey : hierKeys) {
					switch (elementKey) {
					case "size()":
					{
						if (jsonAr != null) {
							result = String.valueOf(jsonAr.size());
						} else {
							result = String.valueOf(json.size());
						}
					}
					break;
					default:
					{
						if (json.get(elementKey).getClass() == JSONArray.class) {
							if (((JSONArray) json.get(elementKey)).get(0).getClass() == JSONObject.class) {
								json = (JSONObject) ((JSONArray) json.get(elementKey)).get(0);
							} else if (((JSONArray) json.get(elementKey)).get(0).getClass() == String.class) {
								jsonAr = (JSONArray) json.get(elementKey);
								result = getStringValues(jsonAr);
							}

						} else {
							result = json.getString(elementKey);
						}
					}
					break;
					}
					
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new AssertionException(new AssertionError(key + " : " + propValue + " not found"));
			}

			if (!result.equals(propValue)) {
				throw new AssertionException(new AssertionError("Expected "
						+ propValue + " != " + result));
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
        	if (dialog == null) {
                buildDialog();
            }
        	
        	key = key.trim();
        	value = value.trim();
            

            if (key == null || key.trim().length() == 0) {
            	key = "id";
            }
            
//            if (valueValue == null || valueValue.trim().length() == 0) {
//            	valueValue = "1";
//            }
            
            StringToStringMap values = new StringToStringMap();
            values.put(KEY_LABEL, key);
            values.put(VALUE_LABEL, value);
            
            values = dialog.show(values);
            
            if (dialog.getReturnValue() == XFormDialog.OK_OPTION) {
                key = values.get(KEY_LABEL);
                value = values.get(VALUE_LABEL);
            }
            
            this.setName("JSON contains [\"" + key + "\" : \"" + value +"\"]");
            
            setConfiguration(createConfiguration());
            return true;
        }

        private void buildDialog() {
            XFormDialogBuilder builder = XFormFactory.createDialogBuilder("Contains Assertion");
            XForm mainForm = builder.createForm("Basic");

            mainForm.addTextField(KEY_LABEL, "Key to check for", XForm.FieldType.TEXT).setWidth(40);
            mainForm.addTextField(VALUE_LABEL, "Value to check for", XForm.FieldType.TEXTAREA).setWidth(40);

            dialog = builder.buildDialog(builder.buildOkCancelHelpActions(HelpUrls.SIMPLE_CONTAINS_HELP_URL),
                    "Specify options. Format: Key.Key.Key = Value", UISupport.TOOL_ICON);
        }
        
        protected XmlObject createConfiguration() {
            XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
            builder.add("key", key.trim());
            builder.add("value", value.trim());
            return builder.finish();
        }
        
        @Override
        protected String internalAssertRequest(MessageExchange messageExchange, SubmitContext submitContext) throws AssertionException {
        	return doAssert(submitContext, messageExchange.getRequestContent(), "Request");
        }

        @Override
        protected String internalAssertProperty(TestPropertyHolder testPropertyHolder, String propertyName, MessageExchange messageExchange, SubmitContext submitContext) throws AssertionException {
        	doAssert(submitContext, testPropertyHolder.getPropertyValue(propertyName), propertyName);
            return "OK";
        }
        
        public PropertyExpansion[] getPropertyExpansions() {
            List<PropertyExpansion> result = new ArrayList<PropertyExpansion>();

            result.addAll(PropertyExpansionUtils.extractPropertyExpansions(getAssertable().getModelItem(), this, "value"));

            return result.toArray(new PropertyExpansion[result.size()]);
        }
    }
}
