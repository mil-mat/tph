package mil.tsh.types;

import mil.tsh.Application;
import mil.tsh.util.APIUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Token implements Serializable {

	public static final String SAVE_PATH = System.getProperty("user.dir") + "/data/token";

	static {
		new File(System.getProperty("user.dir") + "/data/").mkdirs();
	}

	private String _accessToken;
	private String _refreshToken;
	private long _generatedTime;
	private long _expiryTime;

	public Token(String accessToken, String refreshToken, long generatedTime, long expiryTime) {
		_accessToken = accessToken;
		_refreshToken = refreshToken;
		_generatedTime = generatedTime;
		_expiryTime = expiryTime;

		if ((expiryTime - System.currentTimeMillis() / 1000L) < 0) { // Check if token is expired
			refresh();
		}

		new Thread(() -> {
			long expTime = _expiryTime;

			while (true) {
				try {
					Thread.sleep((expTime - System.currentTimeMillis() / 1000L) - 30 * 1000L); // Sleep until token is close to expiry
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				refresh();
				expTime = _expiryTime;
			}
		}).start();
	}

	/**
	 * Refreshes this access token using its refresh token.
	 */
	private void refresh() {
		System.out.println("Refreshing Token...");

		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost post = new HttpPost(APIUtil.TUYA_URL + "access.do");

			post.setEntity(new UrlEncodedFormEntity(new ArrayList<NameValuePair>() {{
				add(new BasicNameValuePair("grant_type", "refresh_token"));
				add(new BasicNameValuePair("refresh_token", _refreshToken));
			}}, "UTF-8"));

			JSONObject response = new JSONObject(EntityUtils.toString(client.execute(post).getEntity()));

			/* Expected Response:
			{"access_token":"XXX",
			"refresh_token":"XXX",
			"token_type":"bearer",
			"expires_in":864000} */

			if (!response.has("access_token")) {
				System.out.println("Failed to generate new token. Logging out.");
				Application.logout();
				return;
			}

			setAccessToken(response.getString("access_token"));
			_refreshToken = response.getString("refresh_token");
			_generatedTime = System.currentTimeMillis() / 1000L;
			_expiryTime = (System.currentTimeMillis() / 1000L) + response.getInt("expires_in");

			writeFile();
			Application.getFrame().refresh();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Token readFile() {
		try {
			return (Token) new ObjectInputStream(new FileInputStream(SAVE_PATH)).readObject();
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE, "Failed to read from token file!");
			e.printStackTrace();
			return null;
		}

	}

	public void writeFile() {
		try {
			ObjectOutputStream oStream = new ObjectOutputStream(new FileOutputStream(SAVE_PATH));
			oStream.writeObject(this);
			oStream.close();
		} catch (IOException e) {
			Logger.getGlobal().log(Level.SEVERE, "Failed to write token file!");
			e.printStackTrace();
		}

	}

	public void deleteFile() {
		new File(SAVE_PATH).delete();
	}

	public static boolean fileExists() { return new File(SAVE_PATH).exists(); }

	public String getAccessToken() {
		return _accessToken;
	}

	public void setAccessToken(String accessToken) {
		_accessToken = accessToken;
	}

	public long getGeneratedTime() {
		return _generatedTime;
	}

	public long getExpiryTime() {
		return _expiryTime;
	}

}
