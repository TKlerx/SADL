package sadl.modellearner.rtiplus.analysis;

import java.util.List;

import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.DataPoint;
import jsat.clustering.OPTICS;
import jsat.linear.distancemetrics.DistanceMetric;

public class OPTICSAnalysis extends JsatClusteringAnalysis {

	public OPTICSAnalysis(DistanceMetric dstMetric, double clusterExpansionRate) {
		this(dstMetric, clusterExpansionRate, null, -1);
	}

	public OPTICSAnalysis(DistanceMetric dstMetric, double clusterExpansionRate, DistributionAnalysis fewElementsAnalysis, int fewElementsLimit) {
		super(dstMetric, clusterExpansionRate, fewElementsAnalysis, fewElementsLimit);
	}

	@Override
	List<List<DataPoint>> computeJsatClusters(DataSet<SimpleDataSet> ds) {

		// TODO OPTICS was not completely implemented yet in JSAT. Missing param xi
		final OPTICS x = new OPTICS(dm, 1);
		return x.cluster(ds);
	}

}
