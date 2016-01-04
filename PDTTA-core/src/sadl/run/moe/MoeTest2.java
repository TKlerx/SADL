/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2016  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package sadl.run.moe;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class MoeTest2 {

	public static void main(String[] args) throws ClientProtocolException, IOException {
		final String postUrl = "http://pc-kbpool-8.cs.upb.de:6543/gp/next_points/epi";// put in your url
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) { // Use this instead
			final HistoryData h = new HistoryData();
			final Configuration c = new Configuration();
			final PdttaParameters parameters = new PdttaParameters();
			for (final Parameter p : parameters.parameters) {
				c.config.put(p, p.getDefault());
			}
			h.history.put(c, 0.5);
			final HttpPost post = new HttpPost(postUrl);
			final String s = parameters.toJsonString(20, h);
			// final String s = Files.readAllLines(Paths.get("testfile")).get(0);
			// final StringEntity postingString = new StringEntity(gson.toJson(p));// convert your pojo to json
			final StringEntity postingString = new StringEntity(s);// convert your pojo to json
			post.setEntity(postingString);
			System.out.println(EntityUtils.toString(post.getEntity(), "UTF-8"));
			post.setHeader("Content-type", "application/json");
			try (CloseableHttpResponse response = httpClient.execute(post)) {
				final String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
				System.out.println(responseString);
			}
		}
		// This is the right query
		// {"domain_info": {"dim": 2, "domain_bounds": [{"max": 1.0, "min": 0.0},{"max": 0.0, "min": -1.0}]}, "gp_historical_info": {"points_sampled":
		// [{"value_var": 0.01, "value": 0.1, "point": [0.0,0.0]}, {"value_var": 0.01, "value": 0.2, "point": [1.0,-1.0]}]}, "num_to_sample": 1}
	}

}
