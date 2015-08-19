/**
 * This file is part of SADL, a library for learning all sorts of (timed) automata and performing sequence-based anomaly detection.
 * Copyright (C) 2013-2015  the original author or authors.
 *
 * SADL is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SADL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SADL.  If not, see <http://www.gnu.org/licenses/>.
 */

package jsat.distributions;



import java.util.logging.Level;
import java.util.logging.Logger;

import jsat.linear.Vec;
import jsat.utils.Pair;

import org.apache.commons.math3.util.Precision;

/**
 * 
 * @author Timo Klerx
 *
 */
public class SingleValueDistribution extends ContinuousDistribution {

	/**
	 * 
	 */
	private static final long serialVersionUID = 557528557730663203L;
	private double value;

	public SingleValueDistribution(double value) {
		this.value = value;
	}

	@Override
	public double pdf(double x) {
		if (Precision.equals(x, value)) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public double cdf(double x) {
		if(x >= value){
			return 1;
		}else{
			return 0;
		}
	}

	@Override
	public double invCdf(double p) {
		return value;
	}
	@Override
	public double min() {
		return value;
	}

	@Override
	public double max() {
		return value;
	}



	@Override
	public String getDescriptiveName() {
		return getDistributionName() + "(value=" + value+")";
	}

	@Override
	public String getDistributionName() {
		return "SingleValueDistribution";
	}

	@Override
	public String[] getVariables() {
		return new String[] { "value" };

	}

	@Override
	public double[] getCurrentVariableValues() {
		return new double[] { value };

	}

	@Override
	public void setVariable(String var, double value) {
		if(var.equals("value")){
			this.value = value;
		}
	}

	@Override
	public ContinuousDistribution clone() {
		return new SingleValueDistribution(this.value);
	}

	@Override
	public void setUsingData(Vec data) {
		final Pair<Boolean, Double> sameValues = MyDistributionSearch.checkForDifferentValues(data);
		if(sameValues.getFirstItem()){
			value = sameValues.getSecondItem();
		}else{
			Logger.getLogger(SingleValueDistribution.class.getName()).log(Level.WARNING,"Trying to use a SingleValueDistribution with data that contains more than one value.");
		}
	}

	@Override
	public double mean() {
		return value;
	}

	@Override
	public double mode() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(value);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SingleValueDistribution)) {
			return false;
		}
		final SingleValueDistribution other = (SingleValueDistribution) obj;
		if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value)) {
			return false;
		}
		return true;
	}

	@Override
	public double variance() {
		return 0;
	}

	@Override
	public double skewness() {
		return Double.NaN;
	}

}
