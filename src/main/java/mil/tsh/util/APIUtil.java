package mil.tsh.util;

import mil.tsh.types.Switch;
import mil.tsh.types.Token;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class APIUtil {

	public static String TUYA_URL = "https://px1.tuyaeu.com/homeassistant/";

	public static Token getToken(String username, String password, int countryCode) {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost post = new HttpPost(TUYA_URL + "auth.do");

			post.setEntity(new UrlEncodedFormEntity(new ArrayList<>() {{
				add(new BasicNameValuePair("userName", username));
				add(new BasicNameValuePair("password", password));
				add(new BasicNameValuePair("countryCode", countryCode + ""));
				add(new BasicNameValuePair("from", "tuya"));
			}}, "UTF-8"));

			JSONObject response = new JSONObject(EntityUtils.toString(client.execute(post).getEntity()));

			if (!response.has("access_token")) return null;

			return new Token(
					response.getString("access_token"),
					response.getString("refresh_token"),
					System.currentTimeMillis() / 1000L,
					(System.currentTimeMillis() / 1000L) + response.getInt("expires_in")
			);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static List<Switch> getDevices(Token token) {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost post = new HttpPost(TUYA_URL + "skill");

			post.setEntity(new StringEntity(new JSONObject("""
					{
						"header": {
							"name": "Discovery",
							"namespace": "discovery",
							"payloadVersion": 1
						},
						"payload": {
							"accessToken": "%s"
						}
					}
					""".formatted(token.getAccessToken())).toString()));
			post.setHeader("Content-type", "application/json");

			JSONArray response;
			try {
				response = new JSONObject(EntityUtils.toString(client.execute(post).getEntity())).getJSONObject("payload").getJSONArray("devices");
			} catch (Exception e) {
				return null;
			}

			List<Switch> devices = new ArrayList<>();

			response.forEach(o -> {
				JSONObject obj = (JSONObject) o;
				if (obj.getString("dev_type").equalsIgnoreCase("switch")) { // Check if device is switch
					devices.add(new Switch(obj.getString("id"), obj.getString("name"), obj.getJSONObject("data").getBoolean("state")));
				}
			});

			return devices;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static void setState(Token token, Switch device, boolean newState) {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost post = new HttpPost(TUYA_URL + "skill");

			post.setEntity(new StringEntity(new JSONObject("""
					{
						"header": {
							"name": "turnOnOff",
							"namespace": "control",
							"payloadVersion": 1
						},
						"payload": {
							"accessToken": "%s",
							"devId": "%s",
							"value": "%s"
						}
					}
					""".formatted(token.getAccessToken(), device.getId(), newState ? 1 : 0)).toString()));
			post.setHeader("Content-type", "application/json");

			client.execute(post);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
