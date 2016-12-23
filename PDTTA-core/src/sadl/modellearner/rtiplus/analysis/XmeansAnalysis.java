package sadl.modellearner.rtiplus.analysis;

import java.util.List;

import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.DataPoint;
import jsat.clustering.SeedSelectionMethods.SeedSelection;
import jsat.clustering.kmeans.HamerlyKMeans;
import jsat.clustering.kmeans.XMeans;
import jsat.linear.distancemetrics.DistanceMetric;
import sadl.utils.MasterSeed;

public class XmeansAnalysis extends JsatClusteringAnalysis {

	private final SeedSelection seedSel;

	public XmeansAnalysis(SeedSelection seedSelection, DistanceMetric dstMetric, double clusterExpansionRate) {
		this(seedSelection, dstMetric, clusterExpansionRate, null, -1);
	}

	public XmeansAnalysis(SeedSelection seedSelection, DistanceMetric dstMetric, double clusterExpansionRate, DistributionAnalysis fewElementsAnalysis,
			int fewElementsLimit) {
		super(dstMetric, clusterExpansionRate, fewElementsAnalysis, fewElementsLimit);
		this.seedSel = seedSelection;
	}

	@Override
	List<List<DataPoint>> computeJsatClusters(DataSet<SimpleDataSet> ds) {

		final XMeans xmeans = new XMeans(new HamerlyKMeans(dm, seedSel, MasterSeed.nextRandom()));
		return xmeans.cluster(ds);
	}

}
