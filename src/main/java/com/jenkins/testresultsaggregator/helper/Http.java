package com.jenkins.testresultsaggregator.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Http {
	
	public static final int HTTPTIMEOUT = 10000;
	
	public enum AuthenticationType {
		CREDENTIALS,
		TOKEN
	}
	
	public static String get(AuthenticationType authenticationType, String url, String authentication) throws IOException {
		return get(authenticationType, new URL(url), authentication);
	}
	
	public static String get(AuthenticationType authenticationType, URL url, String authentication) throws IOException {
		StringBuilder buf = new StringBuilder();
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setConnectTimeout(HTTPTIMEOUT);
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		con.setDoOutput(true);
		if (authenticationType.equals(AuthenticationType.CREDENTIALS) && authentication != null) {
			con.setRequestProperty("Authorization", "Basic " + authentication);
		} else {
			con.setRequestProperty("Authorization", "Bearer " + authentication);
		}
		// Get the response
		BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF8"));
		String line;
		while ((line = rd.readLine()) != null) {
			buf.append(line);
		}
		rd.close();
		return buf.toString();
	}
	
	public static int getResponseCode(AuthenticationType authenticationType, String url, String authentication) {
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setReadTimeout(2000);
			con.setConnectTimeout(2000);
			if (authenticationType.equals(AuthenticationType.CREDENTIALS) && authentication != null) {
				con.setRequestProperty("Authorization", "Basic " + authentication);
			} else {
				con.setRequestProperty("Authorization", "Bearer " + authentication);
			}
			int responseCode = con.getResponseCode();
			con.disconnect();
			return responseCode;
		} catch (IOException ex) {
		}
		return 0;
	}
}
