package sadl.run.moe;

import java.util.ArrayList;
import java.util.List;

public class PdttaParameters {

	NumericDoubleParameter mergeAlpha = new NumericDoubleParameter("mergeAlpha", 0, 1, 0.05);
	NumericDoubleParameter kdeBandwidth = new NumericDoubleParameter("kdeBandwidth", 0, 1000, 0);
	NumericIntParameter recursiveMergeTest = new NumericIntParameter("recMergeTest", 0, 1, 0);
	NumericDoubleParameter aggregatedTimeThreshold = new NumericDoubleParameter("aggTimeThreshold", 0, 1, 0.00001);
	NumericDoubleParameter aggregatedEventThreshold = new NumericDoubleParameter("aggEventThreshold", 0, 1, 0.00001);
	List<Parameter> parameters = new ArrayList<>();
	public PdttaParameters() {
		parameters.add(mergeAlpha);
		parameters.add(kdeBandwidth);
		parameters.add(recursiveMergeTest);
		parameters.add(aggregatedEventThreshold);
		parameters.add(aggregatedTimeThreshold);
	}

	public int getDim() {
		return parameters.size();
	}
	// {"domain_info": {"dim": 2, "domain_bounds": [{"max": 1.0, "min": 0.0},{"max": 0.0, "min": -1.0}]}, "gp_historical_info": {"points_sampled":
	// [{"value_var": 0.01, "value": 0.1, "point": [0.0,0.0]}, {"value_var": 0.01, "value": 0.2, "point": [1.0,-1.0]}]}, "num_to_sample": 1}

	public String toJsonString(int numToSample, HistoryData history) {
		final StringBuilder sb = new StringBuilder();
		sb.append("{\"domain_info\": {\"dim\": ");
		sb.append(getDim());
		sb.append(", \"domain_bounds\": [");
		for (final Parameter p : parameters) {
			sb.append("{\"max\": ");
			sb.append(p.getMax());
			sb.append(", \"min\": ");
			sb.append(p.getMin());
			sb.append("}");
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append("]}, \"gp_historical_info\": {\"points_sampled\":[");
		for (final Configuration c : history) {
			sb.append("{\"value_var\": 0.000001, \"value\": ");
			sb.append(history.history.get(c));
			sb.append(", \"point\": [");
			for (final Parameter p : parameters) {
				sb.append(c.config.get(p));
				sb.append(",");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("]},");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append("]}, \"num_to_sample\": ");
		sb.append(numToSample);
		sb.append("}");
		return sb.toString();
	}

}
