import java.util.ArrayList;
import java.util.HashMap;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.bzzup.entity.JsonElement;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;


public class Main {

	private static HashMap<String, String> keyValueMap = new HashMap<String, String>();
	private static ArrayList<JsonElement> elementsArray = new ArrayList<JsonElement>();

	@SuppressWarnings("unused")
	public static void main(String[] args) throws AssertionException {
		String key = "code.startDate";
		String content = "{\"id\":\"18058767\",\"name\":\"Form 1A\",\"shortName\":\"1A\",\"type\":\"FORM\",\"gradeLevels\":[\"1\"],\"referenced\":[{\"id\":\"18058772\",\"name\":\"Merged Form\",\"shortName\":\"MRG01\",\"type\":\"MERGED_FORM\",\"gradeLevels\":null,\"referenced\":null,\"members\":null,\"unitId\":null,\"schoolYear\":null,\"startDate\":null,\"endDate\":null}],\"members\":null,\"unitId\":\"18055809\",\"schoolYear\":\"2015\",\"startDate\":1406844000000,\"endDate\":1438293600000}";
		//String content = "[{\"personId\":\"SOA01-1111\",\"unitId\":\"18041399\",\"schoolYear\":\"2015\",\"gradeLevel\":\"5\",\"adjustments\":[],\"array\":[\"item1\",\"item2\",\"item3\"],\"items\":[{\"courseId\":\"18041425\",\"subjectId\":null,\"minutes\":500,\"adjustments\":[],\"aids\":[\"aid1\",\"aid2\"],\"subitems\":[{\"subitem1\":\"value1\",\"subitemArray\":[\"11\",\"223\"]},{}]},{\"courseId\":\"18041426\",\"subjectId\":null,\"minutes\":500,\"adjustments\":[],\"aids\":[]}]}]";
		//String content = args[0];
		String result = null;
		JSONArray jsonAr = null;
		JSONArray prevArr = null;
		Boolean isElement = false;
		Boolean isEmptyElement = false;
		
		content = content.trim();
	    if (!content.startsWith("[") && !content.endsWith("]")) {
	    	content = "["+content+"]";
	    }
 	    JSONObject json = (JSONObject) JSONArray.fromObject(content).getJSONObject(0);
	    findKeys(JSONArray.fromObject(content), null);
	    
	    for (JsonElement element : elementsArray) {
			System.out.println(element.getKey()+" : "+element.getValue());
		}
	    
	    test();
	    
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
					if ((json.get(elementKey) != null) && (json.get(elementKey).getClass() == JSONArray.class)) {
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
							break;
						}

					} else if (json.get(elementKey) == null) {
						result = null;
						isElement = true;
						break;
					} else {
						result = json.getString(elementKey);
						isElement = true;
						break;
					}
				}			
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new AssertionException(new AssertionError("Entered key doesn't exist"));
		}
	}
	
	static void test () {
		String testKey = "code";
		String testValue = "4";
		boolean result = false;
		
		System.out.println("Looking for ["+testKey+" : "+testValue+"]...");
		
		for (JsonElement jsonElement : elementsArray) {
			if (jsonElement.getKey().equalsIgnoreCase(testKey)) { 
				if ((jsonElement.getValue() != null) && (jsonElement.getValue().contains(testValue))) {
					result = true;
				}
			}
		}
		
		System.out.println("Result : "+result);
	}
	
	static String getStringValues(JSONArray array) {
		String result = "";
		for (int i = 0; i < array.size(); i++) {
			result += "\""+array.getString(i)+"\",";
		}
		return result.substring(0, result.length()-1);
	}
	
	static void findKeys(Object object, String key) {
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
	
	static void scanForElements(JSONObject obj, String parentKey) {
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
	
	static String buildKey(String parentKey, String currentKey) {
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

	static void putElement(String key, String value) {
		elementsArray.add(new JsonElement(key, value));
	}
}
