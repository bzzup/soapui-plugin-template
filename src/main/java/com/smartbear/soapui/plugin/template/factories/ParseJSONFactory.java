package com.smartbear.soapui.plugin.template.factories;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.xmlbeans.XmlObject;

import com.bzzup.entity.JsonElement;
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
    private static final String CONTAINS = "Switch to \"contains\"";

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
    	
    	private boolean isContains;
    	private String key;
    	private String value;
    	private String result;
    	private ArrayList<JsonElement> elementsArray = new ArrayList<JsonElement>();
    	
        public JSONTestAssertion(TestAssertionConfig assertionConfig, Assertable modelItem)
        {
            super( assertionConfig, modelItem, true, true, true, true );
            XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(getConfiguration());
            key = reader.readString("key", "");
            value = reader.readString("value", "");
            isContains = reader.readBoolean("contains", false);
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
            JSONArray prevArr = null;
            Boolean isElement = false;
            Boolean isEmptyElement = false;
            
			try {
				json = (JSONObject) JSONArray.fromObject(content).get(0);
			} catch (Exception e) {
				throw new AssertionException( new AssertionError("Can't parse content to JSONObject"));
			}

            if (!isContains) {
            	String[] hierKeys = key.split("\\.");
                
    			try {
    				for (String elementKey : hierKeys) {
    					if (elementKey.equalsIgnoreCase("size()")) {
    						if (jsonAr != null) {
    							result = String.valueOf(jsonAr.size());
    						} else if (isElement && !isEmptyElement) {
    							result = "1";
    						} else if (isEmptyElement) {
    							result = "0";
    						} else {
    							if (prevArr == null) {
    								result = String.valueOf(json.size());
    							} else {
    								result = String.valueOf(prevArr.size());
    							}
    						}
    					} else {
    						if (json.get(elementKey).getClass() == JSONArray.class) {
    							if (((JSONArray) json.get(elementKey)).size() != 0) {
    								if (((JSONArray) json.get(elementKey)).get(0).getClass() == JSONObject.class) {
    									prevArr = (JSONArray) json.get(elementKey);
    									json = (JSONObject) ((JSONArray) json.get(elementKey)).get(0);
    								} else if (((JSONArray) json.get(elementKey)).get(0).getClass() == String.class) {
    									jsonAr = (JSONArray) json.get(elementKey);
    									result = getStringValues(jsonAr);
    								}
    							} else {
    								isEmptyElement = true;
    							}

    						} else {
    							result = json.getString(elementKey);
    							isElement = true;
    						}
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
            } else {
            	findKeys(json, null);
            	boolean result = false;
            	
            	for (JsonElement jsonElement : elementsArray) {
        			if (jsonElement.getKey().equalsIgnoreCase(key)) { 
        				if (jsonElement.getValue().contains(propValue)) {
        					result = true;
        				}
        			}
        		}
            	
            	if (result == false) {
            		throw new AssertionException(new AssertionError(key + " : " + propValue + " not found"));
            	}
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
        
		private void findKeys(Object object, String key) {
			if (object.getClass() == JSONObject.class) {
				JSONObject jsonObject = (JSONObject) object;
				scanForElements(jsonObject, key);
			} else if (object.getClass() == JSONArray.class) {
				JSONArray jsonArray = (JSONArray) object;
				if (jsonArray.size() != 0) {
					for (Object obj : jsonArray) {
						if (obj.getClass() == JSONObject.class) {
							scanForElements((JSONObject) obj, key);
						} else if (obj.getClass() == String.class) {
							putElement(buildKey(null, key), getStringValues(jsonArray));
							break;
						} else {
							findKeys(obj, null);
						}
					}
				} else {
					putElement(buildKey(null, key), null);
				}
				
			} else if (object == null) {
				putElement(buildKey(null, key), null);
			} else {
				String res = object.getClass().toString();
			}
		}
		
		private void scanForElements(JSONObject obj, String parentKey) {
			if (!obj.keySet().isEmpty()) {
				for (Object key : obj.keySet()) {
					if ((obj.get(key).getClass() == JSONObject.class) || (obj.get(key).getClass() == JSONArray.class)) {
						findKeys(obj.get(key), buildKey(parentKey, key.toString()));
					}  else {
						putElement(buildKey(parentKey, key.toString()), String.valueOf(obj.get(key)));
					}
				}
			} else {
				putElement(buildKey(parentKey, null), null);
			}
			
		}
		
		private void putElement(String key, String value) {
			elementsArray.add(new JsonElement(key, value));
		}
		
		private String buildKey(String parentKey, String currentKey) {
			String key = "";
			if ((parentKey != null) && (currentKey != null)) {
				key = parentKey + "." + currentKey;
			} else if ((parentKey == null) && (currentKey != null)) {
				key = currentKey;
			} else if ((parentKey != null) && (currentKey == null)) {
				key = parentKey;
			}
			return key;
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
            
            StringToStringMap values = new StringToStringMap();
            values.put(KEY_LABEL, key);
            values.put(VALUE_LABEL, value);
            values.put(CONTAINS, isContains);
            
            values = dialog.show(values);
            
            if (dialog.getReturnValue() == XFormDialog.OK_OPTION) {
                key = values.get(KEY_LABEL);
                value = values.get(VALUE_LABEL);
                isContains = values.getBoolean(CONTAINS);
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
            mainForm.addCheckBox(CONTAINS, null);

            dialog = builder.buildDialog(builder.buildOkCancelHelpActions(HelpUrls.SIMPLE_CONTAINS_HELP_URL),
                    "Specify options. Format: Key.Key.Key = Value", UISupport.TOOL_ICON);
        }
        
        protected XmlObject createConfiguration() {
            XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
            builder.add("key", key.trim());
            builder.add("value", value.trim());
            builder.add("contains", isContains);
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
