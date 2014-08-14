import java.util.ArrayList;
import java.util.HashMap;

import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class Main {

	private static HashMap<String, String> keyValueMap = new HashMap<String, String>();
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws AssertionException {
		String key = "items.size()";
		String content = "[{\"personId\":\"SOA01-1111\",\"unitId\":\"18041399\",\"schoolYear\":\"2015\",\"gradeLevel\":\"5\",\"adjustments\":[],\"items\":[{\"courseId\":\"18041425\",\"subjectId\":null,\"minutes\":500,\"adjustments\":[],\"aids\":[]},{\"courseId\":\"18041426\",\"subjectId\":null,\"minutes\":500,\"adjustments\":[],\"aids\":[]}]}]";
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
	    findKeys(json);
	    
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
			throw new AssertionException(new AssertionError("Entered key doesn't exist"));
		}
	    
	    String x = result;
	}
	
	static String getStringValues(JSONArray array) {
		String result = "";
		for (int i = 0; i < array.size(); i++) {
			result += "\""+array.getString(i)+"\",";
		}
		return result.substring(0, result.length()-1);
	}
	
	static void findKeys(JSONObject object) {
			for (Object key : object.keySet()) {
				if (object.get(key).getClass() == JSONObject.class) {
					findKeys((JSONObject) object.get(key));
				}  else {
					keyValueMap.put(String.valueOf(key), String.valueOf(object.get(key)));
				}
			}
		}

}
