import java.util.ArrayList;
import java.util.HashMap;

import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class Main {

	private static HashMap<String, String> keyValueMap = new HashMap<String, String>();
	private final static int CASE_ARRAY = 1;
	private final static int CASE_OBJECT = 2;
	private static int CASE = 2;
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws AssertionException {
		String key = "longName.mama";
		String content = "[{\"id\":\"1142380\",\"code\":\"KHV0002\",\"startDate\":1343772000000,\"endDate\":null,\"elementId\":\"1140100\",\"points\":0,\"educationLevels\":[\"GRS\", \"TEST\"],\"longName\":[{\"locale\":\"no_NO\",\"value\":\"Kunst og håndverk 2. årstrinn\"}],\"shortName\":[{\"locale\":\"no_NO\",\"value\":\"Kunst og håndverk 2. årstrinn\",\"test\":[{\"name\":\"Andrei\",\"surname\":\"Hrabun\"}]}],\"courseGroup\":null}]";
		//String content = args[0];
		String result = null;
		content = content.trim();
	    if (!content.startsWith("[") && !content.endsWith("]")) {
	    	content = "["+content+"]";
	    }
 	    JSONObject json = (JSONObject) JSONArray.fromObject(content).getJSONObject(0);
	    
	    findKeys(json);
	    
		String[] hierKeys = key.split("\\.");
		try {
			for (String elementKey : hierKeys) {

				if (json.get(elementKey).getClass() == JSONArray.class) {
					if (((JSONArray) json.get(elementKey)).get(0).getClass() == JSONObject.class) {
						json = (JSONObject) ((JSONArray) json.get(elementKey)).get(0);
					} else if (((JSONArray) json.get(elementKey)).get(0).getClass() == String.class) {
						result = getStringValues((JSONArray) json.get(elementKey));
								//(String) ((JSONArray) json.get(elementKey)).get(0);
					}
					
				} else {
					result = json.getString(elementKey);
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
