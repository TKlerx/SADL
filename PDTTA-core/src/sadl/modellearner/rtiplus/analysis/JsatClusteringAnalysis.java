package sadl.modellearner.rtiplus.analysis;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.DataPoint;
import jsat.linear.distancemetrics.DistanceMetric;
import sadl.utils.DatasetTransformationUtils;

public abstract class JsatClusteringAnalysis extends ClusteringAnalysis {

	final DistanceMetric dm;

	public JsatClusteringAnalysis(DistanceMetric dstMetric, double clusterExpansionRate, DistributionAnalysis fewElementsAnalysis,
			int fewElementsLimit) {
		super(clusterExpansionRate, fewElementsAnalysis, fewElementsLimit);
		this.dm = dstMetric;
	}

	@Override
	List<TIntList> computeClusters(TIntList values, TIntList frequencies) {

		// To array
		final List<double[]> data = new ArrayList<>(values.size());
		for (int i = 0; i < values.size(); i++) {
			data.add(new double[] { values.get(i), frequencies.get(i) });
		}

		final DataSet<SimpleDataSet> ds = DatasetTransformationUtils.doublesToDataSet(data);

		final List<List<DataPoint>> c = computeJsatClusters(ds);

		final List<TIntList> clusters = new ArrayList<>(c.size());
		for (final List<DataPoint> clu : c) {
			if (clu.size() > 0) {
				final TIntList l = new TIntArrayList(clu.size());
				for (final DataPoint d : clu) {
					l.add((int) d.getNumericalValues().get(0));
				}
				clusters.add(l);
			}
		}

		return clusters;
	}

	abstract List<List<DataPoint>> computeJsatClusters(final DataSet<SimpleDataSet> ds);

}
